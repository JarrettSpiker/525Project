package com.jspiker.phoneauthnticator;

import android.graphics.Color;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class PhoneAuthenticatorActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_phone_authenticator);

        Button authButton =  (Button)findViewById(R.id.authenticate_button);
        authButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean success = false;
                //TODO this is where youd try to authenticate

                //TODO add seperate messages for failure and no bluetooth device found
                TextView statusText = (TextView) findViewById(R.id.auth_status_text);
                if(success){
                    statusText.setText("Authenticated Successfully!");
                    statusText.setTextColor(Color.GREEN);
                } else{
                    statusText.setText("Authentication Failed!");
                    statusText.setTextColor(Color.RED);
                }

            }
        });
    }
}
