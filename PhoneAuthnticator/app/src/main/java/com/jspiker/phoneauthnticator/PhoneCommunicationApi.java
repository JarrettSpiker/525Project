package com.jspiker.phoneauthnticator;

import android.bluetooth.BluetoothSocket;

import com.google.common.base.Function;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created by jspiker on 11/1/16.
 */

public class PhoneCommunicationApi {

    public static ListenableFuture<TokenAndPasscodeResponse> getTokenAndPasscode(final BluetoothSocket socket){
        return Threading.runOnBackgroundThread(new Function<Void, TokenAndPasscodeResponse>() {
            @Override
            public TokenAndPasscodeResponse apply(Void input) {

                String res = null;
                try {
                    InputStream stream = socket.getInputStream();
                    res = String.valueOf(stream.read()); //TODO this is just a stub
                } catch (IOException e) {
                    e.printStackTrace();
                }

                return new TokenAndPasscodeResponse(res, true);//TODO this is just a stub!
            }
        });


    }


    public static ListenableFuture<Void> sendPasscode(String passcode){
        //TODO this is a stub
        return Futures.immediateFuture(null);
    }

    public static ListenableFuture<Void> waitForInitializationConfirmation(){
        //TODO this is a stub
        return Futures.immediateFuture(null);
    }

    public static ListenableFuture<Void> confirmInitialization(){
        //TODO this is a stub
        return Futures.immediateFuture(null);
    }

    public static ListenableFuture<Boolean> waitForFinalAck(){
        //TODO this is a stub; the Boolean is whether the Ack is positive or negative
        return Futures.immediateFuture(false);
    }

    public static class TokenAndPasscodeResponse{
        public final String token;
        public final boolean isPasscodeRequired;
        public TokenAndPasscodeResponse(String token, boolean isPasscodeRequired){
            this.token = token;
            this.isPasscodeRequired = isPasscodeRequired;
        }
    }

}
