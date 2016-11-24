package com.jspiker.accesscontrolsystem;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;

import com.google.common.base.Function;
import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.IOException;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class AccessControlInitializeActivity extends AppCompatActivity {


    private static final String UUID_STRING = "fb6c2ead-8d7b-47a4-bc5e-c8df7534ef4f"; //This must be the same in both the client and the server
    private static final int REQUEST_ENABLE_BT_CODE = 7;
    private static final int REQUEST_ENABLE_DISC = 8;

    private static final String numberOfDevicesFound = "Number of devices found: ";

    private Spinner numDevicesSpinner;
    private Switch requirePasscodeSwitch;

    private TextView pleaseConfirmText;
    private TextView numDevicesText;

    private Button completeButton;

    public int numDevices = 0;
    private boolean success = false;
    private boolean requirePasscode = false;

    private int foundSoFar = 0; //Dont access this unless synchronized on foundSoFarLock

    public CopyOnWriteArrayList<DeviceInfo> foundDevices;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reinitialize);

        //get references to all of the UI elements which we will need to modify
        requirePasscodeSwitch = (Switch) findViewById(R.id.requirePasscodeSwitch);
        numDevicesSpinner = (Spinner) findViewById(R.id.numItemsSpinner);


        Button confirmButton = (Button) findViewById(R.id.confirmNumDevicesButton);
        confirmButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Start the reinitialization
                numDevices = numDevicesSpinner.getSelectedItemPosition()+1;
                requirePasscode = requirePasscodeSwitch.isChecked();
                requirePasscodeSwitch.setEnabled(false);
                numDevicesSpinner.setEnabled(false);

                //reinitialization begins with starting bluetooth
                startBluetooth();
            }
        });


        numDevicesText = (TextView) findViewById(R.id.numDevicesText);
        pleaseConfirmText = (TextView) findViewById(R.id.pleaseConfirmText);

        // Will display the address of this phone to the user
        TextView thisDeviceIDText = (TextView) findViewById(R.id.IDText);
        thisDeviceIDText.setText("Device Address: " + android.provider.Settings.Secure.getString(this.getContentResolver(), "bluetooth_address"));

        completeButton = (Button) findViewById(R.id.completeButton);
        completeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent resultIntent = new Intent();
                resultIntent.putExtra(AccessControlActivity.INIT_RESULT_KEY, success);
                setResult(Activity.RESULT_OK, resultIntent);
                finish();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Check which request we're responding to
        if (requestCode == REQUEST_ENABLE_BT_CODE) {
            //returning from the "enable bluetooth" activity
            if(resultCode ==  RESULT_OK){
                makeDiscoverable();
            } else{
                handleBluetoothFailed("Could not enable bluetooth");
            }
        }
        if(requestCode == REQUEST_ENABLE_DISC){
            tryToFindDevices();
        }
    }

    /**
     * Let the user know that their device's bluetooth connection failed somehow
     */
    private void handleBluetoothFailed(final String reason){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                numDevicesText.setVisibility(View.VISIBLE);
                numDevicesText.setText("Bluetooth connection failed\n" + reason);
                numDevicesText.setTextColor(Color.RED);

                switchToFindingDevicesMode(false);
            }
        });
    }


    /**
     * Ensure that bluetooth is on, and make the device discoverable
     */
    private void startBluetooth(){

        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if(bluetoothAdapter == null){
            //bluetooth is not supported on the device
            handleBluetoothFailed("This device does not support bluetooth");
            return;
        }
        if(!bluetoothAdapter.isEnabled()){
            Intent startBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(startBluetooth, REQUEST_ENABLE_BT_CODE);
        } else{
            makeDiscoverable();
        }
    }

    /**
     * Fire the intent which will make the the server discoverable
     * If this is successful, control will be transfered to the onActivityResult method
     */
    private void makeDiscoverable(){
        Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 3600);
        startActivityForResult(discoverableIntent, REQUEST_ENABLE_DISC);
    }

    /**
     * Alter the UI for whether or not we are actively looking for devices
     * @param findingDevices whether we are now looking for devices or not
     */
    private void switchToFindingDevicesMode(boolean findingDevices){
        numDevicesText.setEnabled(findingDevices);

        requirePasscodeSwitch.setEnabled(!findingDevices);
        numDevicesSpinner.setEnabled(!findingDevices);
        pleaseConfirmText.setEnabled(!findingDevices);
        pleaseConfirmText.setVisibility( findingDevices ? View.INVISIBLE : View.VISIBLE);
    }

    /**
     * Start looking for devices. Run the function which starts a bluetooth server on a background thread
     */
    private void tryToFindDevices() {
        setNumberOfDevicesFound(0);

        switchToFindingDevicesMode(true);
        foundDevices = new CopyOnWriteArrayList<>();
        Threading.runOnBackgroundThread(findDevicesFunction);
    }

    /**
     * Open a server socket which waits for some number of devices to connect.
     * For each new device found, handle that device on a new thread
     * continue until we have found enough devices
     */
    private Function<Void,Void> findDevicesFunction =  new Function<Void, Void>() {
        @Override
        public Void apply(Void input) {

            final  AtomicReference<BluetoothServerSocket> serverSocket = new AtomicReference<>();
            try {
                try {
                    //open the server socket and start listening for connection requests
                    serverSocket.set(BluetoothAdapter.getDefaultAdapter().listenUsingRfcommWithServiceRecord("Initialize Access Control System",UUID.fromString(UUID_STRING)));
                } catch (IOException e) {
                    handleBluetoothFailed("Bluetooth socket creation failed: " + e.getMessage());
                    return  null;
                }

                BluetoothSocket socket = null;
                final AtomicInteger connectedSoFar = new AtomicInteger(0); //number of successful connections

                //keep looking until we find the user specified number of devices
                while (connectedSoFar.get() < numDevices) {
                    try {
                        socket = serverSocket.get().accept();
                    } catch (IOException e) {
                        continue;
                    }

                    if (socket != null) {
                        // Do work to manage the connection (in a separate thread)
                        ListenableFuture<Void> connect = handlePhoneConnected(socket);

                        //when "handling the connection" is done, increment the number of devices we've found
                        Futures.transform(connect, new Function<Void, Void>() {
                            @Override
                            public Void apply(Void input) {
                                int connected = connectedSoFar.incrementAndGet();

                                //if we have found enough devices, close the server socket to avoid a deadlock
                                if(connected >= numDevices){
                                    try {
                                        serverSocket.get().close();
                                    }catch (IOException e){
                                        //dodge
                                    }
                                }
                                return  null;
                            }
                        });

                    }
                }


                //we have now found enough devices. Send confirmations to each of them
                completeRegistration();
            } finally {

                //ensure that the server socket eventually gets closed
                if(serverSocket != null){
                    try {
                        serverSocket.get().close();
                    } catch (IOException e) {
                    }
                }
            }
            return null;
        }
    };

    /**
     * Take a socket (connection to a new phone) and establish the token and passcode
     */
    private  ListenableFuture<Void> handlePhoneConnected(final BluetoothSocket socket){

        //generate a random token
        ListenableFuture<String> generateToken =Futures.transform(Threading.switchToBackground(), new Function<Void, String>() {
                @Override
                public String apply(Void input) {
                    SecureRandom random = new SecureRandom();
                    return new BigInteger(130, random).toString(32);

                }
            });

        final AtomicReference<String> token = new AtomicReference<>();
        final AtomicReference<String> passcode = new AtomicReference<>();

        //send the token to the client
        ListenableFuture<Void> sendToken = Futures.transformAsync(generateToken, new AsyncFunction<String, Void>() {
            @Override
            public ListenableFuture<Void> apply(String token) {
                return AccessControlCommunicationApi.sendTokenAndPasscode(socket, token, requirePasscode);
            }
        });

        //wait for the client to reply with its passcode
        ListenableFuture<String> getPasscode = Futures.transformAsync(sendToken, new AsyncFunction<Void, String>() {
            @Override
            public ListenableFuture<String> apply(Void input) throws Exception {
                return AccessControlCommunicationApi.receivePasscode(socket);
            }
        });

        //Update the ui with the number of devices we have found, and save a refrence to the device, so that we can send a confirmation later
        ListenableFuture<Void> updateDevicesFound = Futures.transform(getPasscode, new Function<String, Void>() {
            @Override
            public Void apply(String input) {
                DeviceInfo info = new DeviceInfo(socket, socket.getRemoteDevice().getAddress(), token.get(), passcode.get());
                foundDevices.add(info);
                int found = ++foundSoFar;
                setNumberOfDevicesFound(found);
                return null;
            }
        });

        //If any of the above processes fail, fail silently
        return Futures.catching(updateDevicesFound, Throwable.class, new Function<Throwable, Void>() {
            @Override
            public Void apply(Throwable input) {
                Log.w("Reinit error", input.getMessage());
                return null;
            }
        });
    }


    /**
     * Send a confirmation to each waiting device, wait for them to all acknowledge ,
     * and then notify all devices that registration was sucessful
     * @return
     */
    private ListenableFuture<Void> completeRegistration(){

        //start listenting for responses on all phones
        ArrayList<ListenableFuture<Boolean>> waitForConfirmationList = new ArrayList<>();
        for(final DeviceInfo device : foundDevices){
            ListenableFuture<Boolean> waitForDevice = Futures.transformAsync(
                    Threading.runOnBackgroundThread(new Function<Void, DeviceInfo>() {
                                @Override
                                public DeviceInfo apply(Void input) {
                                    return device;//This just switches to the background thread
                                }
                            }),
                            new AsyncFunction<DeviceInfo, Boolean>() {
                                @Override
                                public ListenableFuture<Boolean> apply(DeviceInfo deviceInfo) throws Exception {
                                    return AccessControlCommunicationApi.receiveConfirmationResponse(deviceInfo.socket);
                                }
                            });

            waitForConfirmationList.add(waitForDevice);
        }

        //send confirmation to every device
        for(final DeviceInfo device : foundDevices){
            Threading.runOnBackgroundThread(new Function<Void, Void>() {
                @Override
                public Void apply(Void input) {
                    AccessControlCommunicationApi.sendConfirmation(device.socket);
                    return null;
                }
            });
        }


        //wait until a response is received from all devices
        ListenableFuture<List<Boolean>> responsesReceived = Futures.allAsList(waitForConfirmationList);

        ListenableFuture<Boolean> combineResponses = Futures.transform(responsesReceived, new Function<List<Boolean>, Boolean>() {
            @Override
            public Boolean apply(List<Boolean> input) {
                for(Boolean b : input){
                    if(!b){
                        return false;
                    }
                }
                return true;
            }
        });

        ListenableFuture<Boolean> withFallback = Futures.catching(combineResponses, Throwable.class, new Function<Throwable, Boolean>() {
            @Override
            public Boolean apply(Throwable input) {
                return false;
            }
        });

        //if all devices responded true, then registration was successful. Otherwise it failed
        return Futures.transformAsync(withFallback, new AsyncFunction<Boolean, Void>() {
            @Override
            public ListenableFuture<Void> apply(Boolean success) throws Exception {
                if(success){
                    return allDevicesConfirmed();
                } else{
                    return deviceRejectedConnection();
                }
            }
        });
    }

    /**
     * Update UI to show that registration was successful
     * send a final ack to all devices letting them know that registration was successful
     * Save the token and passcode for each device
     * @return
     */
    private ListenableFuture<Void> allDevicesConfirmed(){

        //Save the token and passcode for each device
        for(DeviceInfo deviceInfo : foundDevices){
            AccessControlStorage.setPasscodeForDevice(this, deviceInfo.macAddress, deviceInfo.passcode);
            AccessControlStorage.setTokenForDevice(this, deviceInfo.macAddress, deviceInfo.token);

        }

        //send a final ack to all devices letting them know that registration was successful
        sendFinalAcks(true);

        //Update UI to show that registration was successful
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                numDevicesText.setText("Setup complete!");
                numDevicesText.setTextColor(Color.GREEN);
                completeButton.setVisibility(View.VISIBLE);
                completeButton.setEnabled(true);

            }
        });

        // Save the number of devices to storage
        AccessControlStorage.setNumDevices(this, numDevices);

        success = true;

        return Futures.immediateFuture(null);
    }


    /**
     * Update UI to show that registration failed
     * send a final ack to all devices letting them know that registration failed
     * @return
     */
    private  ListenableFuture<Void> deviceRejectedConnection(){
        //send a final ack to all devices letting them know that registration failed
        sendFinalAcks(false);


        //Update UI to show that registration failed
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                switchToFindingDevicesMode(false);
                numDevicesText.setText("Devices rejected final confirmation");
                numDevicesText.setTextColor(Color.RED);
                completeButton.setVisibility(View.VISIBLE);
                completeButton.setEnabled(true);
            }
        });
        return Futures.immediateFuture(null);
    }

    /**
     * Send an ack to each device letting them know if registration was successful or not
     * @param positive
     */
    private void sendFinalAcks(boolean positive){
        for(DeviceInfo deviceInfo : foundDevices){
            AccessControlCommunicationApi.sendFinalAck(deviceInfo.socket, positive);
        }
    }

    /**
     * Update the UI to show how many devices have been found
     * @param number
     */
    private void setNumberOfDevicesFound(final int number){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                numDevicesText.setVisibility(View.VISIBLE);
                numDevicesText.setText(numberOfDevicesFound + number);
            }
        });

    }

    public String getUuidString(){
        return UUID_STRING;
    }

    public static class DeviceInfo{
        final BluetoothSocket socket;
        final String token;
        final String passcode;
        final String macAddress;
        DeviceInfo(BluetoothSocket socket, String macAddress, String token, String passcode){
            this.socket = socket;
            this.token = token;
            this.passcode = passcode;
            this.macAddress = macAddress;
        }
    }
}
