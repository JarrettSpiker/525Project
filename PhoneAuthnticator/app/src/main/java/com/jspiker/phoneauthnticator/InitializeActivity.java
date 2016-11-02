package com.jspiker.phoneauthnticator;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.InputType;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.appindexing.Thing;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.common.base.Function;
import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class InitializeActivity extends AppCompatActivity {

    private static final String UUID_STRING = "525ProjectUUID"; //This must be the same in both the client and the server

    private static final int REQUEST_ENABLE_BT_CODE = 7;


    TextView statusText;
    ListView foundDevices;

    ArrayAdapter<BluetoothDevice> deviceAdapter;

    boolean discoveryStarted = false;

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            // When discovery finds a device
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                deviceFound(device);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_initialize);
        statusText = (TextView) findViewById(R.id.initialize_status);

        foundDevices = (ListView) findViewById(R.id.found_devices_list);
        foundDevices.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                bluetoothServerSelected(deviceAdapter.getItem(position));
            }
        });
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(receiver, filter);
    }

    @Override
    protected void onStart() {
        super.onStart();
        setStatusText("Searching for devices...", Color.GRAY);

        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            setStatusText("This device does not support bluetooth", Color.RED);
            return;
        }
        if (!bluetoothAdapter.isEnabled()) {
            Intent startBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(startBluetooth, REQUEST_ENABLE_BT_CODE);
        } else {
            startDiscovery();
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // Check which request we're responding to
        if (requestCode == REQUEST_ENABLE_BT_CODE) {
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

        unregisterReceiver(receiver);
    }

    private void startDiscovery() {
        foundDevices.setEnabled(true);
        discoveryStarted = true;
        deviceAdapter = new ArrayAdapter<BluetoothDevice>(this, android.R.layout.simple_list_item_1);
        BluetoothAdapter.getDefaultAdapter().startDiscovery();
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


    private void deviceFound(BluetoothDevice device) {
        if (!discoveryStarted) {
            return;
        }

        deviceAdapter.add(device);
    }

    private void bluetoothServerSelected(final BluetoothDevice device) {
        discoveryStarted = false;
        BluetoothAdapter.getDefaultAdapter().cancelDiscovery();
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

    private ListenableFuture<Void> handleConnectedSocketAsync(BluetoothSocket socket) {
        final ListenableFuture<CommunicationApi.TokenAndPasscodeResponse> getTokenAndPasscode = CommunicationApi.getTokenAndPasscode(socket);

        final AtomicBoolean passcodeRequired = new AtomicBoolean(false);

        ListenableFuture<Void> saveToken = Futures.transform(getTokenAndPasscode, new Function<CommunicationApi.TokenAndPasscodeResponse, Void>() {
            @Override
            public Void apply(CommunicationApi.TokenAndPasscodeResponse response) {
                StorageAccess.setToken(InitializeActivity.this, response.token);
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
                            AlertDialog.Builder builder = new AlertDialog.Builder(InitializeActivity.this);
                            builder.setTitle("Enter passcode");

                            // Set up the input
                            final EditText passcodeBox = new EditText(InitializeActivity.this);
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
                return CommunicationApi.sendPasscode(passcode);
            }
        });

        ListenableFuture<Void> waitForconfirmation = Futures.transformAsync(sendPasscode, new AsyncFunction<Void, Void>() {
            @Override
            public ListenableFuture<Void> apply(Void input) throws Exception {
                return CommunicationApi.waitForInitializationConfirmation();
            }
        });

        return Futures.transformAsync(waitForconfirmation, new AsyncFunction<Void, Void>() {
            @Override
            public ListenableFuture<Void> apply(Void input) throws Exception {
                return CommunicationApi.confirmInitialization();
            }
        });
    }
}
