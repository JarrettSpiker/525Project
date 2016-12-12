package com.jspiker.phoneauthnticator.communication;

import android.content.Context;
import android.content.Intent;

import com.jspiker.phoneauthnticator.bluetooth.BluetoothCommunicationManager;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;

/**
 * Created by jspiker on 11/12/16.
 */

public interface CommunicationManager {

    boolean areCommunicationsSupported();

    boolean areCommunicationsEnabled();

    Intent enableCommunicationsIntent();

    Intent enableDiscoverabilityIntent();

    Set<CommunicationDevice> getBondedDevices();

    String getDeviceFoundCode();

    CommunicationDevice getDeviceFromDiscoveryIntent(Intent intent);

    void startDiscovery();
    void cancelDiscovery();

    public static final CommunicationManager manager = new BluetoothCommunicationManager();

}
