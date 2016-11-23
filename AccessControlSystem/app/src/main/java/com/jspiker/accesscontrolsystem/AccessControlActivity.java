package com.jspiker.accesscontrolsystem;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.google.common.base.Function;
import com.google.common.primitives.Booleans;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

public class AccessControlActivity extends AppCompatActivity {

    private static final String UUID_STRING = "525ProjectUUID"; //This must be the same in both the client and the server

    private CryptoUtilities cryptoUtilities = new CryptoUtilities();
    private AccessControlCommunicationApi communicationApi = new AccessControlCommunicationApi();
    private AccessControlInitializeActivity initializeActivity = new AccessControlInitializeActivity();

    private ListenableFuture<Void> waitForAuthenticationRequestThread;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_access_control);

        Button reinitButton = (Button) findViewById(R.id.reinitialize_button);
        reinitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(AccessControlActivity.this);
                builder.setTitle("Reinitialize")
                        .setMessage("This will reset the system. Are you sure?")
                        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                //Yes button clicked, do something
                                reinitialize();
                            }
                        })
                        .setNegativeButton("No", null)
                        .show();

            }
        });
        waitForAuthenticationRequestThread = Threading.runOnBackgroundThread(waitForAuthenticationRequests);
    }


    private void reinitialize(){
        Intent intent = new Intent(this, AccessControlInitializeActivity.class);
        startActivity(intent);
    }

    private byte[] getSalt(){
        return cryptoUtilities.generateSalt();
    }

    private Function<Void,Void> waitForAuthenticationRequests =  new Function<Void, Void>() {
        @Override
        public Void apply(Void input) {
            BluetoothServerSocket serverSocket = null;
            try {
                try {
                    serverSocket = BluetoothAdapter.getDefaultAdapter().listenUsingRfcommWithServiceRecord("Wait for authentication requests", UUID.fromString(initializeActivity.getUuidString()));
                } catch (IOException e) {
                    return  null;
                }

                BluetoothSocket socket = null;
                final AtomicInteger authenticatedSoFar = new AtomicInteger(0);
                String macAddress;
                String deviceToken;
                ArrayList<ListenableFuture<Boolean>> authenticationThreads = new ArrayList<>();
                //keep looking until we find enough devices
                while (authenticatedSoFar.get() < AccessControlStorage.getNumDevices(AccessControlActivity.this)) {
                    try {
                        socket = serverSocket.accept(10000);
                    } catch (IOException e) {
                        continue;
                    }

                    if (socket != null) {
                        macAddress = socket.getRemoteDevice().getAddress();
                        deviceToken = AccessControlStorage.getTokenForDevice(AccessControlActivity.this, macAddress);
                        Futures.transform(authenticateDevice(deviceToken, socket), new Function<Boolean, Void>() {
                            @Override
                            public Void apply(Boolean input) {

                                if(input) authenticatedSoFar.getAndIncrement();
                                return null;
                            }
                        });
                    }

                }

                Futures.whenAllSucceed(authenticationThreads).call(new Callable<Void>() {
                    @Override
                    public Void call() throws Exception {
                        // Successfully unlock the system

                        return null;
                    }
                });

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



    private ListenableFuture<Boolean> authenticateDevice(final String deviceToken, final BluetoothSocket bluetoothSocket){
        ListenableFuture<Void> switchtoBackground = Threading.switchToBackground();

        ListenableFuture<Boolean> authenticate = Futures.transform(switchtoBackground, new Function<Void, Boolean>() {
            @Override
            public Boolean apply(Void input) {
                byte [] salt;
                //byte [] receivedTokenHash;   So that I can compile
                //byte [] receivedTokenHash = null;

                boolean authenticated;
                boolean received;
                String receivedTokenHash = "";
                received = false;

                // Generate the salt
                salt = getSalt();

                // Step 1: Send salt to device
                AccessControlCommunicationApi.sendAuthenicationRequest(bluetoothSocket, new String (salt));

                // Step 2: Wait for the receivedHashedToken
                while(!received){
                    try {
                        receivedTokenHash = AccessControlCommunicationApi.receiveAuthenticationResponse(bluetoothSocket).get();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        return false;
                    } catch (ExecutionException e) {
                        e.printStackTrace();
                        return false;
                    }
                    if (receivedTokenHash != null || receivedTokenHash != "") received = true;
                }

                authenticated = cryptoUtilities.verifyTokenHash(deviceToken, salt, receivedTokenHash.getBytes());

                // Send fail/success response back to the phone
                AccessControlCommunicationApi.sendFinalAck(bluetoothSocket, authenticated);

                return authenticated;
            }
        });
    }


}
