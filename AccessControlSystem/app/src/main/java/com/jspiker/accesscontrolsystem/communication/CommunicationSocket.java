package com.jspiker.accesscontrolsystem.communication;

import java.io.IOException;

/**
 * Created by jspiker on 11/12/16.
 */

public interface CommunicationSocket {

    void write(byte[] bytes) throws IOException;

    void read(byte[] bytes) throws IOException;

    String getAddress();
}
