package com.jspiker.phoneauthnticator.model;

import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.jspiker.phoneauthnticator.Threading;
import com.jspiker.phoneauthnticator.communication.CommunicationSocket;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by jspiker on 11/1/16.
 */

public class PhoneCommunicationApi {

    private static final String passCodeRequiredKey = "passcodeRequired";
    private static final String passcodeKey = "passcode";
    private static final String tokenKey = "token";
    private static final String serverConfirmationKey = "serverConfirmation";
    private static final String clientConfirmationKey = "clientConfirmation";
    private static final String finalAckKey = "finalAck";
    private static final String serverSaltForAuthentication = "serverSaltForAuthentication";
    private static final String hashedTokenKey = "hashedToken";


    public static ListenableFuture<TokenAndPasscodeResponse> getTokenAndPasscodeRequired(final CommunicationSocket socket){
        return Futures.transformAsync(Threading.switchToBackground(), new AsyncFunction<Void, TokenAndPasscodeResponse>() {
            @Override
            public ListenableFuture<TokenAndPasscodeResponse> apply(Void input) throws Exception {
                byte[] bytes = new byte[500];
                socket.read(bytes);
                String s = new String(bytes);
                JSONObject json = new JSONObject(s);
                TokenAndPasscodeResponse response = new TokenAndPasscodeResponse(json.getString(tokenKey), json.getBoolean(passCodeRequiredKey));
                return Futures.immediateFuture(response);
            }
        });

    }


    public static ListenableFuture<Void> sendPasscode(final CommunicationSocket socket, final String passcode) throws JSONException{
        return Futures.transformAsync(Threading.switchToBackground(),
                new AsyncFunction<Void, Void>() {
                    @Override
                    public ListenableFuture<Void> apply(Void input) throws Exception {
                        JSONObject jsonObject = new JSONObject();
                        jsonObject.put(passcodeKey, passcode);
                        socket.write(jsonObject.toString().getBytes());
                        return Futures.immediateFuture(null);
                    }
                });
    }

    public static ListenableFuture<Void> waitForInitializationConfirmation(final CommunicationSocket socket){
        return Futures.transformAsync(Threading.switchToBackground(), new AsyncFunction<Void, Void>() {
            @Override
            public ListenableFuture<Void> apply(Void input) throws Exception {
                byte[] bytes = new byte[500];
                socket.read(bytes);
                JSONObject json = new JSONObject(new String(bytes));
                boolean confirm = json.getBoolean(serverConfirmationKey);
                if(!confirm){
                    throw new RuntimeException("Confirmation rejected");
                }
                return Futures.immediateFuture(null);
            }
        });
    }

    public static ListenableFuture<Void> confirmInitialization(final CommunicationSocket socket){
        return Futures.transformAsync(Threading.switchToBackground(),
                new AsyncFunction<Void, Void>() {
                    @Override
                    public ListenableFuture<Void> apply(Void input) throws Exception {
                        JSONObject jsonObject = new JSONObject();
                        jsonObject.put(clientConfirmationKey, true);
                        socket.write(jsonObject.toString().getBytes());
                        return Futures.immediateFuture(null);
                    }
                });
    }

    public static ListenableFuture<Boolean> waitForFinalAck(final CommunicationSocket socket){
        return Futures.transformAsync(Threading.switchToBackground(), new AsyncFunction<Void, Boolean>() {
            @Override
            public ListenableFuture<Boolean> apply(Void input) throws Exception {
                byte[] bytes = new byte[500];
                socket.read(bytes);
                JSONObject json = new JSONObject(new String(bytes));
                boolean ack = json.getBoolean(finalAckKey);
                return Futures.immediateFuture(ack);
            }
        });
    }

    public static ListenableFuture<Void> sendAuthenicationRequest(final CommunicationSocket socket, final byte[] hashedToken){
        return Futures.transformAsync(Threading.switchToBackground(),
                new AsyncFunction<Void, Void>() {
                    @Override
                    public ListenableFuture<Void> apply(Void input) throws Exception {
                        socket.write(hashedToken);
                        return Futures.immediateFuture(null);
                    }
                });
    }

    public static ListenableFuture<String> receiveAuthenticationResponse (final CommunicationSocket socket){
        return Futures.transformAsync(Threading.switchToBackground(), new AsyncFunction<Void, String>() {
            @Override
            public ListenableFuture<String> apply(Void input) throws Exception {
                byte[] bytes = new byte[500];
                socket.read(bytes);
                JSONObject json = new JSONObject(new String(bytes));
                String salt = json.getString(serverSaltForAuthentication);
                return Futures.immediateFuture(salt);
            }
        });
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
