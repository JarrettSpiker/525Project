package com.jspiker.accesscontrolsystem;

import android.bluetooth.BluetoothSocket;

import com.google.common.base.Function;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by jspiker on 11/1/16.
 */

public class CommunicationApi {

    public static ListenableFuture<Void> sendTokenAndPasscode(final BluetoothSocket socket, final String token, boolean passcode){
        return Threading.runOnBackgroundThread(new Function<Void, Void>() {
            @Override
            public Void apply(Void input) {

                String res = null;
                try {
                    OutputStream stream = socket.getOutputStream();
                    stream.write(token.getBytes()); //TODO this is just a stub
                } catch (IOException e) {
                    e.printStackTrace();
                }

                return null;
            }
        });


    }


    public static ListenableFuture<String> receivePasscode(){
        //TODO this is a stub
        return Futures.immediateFuture(null);
    }

}
