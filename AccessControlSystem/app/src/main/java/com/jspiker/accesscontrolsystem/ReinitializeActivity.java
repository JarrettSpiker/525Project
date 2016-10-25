package com.jspiker.accesscontrolsystem;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

public class ReinitializeActivity extends AppCompatActivity {

    Spinner numDevicesSpinner;

    int numDevices = 0;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reinitialize);

        numDevicesSpinner = (Spinner) findViewById(R.id.numItemsSpinner);
        Button confirmButton = (Button) findViewById(R.id.confirmNumDevicesButton);
        confirmButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                numDevices = numDevicesSpinner.getSelectedItemPosition()+1;
                numDevicesSpinner.setEnabled(false);
                tryToFindDevices();
            }
        });
    }

    public void tryToFindDevices(){
        TextView text = (TextView) findViewById(R.id.numDevicesText);
        text.setText("Number of devices found: 0");
        //TODO try to find devices
    }
}
