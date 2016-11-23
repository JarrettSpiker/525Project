package com.jspiker.accesscontrolsystem;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;

import com.google.common.base.Function;
import com.google.common.primitives.Booleans;
import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.IOException;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;

import static com.google.common.util.concurrent.Futures.allAsList;

public class AccessControlInitializeActivity extends AppCompatActivity {


    private static final String UUID_STRING = "fb6c2ead-8d7b-47a4-bc5e-c8df7534ef4f"; //This must be the same in both the client and the server
    private static final int REQUEST_ENABLE_BT_CODE = 7;
    private static final int REQUEST_ENABLE_DISC = 8;

    private static final String numberOfDevicesFound = "Number of devices found: ";

    private Spinner numDevicesSpinner;
    private Switch requirePasscodeSwitch;

    private TextView pleaseConfirmText;
    private TextView numDevicesText;

    private TextView thisDeviceIDText;

    private Button cancelButton;

    public int numDevices = 0;

    private boolean requirePasscode = false;

    private final Object foundSoFarLock = new Object();
    private int foundSoFar = 0; //Dont access this unless synchronized on foundSoFarLock

    public CopyOnWriteArrayList<DeviceInfo> foundDevices;

    private ListenableFuture<Void> findDevicesThread;

    private Function<Void,Void> findDevicesFunction =  new Function<Void, Void>() {
        @Override
        public Void apply(Void input) {
            BluetoothServerSocket serverSocket = null;
            if (ContextCompat.checkSelfPermission(AccessControlInitializeActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {

                System.out.print("Here");
            }
            try {
                try {
                    serverSocket = BluetoothAdapter.getDefaultAdapter().listenUsingRfcommWithServiceRecord("Initialize Access Control System",UUID.fromString(UUID_STRING));
                } catch (IOException e) {
                    handleBluetoothFailed("Bluetooth socket creation failed: " + e.getMessage());
                    return  null;
                }

                BluetoothSocket socket = null;
                int connectedSoFar = 0;
                ArrayList<ListenableFuture<Void>> connections = new ArrayList<>();
                //keep looking until we find enough devices
                while (connectedSoFar < numDevices) {
                    try {
                        socket = serverSocket.accept();
                    } catch (IOException e) {
                        continue;
                    }

                    if (socket != null) {
                        // Do work to manage the connection (in a separate thread)
                        connections.add(handlePhoneConnected(socket));
                    }
                }

                Futures.whenAllSucceed(connections).call(new Callable<Void>() {
                    @Override
                    public Void call() throws Exception {
                        completeRegistration();
                        return null;
                    }
                });
                completeRegistration();
            } finally {
                if(serverSocket != null){
                    try {
                        serverSocket.close();
                    } catch (IOException e) {
                    }
                }
            }
            return null;
        }
    };


    private ListenableFuture<Void> completeRegistration(){

        //start listenting for responses on all phones
        AsyncFunction<DeviceInfo, Boolean> waitForConfirmationResponse = new AsyncFunction<DeviceInfo, Boolean>() {
            @Override
            public ListenableFuture<Boolean> apply(DeviceInfo deviceInfo) throws Exception {
                return AccessControlCommunicationApi.receiveConfirmationResponse(deviceInfo.socket);
            }
        };

        ArrayList<ListenableFuture<Boolean>> waitForConfirmationList = new ArrayList<>();
        for(final DeviceInfo device : foundDevices){
            ListenableFuture<Boolean> waitForDevice =
                    Futures.transformAsync(Threading.runOnBackgroundThread(new Function<Void, DeviceInfo>() {
                        @Override
                        public DeviceInfo apply(Void input) {
                            return device;//This just switches to the background thread
                        }
                    }),
                    waitForConfirmationResponse);

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

    private ListenableFuture<Void> allDevicesConfirmed(){

        for(DeviceInfo deviceInfo : foundDevices){
            AccessControlStorage.setPasscodeForDevice(this, deviceInfo.macAddress, deviceInfo.passcode);
            AccessControlStorage.setTokenForDevice(this, deviceInfo.macAddress, deviceInfo.passcode);

        }

        sendFinalAcks(true);

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                numDevicesText.setText("Setup complete!");
                numDevicesText.setTextColor(Color.GREEN);
            }
        });

        // Save the number of devices to storage
        AccessControlStorage.setNumDevices(this, numDevices);

        return Futures.immediateFuture(null);
    }

    private  ListenableFuture<Void> deviceRejectedConnection(){
        sendFinalAcks(false);

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                switchToFindingDevicesMode(false);
                numDevicesText.setText("Devices rejected final confirmation");
                numDevicesText.setTextColor(Color.RED);
            }
        });
        return Futures.immediateFuture(null);
    }

    private void sendFinalAcks(boolean positive){
        for(DeviceInfo deviceInfo : foundDevices){
            AccessControlCommunicationApi.sendFinalAck(deviceInfo.socket, positive);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reinitialize);

        requirePasscodeSwitch = (Switch) findViewById(R.id.requirePasscodeSwitch);
        numDevicesSpinner = (Spinner) findViewById(R.id.numItemsSpinner);
        Button confirmButton = (Button) findViewById(R.id.confirmNumDevicesButton);
        confirmButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                numDevices = numDevicesSpinner.getSelectedItemPosition()+1;
                requirePasscode = requirePasscodeSwitch.isChecked();
                requirePasscodeSwitch.setEnabled(false);
                numDevicesSpinner.setEnabled(false);
                startBluetooth();
            }
        });

        cancelButton = (Button) findViewById(R.id.cancelFindingDevicesButton);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(findDevicesThread != null){
                    findDevicesThread.cancel(true);
                }
            }
        });

        numDevicesText = (TextView) findViewById(R.id.numDevicesText);
        pleaseConfirmText = (TextView) findViewById(R.id.pleaseConfirmText);

        // Will display the address of this phone to the user
        thisDeviceIDText = (TextView) findViewById(R.id.IDText);
        // Need to figure out how to access the device ID in order to display it - similar to client side only check yourself instead of other devices?
        //thisDevice =
        //thisDeviceIDText.setText("Access Control System Device ID:\n" + thisDevice.getAddress());

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

    private void makeDiscoverable(){
        Intent discoverableIntent = new
        Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 3600);
        startActivityForResult(discoverableIntent, REQUEST_ENABLE_DISC);
    }

    private void tryToFindDevices() {
        setNumberOfDevicesFound(0);

        switchToFindingDevicesMode(true);
        foundDevices = new CopyOnWriteArrayList<>();
        findDevicesThread = Threading.runOnBackgroundThread(findDevicesFunction);
    }

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

    private  ListenableFuture<Void> handlePhoneConnected(final BluetoothSocket socket){

        ListenableFuture<String> generateToken =Futures.transform(Threading.switchToBackground(), new Function<Void, String>() {
                @Override
                public String apply(Void input) {
                    SecureRandom random = new SecureRandom();
                    return new BigInteger(130, random).toString(32);

                }
            });

        final AtomicReference<String> token = new AtomicReference<>();
        final AtomicReference<String> passcode = new AtomicReference<>();


        ListenableFuture<Void> sendToken = Futures.transformAsync(generateToken, new AsyncFunction<String, Void>() {
            @Override
            public ListenableFuture<Void> apply(String token) {
                return AccessControlCommunicationApi.sendTokenAndPasscode(socket, token, requirePasscode);
            }
        });

        ListenableFuture<String> getPasscode = Futures.transformAsync(sendToken, new AsyncFunction<Void, String>() {
            @Override
            public ListenableFuture<String> apply(Void input) throws Exception {
                return AccessControlCommunicationApi.receivePasscode(socket);
            }
        });

        ListenableFuture<Void> updateDevicesFound = Futures.transform(getPasscode, new Function<String, Void>() {
            @Override
            public Void apply(String input) {
                DeviceInfo info = new DeviceInfo(socket, socket.getRemoteDevice().getAddress(), token.get(), passcode.get());
                foundDevices.add(info);
                int found = 0;
                synchronized (foundSoFarLock){
                    foundSoFar++;
                    found = foundSoFar;
                }
                setNumberOfDevicesFound(found);
                return null;
            }
        });

        return Futures.catching(updateDevicesFound, Throwable.class, new Function<Throwable, Void>() {
            @Override
            public Void apply(Throwable input) {
                Log.w("Reinit error", input.getMessage());
                return null;
            }
        });
    }

    private void setNumberOfDevicesFound(final int number){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                numDevicesText.setVisibility(View.VISIBLE);
                numDevicesText.setText(numberOfDevicesFound + number);
            }
        });

    }

    private void switchToFindingDevicesMode(boolean findingDevices){
        cancelButton.setVisibility(findingDevices ? View.VISIBLE : View.INVISIBLE);
        cancelButton.setEnabled(findingDevices);
        numDevicesText.setEnabled(findingDevices);

        requirePasscodeSwitch.setEnabled(!findingDevices);
        numDevicesSpinner.setEnabled(!findingDevices);
        pleaseConfirmText.setEnabled(!findingDevices);
        pleaseConfirmText.setVisibility( findingDevices ? View.INVISIBLE : View.VISIBLE);
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
