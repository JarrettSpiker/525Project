package com.jspiker.phoneauthnticator;

import android.bluetooth.BluetoothAdapter;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class PhoneAuthenticatorActivity extends AppCompatActivity {
    private static final int REQUEST_ENABLE_BT_CODE = 7;

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
                boolean success = false;
                startBluetooth();

                //TODO add seperate messages for failure and no bluetooth device found
                statusText = (TextView) findViewById(R.id.auth_status_text);
                if(success){
                    statusText.setText("Authenticated Successfully!");
                    statusText.setTextColor(Color.GREEN);
                } else{
                    statusText.setText("Authentication Failed!");
                    statusText.setTextColor(Color.RED);
                }

            }
        });


        resetButton = (Button) findViewById(R.id.reset_button);
        resetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(PhoneAuthenticatorActivity.this);
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

        authButton.setEnabled(false);
    }

    @Override
    public void onResume(){
        super.onResume();
        boolean containsToken = StorageAccess.containsToken(this);
        authButton.setEnabled(containsToken);
        if(!containsToken){
            statusText.setText("You have not initialized the system. Please press Reset");
            statusText.setTextColor(Color.RED);
        }
    }

    private void reinitialize(){
        Intent intent = new Intent(this, InitializeActivity.class);
        startActivity(intent);
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

    private void handleBluetoothFailed(final String reason){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                initText.setText("Bluetooth connection failed\n" + reason);
            }
        });
    }

    private void tryToFindDevices() {


        // Ask about the threads and copying the socket code


    }

}