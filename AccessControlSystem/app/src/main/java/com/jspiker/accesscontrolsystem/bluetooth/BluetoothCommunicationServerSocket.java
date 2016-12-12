package com.jspiker.accesscontrolsystem.bluetooth;

import android.bluetooth.BluetoothServerSocket;

import com.jspiker.accesscontrolsystem.communication.CommunicationServerSocket;
import com.jspiker.accesscontrolsystem.communication.CommunicationSocket;

import java.io.IOException;

/**
 * Created by jspiker on 11/12/16.
 */

public class BluetoothCommunicationServerSocket implements CommunicationServerSocket {
    private final BluetoothServerSocket bluetoothServerSocket;

    public BluetoothCommunicationServerSocket(BluetoothServerSocket bluetoothServerSocket) {
        this.bluetoothServerSocket = bluetoothServerSocket;
    }

    @Override
    public CommunicationSocket waitForCommunicationSocket() throws IOException {
        return new BluetoothCommunicationSocket(bluetoothServerSocket.accept());
    }

    @Override
    public void close() throws IOException {
        bluetoothServerSocket.close();
    }
}
