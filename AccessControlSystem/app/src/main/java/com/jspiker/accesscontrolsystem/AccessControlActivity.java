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
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.IOException;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.Callable;

public class AccessControlActivity extends AppCompatActivity {

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
                    serverSocket = BluetoothAdapter.getDefaultAdapter().listenUsingRfcommWithServiceRecord("Wait for authentication requests", UUID.fromString(UUID_STRING));
                } catch (IOException e) {
                    return  null;
                }

                BluetoothSocket socket = null;
                int authenticatedSoFar = 0;
                String macAddress;
                String deviceToken;
                ArrayList<ListenableFuture<Void>> connections = new ArrayList<>();
                //keep looking until we find enough devices
                while (authenticatedSoFar < initializeActivity.numDevices) {
                    try {
                        socket = serverSocket.accept();
                    } catch (IOException e) {
                        continue;
                    }

                    if (socket != null) {
                        macAddress = socket.getRemoteDevice().getAddress();
                        deviceToken = AccessControlStorage.getTokenForDevice(getParent(), macAddress);
                        authenticateDevice(deviceToken, socket);
                    }
                }

                Futures.whenAllSucceed(connections).call(new Callable<Void>() {
                    @Override
                    public Void call() throws Exception {

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


    private boolean authenticateDevice(String deviceToken, BluetoothSocket bluetoothSocket){
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
        AccessControlCommunicationApi.sendAuthenicationRequest(bluetoothSocket, salt.toString());

        // Step 2: Wait for the receivedHashedToken
        while(!received){
            receivedTokenHash = AccessControlCommunicationApi.receiveAuthenticationResponse(bluetoothSocket).toString();
            if (receivedTokenHash != null || receivedTokenHash != "") received = true;
        }

        authenticated = cryptoUtilities.verifyTokenHash(deviceToken, salt, receivedTokenHash.getBytes());

        return authenticated;
    }


}
