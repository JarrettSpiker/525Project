package com.jspiker.phoneauthnticator.model;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.jspiker.phoneauthnticator.CryptoUtilities;
import com.jspiker.phoneauthnticator.R;
import com.jspiker.phoneauthnticator.communication.CommunicationDevice;
import com.jspiker.phoneauthnticator.communication.CommunicationManager;
import com.jspiker.phoneauthnticator.communication.CommunicationSocket;

import java.util.Set;
import java.util.UUID;

public class PhoneAuthenticatorActivity extends AppCompatActivity {
    private static final int REQUEST_ENABLE_COMMUNICATIONS_CODE = 7;

    private TextView initText;

    private Button authButton;
    private Button resetButton;
    private TextView statusText;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_phone_authenticator);

        statusText = (TextView) findViewById(R.id.auth_status_text);

        authButton =  (Button)findViewById(R.id.authenticate_button);
        authButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startCommunications();
            }
        });


        resetButton = (Button) findViewById(R.id.reset_button);
        resetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(PhoneAuthenticatorActivity.this);
                builder.setTitle("Reinitialize")
                        .setMessage("This will reset this device to the system. Are you sure?")
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

        authButton.setEnabled(false);
    }

    @Override
    public void onResume(){
        super.onResume();
        boolean containsToken = PhoneStorageAccess.containsToken(this);
        authButton.setEnabled(containsToken);
        if(!containsToken){
            statusText.setText("You have not initialized the system.");
            statusText.setTextColor(Color.RED);
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Check which request we're responding to
        if (requestCode == REQUEST_ENABLE_COMMUNICATIONS_CODE) {
            //returning from the "enable communications" activity
            if(resultCode ==  RESULT_OK){
                attemptAuthentication();
            } else{
                handleCommunicationFailed("Could not enable communications");
            }
        }
    }

    /**
     * Ensure that communications are enabled before attempting authentication
     */
    private void startCommunications(){
        if(!CommunicationManager.manager.areCommunicationsSupported()){
            //the communication method is not supported on the device
            handleCommunicationFailed("This device does not support communications");
            return;
        }
        if(!CommunicationManager.manager.areCommunicationsEnabled()){
            startActivityForResult(CommunicationManager.manager.enableCommunicationsIntent(), REQUEST_ENABLE_COMMUNICATIONS_CODE);
        }
        else{
            attemptAuthentication();
        }
    }

    /**
     * Launch the reinit activity
     */
    private void reinitialize(){
        Intent intent = new Intent(this, PhoneInitializeActivity.class);
        startActivity(intent);
    }


    private void handleCommunicationFailed(final String reason){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                initText.setText("Connection failed\n" + reason);
            }
        });
    }


    private void attemptAuthentication() {
        Set<CommunicationDevice> pairedDevices = CommunicationManager.manager.getBondedDevices();
        String savedServerMacAddress = PhoneStorageAccess.getServerMacAddress(this);
        CommunicationDevice pairedDevice = null;
        CommunicationSocket clientSocket;
        String salt;
        byte[] tokenHash;

        CryptoUtilities cryptoUtilities = new CryptoUtilities();

        boolean connectedSuccessfully = false;

        // If there are paired devices
        if (pairedDevices.size() > 0) {

            // Loop through paired devices
            for (CommunicationDevice device : pairedDevices) {
                if (device.getAddress().equals(savedServerMacAddress) ){
                    pairedDevice = device;
                    break;
                }
            }

            try {
                clientSocket = pairedDevice.createCommunicationSocket(UUID.fromString("19ca4e12-abd6-4bcd-9937-37c8ccdad5f4"));
                clientSocket.connect();

                salt = PhoneCommunicationApi.receiveAuthenticationResponse(clientSocket).get();
                tokenHash = cryptoUtilities.hashToken(PhoneStorageAccess.getToken(this), salt.getBytes());
                PhoneCommunicationApi.sendAuthenicationRequest(clientSocket, tokenHash);
                connectedSuccessfully=PhoneCommunicationApi.waitForFinalAck(clientSocket).get();


            } catch (Exception e) {
                e.printStackTrace();
                connectedSuccessfully = false;
            }

        }
        if(connectedSuccessfully){
            statusText.setText("Your authentication was successful");
            statusText.setTextColor(Color.GREEN);
        } else {
            statusText.setText("Your authentication failed");
            statusText.setTextColor(Color.RED);
        }
    }

}