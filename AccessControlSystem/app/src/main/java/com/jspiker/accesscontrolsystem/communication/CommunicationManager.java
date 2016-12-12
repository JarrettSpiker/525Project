package com.jspiker.accesscontrolsystem.communication;

import android.content.Context;
import android.content.Intent;

import com.jspiker.accesscontrolsystem.bluetooth.BluetoothCommunicationManager;

import java.io.IOException;
import java.util.UUID;

/**
 * Created by jspiker on 11/12/16.
 */

public interface CommunicationManager {

    CommunicationServerSocket getServerSocket(Context context, UUID uuid) throws IOException;

    boolean areCommunicationsSupported();

    boolean areCommunicationsEnabled();

    Intent enableCommunicationsIntent();

    Intent enableDiscoverabilityIntent();

    public static final CommunicationManager manager = new BluetoothCommunicationManager();

}
