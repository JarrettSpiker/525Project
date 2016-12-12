package com.jspiker.phoneauthnticator.bluetooth;

import android.bluetooth.BluetoothSocket;

import com.jspiker.phoneauthnticator.communication.CommunicationSocket;

import java.io.IOException;

/**
 * Created by jspiker on 11/12/16.
 */

public class BluetoothCommunicationSocket  implements CommunicationSocket {

    private final BluetoothSocket bluetoothSocket;

    public BluetoothCommunicationSocket(BluetoothSocket socket){
        this.bluetoothSocket = socket;
    }


    @Override
    public void write(byte[] bytes) throws IOException {
        bluetoothSocket.getOutputStream().write(bytes);
    }

    @Override
    public void read(byte[] buffer) throws IOException {
        bluetoothSocket.getInputStream().read(buffer);
    }

    @Override
    public String getAddress() {
        return bluetoothSocket.getRemoteDevice().getAddress();
    }

    @Override
    public void connect() throws IOException {
        bluetoothSocket.connect();
    }
}
