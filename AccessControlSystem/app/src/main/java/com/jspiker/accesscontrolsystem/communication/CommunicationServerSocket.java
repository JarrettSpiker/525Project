package com.jspiker.accesscontrolsystem.communication;

import java.io.IOException;

/**
 * Created by jspiker on 11/12/16.
 */

public interface CommunicationServerSocket {

    CommunicationSocket waitForCommunicationSocket() throws IOException;

    void close() throws IOException;
}
