package com.jspiker.phoneauthnticator.bluetooth;

import android.bluetooth.BluetoothDevice;

import com.jspiker.phoneauthnticator.communication.CommunicationDevice;
import com.jspiker.phoneauthnticator.communication.CommunicationSocket;

import java.io.IOException;
import java.util.UUID;

/**
 * Created by jspiker on 11/12/16.
 */

public class BluetoothCommunicationDevice implements CommunicationDevice {

    private final BluetoothDevice device;

    public BluetoothCommunicationDevice(BluetoothDevice device) {
        this.device = device;
    }

    @Override
    public String getAddress() {
        return device.getAddress();
    }

    @Override
    public CommunicationSocket createCommunicationSocket(UUID uuid) throws IOException {
        return new BluetoothCommunicationSocket(device.createRfcommSocketToServiceRecord(uuid));
    }

    @Override
    public String toString(){
        return device.getName() + " : " + device.getAddress();
    }
}
