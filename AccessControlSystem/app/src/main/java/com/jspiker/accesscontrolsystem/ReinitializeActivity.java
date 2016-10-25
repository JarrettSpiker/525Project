package com.jspiker.accesscontrolsystem;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;

public class ReinitializeActivity extends AppCompatActivity {

    private static final String numberOfDevicesFound = "Number of devices found: ";

    private Spinner numDevicesSpinner;
    private Switch requirePasscodeSwitch;

    private int numDevices = 0;
    private int foundSoFar = 0;
    private boolean requirePasscode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reinitialize);

        requirePasscodeSwitch = (Switch) findViewById(R.id.requirePasscodeSwitch);
        numDevicesSpinner = (Spinner) findViewById(R.id.numItemsSpinner);
        Button confirmButton = (Button) findViewById(R.id.confirmNumDevicesButton);
        confirmButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                numDevices = numDevicesSpinner.getSelectedItemPosition()+1;
                requirePasscode = requirePasscodeSwitch.isChecked();
                requirePasscodeSwitch.setEnabled(false);
                numDevicesSpinner.setEnabled(false);
                tryToFindDevices();
            }
        });
    }

    private void tryToFindDevices(){
        TextView text = (TextView) findViewById(R.id.numDevicesText);
        text.setText(numberOfDevicesFound + foundSoFar);
        //TODO try to find devices
    }
}
