package com.jspiker.accesscontrolsystem;

import android.content.DialogInterface;
import android.content.Intent;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class AccessControlActivity extends AppCompatActivity {

    private CryptoUtilities cryptoUtilities = new CryptoUtilities();
    private AccessControlCommunicationApi communicationApi = new AccessControlCommunicationApi();
    private AccessControlInitializeActivity initializeActivity = new AccessControlInitializeActivity();

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
    }


    private void reinitialize(){
        Intent intent = new Intent(this, AccessControlInitializeActivity.class);
        startActivity(intent);
    }

    private byte[] getSalt(){
        return cryptoUtilities.generateSalt();
    }

    private boolean authenticateDevice(String deviceToken){
        byte [] salt;
        byte [] receivedTokenHash;
        salt = getSalt();
        boolean authenticated;

        // Step 1: Send salt to device


        // Step 2: Wait for the receivedHashedToken

        authenticated = cryptoUtilities.verifyTokenHash(deviceToken, salt, receivedTokenHash);

        return authenticated;
    }

}
