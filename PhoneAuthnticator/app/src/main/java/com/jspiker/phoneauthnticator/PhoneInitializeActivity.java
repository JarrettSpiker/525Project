package com.jspiker.phoneauthnticator;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.InputType;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.google.common.base.Function;
import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class PhoneInitializeActivity extends AppCompatActivity {

    private static final String UUID_STRING = "fb6c2ead-8d7b-47a4-bc5e-c8df7534ef4f"; //This must be the same in both the client and the server

    private static final int REQUEST_COARSE_LOCATION = 8;
    private static final int REQUEST_ENABLE_BT = 7;

    TextView statusText;
    ListView foundDevices;
    BluetoothAdapter mAdapter;
    ArrayAdapter<BluetoothDevice> deviceAdapter;
    // For debugging >
    //ArrayAdapter<String> deviceAdapter;


    boolean discoveryStarted = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_initialize);
        statusText = (TextView) findViewById(R.id.initialize_status);

        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(receiver, filter);

        mAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mAdapter == null) {
            setStatusText("This device does not support bluetooth", Color.RED);
            return;
        }

        deviceAdapter = new ArrayAdapter<BluetoothDevice>(this, android.R.layout.simple_list_item_1);
        // For debugging >
        //deviceAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);

        foundDevices = (ListView) findViewById(R.id.found_devices_list);
        foundDevices.setAdapter( deviceAdapter );
        foundDevices.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                bluetoothServerSelected(deviceAdapter.getItem(position));
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        setStatusText("Searching for devices...", Color.GRAY);

       /* BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            setStatusText("This device does not support bluetooth", Color.RED);
            return;
        } */

        if (!mAdapter.isEnabled()) {
            Intent startBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(startBluetooth, REQUEST_ENABLE_BT);
            // We need to go into the app permission settings if the list of devices is not populating
        } else {
            startDiscovery();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case REQUEST_COARSE_LOCATION: {
                startDiscovery();
            }
        }
    }

    @Override
    public void onResume(){
        super.onResume();
        IntentFilter intFilter = new IntentFilter();
        intFilter.addAction(BluetoothDevice.ACTION_FOUND);
        intFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        intFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(receiver, intFilter);
    }

    @Override
    public void onPause(){
        super.onPause();
        unregisterReceiver(receiver);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // Check which request we're responding to
        if (requestCode == REQUEST_ENABLE_BT) {
            //returning from the "enable bluetooth" activity
            if (resultCode == RESULT_OK) {
                startDiscovery();
            } else {
                setStatusText("Could not enable bluetooth", Color.RED);
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    private void startDiscovery() {

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                    REQUEST_COARSE_LOCATION);
            return;
        }


        foundDevices.setEnabled(true);

        discoveryStarted = true;
        deviceAdapter = new ArrayAdapter<BluetoothDevice>(this, android.R.layout.simple_list_item_1);
        foundDevices.setAdapter(deviceAdapter);
        mAdapter.startDiscovery();
    }


    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            // When discovery finds a device
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                //deviceFound(device);
                deviceAdapter.add(device);
                // For debugging >
                //deviceAdapter.add(device.getName() + "\n" + device.getAddress());
            }
            /*
            else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {

            }
            */
        }
    };

    private void setStatusText(final String text, final Integer color) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                statusText.setText(text);
                if (color != null) {
                    statusText.setTextColor(color);
                }
            }
        });

    }

    /*
    private void deviceFound(BluetoothDevice device) {
        if (!discoveryStarted) {
            return;
        }
        deviceAdapter.add(device);
    }
    */

    private void bluetoothServerSelected(final BluetoothDevice device) {
        discoveryStarted = false;
        mAdapter.cancelDiscovery();
        //BluetoothAdapter.getDefaultAdapter().cancelDiscovery();
        ListenableFuture<BluetoothSocket> getSocket =
                Threading.runOnBackgroundThread(new Function<Void, BluetoothSocket>() {
                    @Override
                    public BluetoothSocket apply(Void input) {
                        BluetoothSocket socket = null;
                        try {
                            socket = device.createRfcommSocketToServiceRecord(UUID.fromString(UUID_STRING));
                            socket.connect();
                            return socket;
                        } catch (IOException e) {
                            setStatusText("Failed to establish link: " + e.getMessage(), Color.RED);
                        }
                        return null;
                    }
                });

        Futures.transformAsync(getSocket, new AsyncFunction<BluetoothSocket, Void>() {
            @Override
            public ListenableFuture<Void> apply(BluetoothSocket socket) {
                if (socket == null) {
                    return Futures.immediateFuture(null);
                }
                return handleConnectedSocketAsync(socket);
            }
        });

    }

    private ListenableFuture<Void> handleConnectedSocketAsync(final BluetoothSocket socket) {
        final ListenableFuture<PhoneCommunicationApi.TokenAndPasscodeResponse> getTokenAndPasscode = PhoneCommunicationApi.getTokenAndPasscodeRequired(socket);

        final AtomicBoolean passcodeRequired = new AtomicBoolean(false);

        ListenableFuture<Void> saveToken = Futures.transform(getTokenAndPasscode, new Function<PhoneCommunicationApi.TokenAndPasscodeResponse, Void>() {
            @Override
            public Void apply(PhoneCommunicationApi.TokenAndPasscodeResponse response) {
                PhoneStorageAccess.setToken(PhoneInitializeActivity.this, response.token);
                passcodeRequired.set(response.isPasscodeRequired);
                return null;
            }
        });

        ListenableFuture<String> getPasscode = Futures.transform(saveToken, new Function<Void, String>() {
            @Override
            public String apply(Void input) {

                final AtomicReference<String> passcode = new AtomicReference<>();
                if (passcodeRequired.get()) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            AlertDialog.Builder builder = new AlertDialog.Builder(PhoneInitializeActivity.this);
                            builder.setTitle("Enter passcode");

                            // Set up the input
                            final EditText passcodeBox = new EditText(PhoneInitializeActivity.this);
                            // Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
                            passcodeBox.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                            builder.setView(passcodeBox);

                            // Set up the buttons
                            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    passcode.set(passcodeBox.getText().toString());
                                    notify();
                                }
                            });

                            builder.show();
                        }
                    });
                    try {
                        wait();
                    } catch (InterruptedException e) {
                    }
                }

                return passcode.get();
            }
        });

        ListenableFuture<Void> sendPasscode = Futures.transformAsync(getPasscode, new AsyncFunction<String,Void>() {
            @Override
            public ListenableFuture apply(String passcode) throws Exception {
                return PhoneCommunicationApi.sendPasscode(socket,passcode);
            }
        });

        ListenableFuture<Void> waitForConfirmation = Futures.transformAsync(sendPasscode, new AsyncFunction<Void, Void>() {
            @Override
            public ListenableFuture<Void> apply(Void input) throws Exception {
                return PhoneCommunicationApi.waitForInitializationConfirmation(socket);
            }
        });

        ListenableFuture<Void> confirm =  Futures.transformAsync(waitForConfirmation, new AsyncFunction<Void, Void>() {
            @Override
            public ListenableFuture<Void> apply(Void input) throws Exception {
                return PhoneCommunicationApi.confirmInitialization(socket);
            }
        });


        ListenableFuture<Boolean> waitForFinalAck = Futures.transformAsync(confirm, new AsyncFunction<Void, Boolean>() {
            @Override
            public ListenableFuture<Boolean> apply(Void input) throws Exception {
                return PhoneCommunicationApi.waitForFinalAck(socket);
            }
        });

        Futures.catching(waitForFinalAck, Throwable.class, new Function<Throwable, Boolean>() {
            @Override
            public Boolean apply(Throwable input) {
                setStatusText("Connection failed :\n" + input.getMessage(), Color.RED);
                return null;
            }
        });

        return Futures.transform(waitForFinalAck, new Function<Boolean, Void>() {
            @Override
            public Void apply(Boolean input) {
                if(input == null){
                    //hit an exception which was already handled
                    return null;
                }

                if(input){
                    PhoneStorageAccess.setServerMacAddress(PhoneInitializeActivity.this, socket.getRemoteDevice().getAddress());
                    setStatusText("Authentication was successful!", Color.GREEN);
                } else{
                    setStatusText("Establishment of all entities failed. Authentication failed", Color.RED);
                }
                return null;
            }
        });
    }
}
