package com.jspiker.phoneauthnticator.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;

import com.jspiker.phoneauthnticator.communication.CommunicationDevice;
import com.jspiker.phoneauthnticator.communication.CommunicationManager;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Created by jspiker on 11/12/16.
 */

public final class BluetoothCommunicationManager implements CommunicationManager {

    @Override
    public boolean areCommunicationsSupported() {
        return BluetoothAdapter.getDefaultAdapter() != null;
    }

    @Override
    public boolean areCommunicationsEnabled() {
        return BluetoothAdapter.getDefaultAdapter().isEnabled();
    }

    @Override
    public Intent enableCommunicationsIntent() {
        return new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
    }

    @Override
    public Intent enableDiscoverabilityIntent() {
        Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 3600);
        return discoverableIntent;
    }

    @Override
    public Set<CommunicationDevice> getBondedDevices() {
        Set<BluetoothDevice> bluetoothDevices = BluetoothAdapter.getDefaultAdapter().getBondedDevices();
        Set<CommunicationDevice> communicationDevices = new HashSet<>();
        for(BluetoothDevice device : bluetoothDevices){
            communicationDevices.add(new BluetoothCommunicationDevice(device));
        }
        return  communicationDevices;
    }

    @Override
    public String getDeviceFoundCode() {
        return BluetoothDevice.ACTION_FOUND;
    }

    @Override
    public CommunicationDevice getDeviceFromDiscoveryIntent(Intent intent) {
        BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
        return new BluetoothCommunicationDevice(device);
    }

    @Override
    public void startDiscovery() {
        BluetoothAdapter.getDefaultAdapter().startDiscovery();
    }

    @Override
    public void cancelDiscovery() {
        BluetoothAdapter.getDefaultAdapter().cancelDiscovery();
    }
}
