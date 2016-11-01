package com.jspiker.accesscontrolsystem;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.graphics.Color;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;

import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.util.concurrent.ListenableFuture;

import org.w3c.dom.Text;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.UUID;

public class ReinitializeActivity extends AppCompatActivity {

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

    private ListenableFuture<Void> findDevicesThread;

    private Function<Void,Void> findDevicesFunction =  new Function<Void, Void>() {
        @Override
        public Void apply(Void input) {
            BluetoothServerSocket serverSocket = null;
            try {
                try {
                    serverSocket = BluetoothAdapter.getDefaultAdapter().listenUsingRfcommWithServiceRecord("Initialize Access Control System", UUID.randomUUID());
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
                        continue;
                    }
                }
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
        }
    }

    private void tryToFindDevices() {
        numDevicesText.setVisibility(View.VISIBLE);
        numDevicesText.setText(numberOfDevicesFound + getFoundSoFar());

        switchToFindingDevicesMode(true);

        findDevicesThread = Threading.runOnBackgroundThread(findDevicesFunction);
    }

    private void handleBluetoothFailed(final String reason){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                numDevicesText.setText("Bluetooth connection failed\n" + reason);
                numDevicesText.setTextColor(Color.RED);

                switchToFindingDevicesMode(false);
            }
        });
    }

    private synchronized void handlePhoneConnected(BluetoothSocket socket){
        synchronized (foundSoFarLock) {
            //TODO handle the connection of the new phone
            /*
            This will somehow have to involve getting the passcode from the phone, giving the phone a token, and then saving the phone to some list so that we can send a confirmation message
             */
            foundSoFar++;

        }

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
}
