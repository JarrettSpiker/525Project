package com.jspiker.accesscontrolsystem.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;

import com.jspiker.accesscontrolsystem.communication.CommunicationManager;
import com.jspiker.accesscontrolsystem.communication.CommunicationServerSocket;

import java.io.IOException;
import java.util.UUID;

/**
 * Created by jspiker on 11/12/16.
 */

public final class BluetoothCommunicationManager implements CommunicationManager {

    @Override
    public CommunicationServerSocket getServerSocket(Context context, UUID uuid) throws IOException {
        return new BluetoothCommunicationServerSocket(BluetoothAdapter.getDefaultAdapter().listenUsingRfcommWithServiceRecord("Wait for connection requests", uuid));
    }

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
}
