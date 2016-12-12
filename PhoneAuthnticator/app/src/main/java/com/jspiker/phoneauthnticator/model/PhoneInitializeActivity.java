package com.jspiker.phoneauthnticator.model;

import android.Manifest;
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
import com.jspiker.phoneauthnticator.R;
import com.jspiker.phoneauthnticator.Threading;
import com.jspiker.phoneauthnticator.communication.CommunicationDevice;
import com.jspiker.phoneauthnticator.communication.CommunicationManager;
import com.jspiker.phoneauthnticator.communication.CommunicationSocket;

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
    ArrayAdapter<CommunicationDevice> deviceAdapter;

    boolean discoveryStarted = false;

    //notifies when we have found a potential server
    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            // When discovery finds a device
            if (CommunicationManager.manager.getDeviceFoundCode().equals(action)) {
                deviceAdapter.add(CommunicationManager.manager.getDeviceFromDiscoveryIntent(intent));
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_initialize);
        statusText = (TextView) findViewById(R.id.initialize_status);

        IntentFilter filter = new IntentFilter(CommunicationManager.manager.getDeviceFoundCode());
        registerReceiver(receiver, filter);

        if (!CommunicationManager.manager.areCommunicationsSupported()) {
            setStatusText("This device does not support communications", Color.RED);
            return;
        }

        deviceAdapter = new ArrayAdapter<CommunicationDevice>(this, android.R.layout.simple_list_item_1);

        foundDevices = (ListView) findViewById(R.id.found_devices_list);
        foundDevices.setAdapter( deviceAdapter );
        foundDevices.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                serverSelected(deviceAdapter.getItem(position));
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        setStatusText("Searching for devices...", Color.GRAY);

        if (!CommunicationManager.manager.areCommunicationsEnabled()) {
            startActivityForResult(CommunicationManager.manager.enableCommunicationsIntent(), REQUEST_ENABLE_BT);
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

        //make sure that our broadcast receiver is listening
        IntentFilter intFilter = new IntentFilter();
        intFilter.addAction(CommunicationManager.manager.getDeviceFoundCode());
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
            //returning from the "enable communications" activity
            if (resultCode == RESULT_OK) {
                startDiscovery();
            } else {
                setStatusText("Could not enable communications", Color.RED);
            }
        }
    }

    /**
     * Try to find servers
     */
    private void startDiscovery() {

        //if we dont have location permissions, request them
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                    REQUEST_COARSE_LOCATION);
            return;
        }


        foundDevices.setEnabled(true);

        discoveryStarted = true;
        deviceAdapter = new ArrayAdapter<CommunicationDevice>(this, android.R.layout.simple_list_item_1);
        foundDevices.setAdapter(deviceAdapter);
        CommunicationManager.manager.startDiscovery();
    }


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

    /**
     * Called when the user has selected a server to authenticate with
     * Attempt to get a token from that server, and send it a passcode
     */
    private void serverSelected(final CommunicationDevice device) {
        discoveryStarted = false;
        CommunicationManager.manager.cancelDiscovery();

        //open a connection with the selected derver
        ListenableFuture<CommunicationSocket> getSocket =
                Threading.runOnBackgroundThread(new Function<Void, CommunicationSocket>() {
                    @Override
                    public CommunicationSocket apply(Void input) {
                        CommunicationSocket socket = null;
                        try {
                            socket = device.createCommunicationSocket(UUID.fromString(UUID_STRING));
                            socket.connect();
                            return socket;
                        } catch (IOException e) {
                            setStatusText("Failed to establish link: " + e.getMessage(), Color.RED);
                        }
                        return null;
                    }
                });


        Futures.transformAsync(getSocket, new AsyncFunction<CommunicationSocket, Void>() {
            @Override
            public ListenableFuture<Void> apply(CommunicationSocket socket) {
                if (socket == null) {
                    return Futures.immediateFuture(null);
                }
                return handleConnectedSocketAsync(socket);
            }
        });

    }

    /**
     * get the token from, and send the passcode to a connected socket. Then wait for confirmation from the server
     * @param socket
     * @return
     */
    private ListenableFuture<Void> handleConnectedSocketAsync(final CommunicationSocket socket) {

        //get the token, and whether or not we need a passcode from the server
        final ListenableFuture<PhoneCommunicationApi.TokenAndPasscodeResponse> getTokenAndPasscode = PhoneCommunicationApi.getTokenAndPasscodeRequired(socket);

        final AtomicBoolean passcodeRequired = new AtomicBoolean(false);

        //persist the received token
        ListenableFuture<Void> saveToken = Futures.transform(getTokenAndPasscode, new Function<PhoneCommunicationApi.TokenAndPasscodeResponse, Void>() {
            @Override
            public Void apply(PhoneCommunicationApi.TokenAndPasscodeResponse response) {
                PhoneStorageAccess.setToken(PhoneInitializeActivity.this, response.token);
                passcodeRequired.set(response.isPasscodeRequired);
                return null;
            }
        });

        //if we require a passcode, get one from the user
        ListenableFuture<String> getPasscode = Futures.transform(saveToken, new Function<Void, String>() {
            @Override
            public String apply(Void input) {

                final AtomicReference<String> passcode = new AtomicReference<>("");
                final Object o = new Object();
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
                                    synchronized (o){
                                        o.notify();
                                    }

                                }
                            });

                            builder.show();
                        }
                    });
                    try {
                        synchronized (o) {
                            o.wait();
                        }
                    } catch (InterruptedException e) {
                    }
                }

                return passcode.get();
            }
        });

        //send the passcode (even if it is empty) to the server
        ListenableFuture<Void> sendPasscode = Futures.transformAsync(getPasscode, new AsyncFunction<String,Void>() {
            @Override
            public ListenableFuture apply(String passcode) throws Exception {
                return PhoneCommunicationApi.sendPasscode(socket,passcode);
            }
        });

        //wait for the server to confirm that all devices have registered
        ListenableFuture<Void> waitForConfirmation = Futures.transformAsync(sendPasscode, new AsyncFunction<Void, Void>() {
            @Override
            public ListenableFuture<Void> apply(Void input) throws Exception {
                return PhoneCommunicationApi.waitForInitializationConfirmation(socket);
            }
        });

        //acknowledge the server's confirmation
        ListenableFuture<Void> confirm =  Futures.transformAsync(waitForConfirmation, new AsyncFunction<Void, Void>() {
            @Override
            public ListenableFuture<Void> apply(Void input) throws Exception {
                return PhoneCommunicationApi.confirmInitialization(socket);
            }
        });

        //wait for the server to confirm that all devices have acknowledged
        //if they have, then registration was successful. Failure otherwise
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
                //if successful...
                if(input){

                    //persist the server's address (so that we can reconnect later) and notify the user
                    PhoneStorageAccess.setServerMacAddress(PhoneInitializeActivity.this, socket.getAddress());
                    setStatusText("Registration was successful!", Color.GREEN);
                } else{
                    setStatusText("Establishment of all entities failed. Authentication failed", Color.RED);
                }
                return null;
            }
        });
    }
}
