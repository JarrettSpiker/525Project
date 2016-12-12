package com.jspiker.phoneauthnticator.communication;

import java.io.IOException;
import java.util.UUID;

/**
 * Created by jspiker on 11/12/16.
 */

public interface CommunicationDevice {
    String getAddress();

    CommunicationSocket createCommunicationSocket(UUID uuid) throws IOException;
}
