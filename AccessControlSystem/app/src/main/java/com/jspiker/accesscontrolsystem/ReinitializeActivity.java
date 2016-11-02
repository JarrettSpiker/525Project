package com.jspiker.accesscontrolsystem;

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
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;

public class ReinitializeActivity extends AppCompatActivity {


    private static final String UUID_STRING = "525ProjectUUID"; //This must be the same in both the client and the server
    private static final int REQUEST_ENABLE_BT_CODE = 7;

    private static final String numberOfDevicesFound = "Number of devices found: ";

    private Spinner numDevicesSpinner;
    private Switch requirePasscodeSwitch;

    private TextView pleaseConfirmText;
    private TextView numDevicesText;

    private Button cancelButton;

    private int numDevices = 0;

    private boolean requirePasscode = false;

    private final Object foundSoFarLock = new Object();
    private int foundSoFar = 0; //Dont access this unless synchronized on foundSoFarLock

    private CopyOnWriteArrayList<DeviceInfo> foundDevices;

    private ListenableFuture<Void> findDevicesThread;

    private Function<Void,Void> findDevicesFunction =  new Function<Void, Void>() {
        @Override
        public Void apply(Void input) {
            BluetoothServerSocket serverSocket = null;
            try {
                try {
                    serverSocket = BluetoothAdapter.getDefaultAdapter().listenUsingRfcommWithServiceRecord("Initialize Access Control System",UUID.fromString(UUID_STRING));
                } catch (IOException e) {
                    handleBluetoothFailed("Bluetooth socket creation failed: " + e.getMessage());
                    return  null;
                }

                BluetoothSocket socket = null;
                int connectedSoFar = 0;

                //keep looking until we find enough devices
                while (connectedSoFar < numDevices) {
                    try {
                        socket = serverSocket.accept();
                    } catch (IOException e) {
                        continue;
                    }

                    if (socket != null) {
                        // Do work to manage the connection (in a separate thread)
                        handlePhoneConnected(socket);
                    }
                }

                //TODO we now have a list of deviced in foundDevices; finish the connection and save the info for each
            }finally {
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
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Check which request we're responding to
        if (requestCode == REQUEST_ENABLE_BT_CODE) {
            //returning from the "enable bluetooth" activity
            if(resultCode ==  RESULT_OK){
                tryToFindDevices();
            } else{
                handleBluetoothFailed("Could not enable bluetooth");
            }
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
            tryToFindDevices();
        }
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
        ListenableFuture<String> generateToken = Threading.runOnBackgroundThread(new Function<Void, String>() {
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
                return CommunicationApi.sendTokenAndPasscode(socket, token, requirePasscode);
            }
        });

        ListenableFuture<String> getPasscode = Futures.transformAsync(sendToken, new AsyncFunction<Void, String>() {
            @Override
            public ListenableFuture<String> apply(Void input) throws Exception {
                return CommunicationApi.receivePasscode();
            }
        });

        ListenableFuture<Void> updateDevicesFound = Futures.transform(getPasscode, new Function<String, Void>() {
            @Override
            public Void apply(String input) {
                DeviceInfo info = new DeviceInfo(socket, token.get(), passcode.get());
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
                numDevicesText.setText( numberOfDevicesFound + number);
            }
        });

    }

    private int getFoundSoFar(){
        synchronized (foundSoFarLock){
            return foundSoFar;
        }
    }

    private void setFoundSoFar(int newValue){
        synchronized (foundSoFarLock){
            foundSoFar = newValue;
        }
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

    private static class DeviceInfo{
        final BluetoothSocket socket;
        final String token;
        final String passcode;
        DeviceInfo(BluetoothSocket socket, String token, String passcode){
            this.socket = socket;
            this.token = token;
            this.passcode = passcode;
        }
    }
}
