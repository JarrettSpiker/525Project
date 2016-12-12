package com.jspiker.accesscontrolsystem.model;

import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.jspiker.accesscontrolsystem.Threading;
import com.jspiker.accesscontrolsystem.communication.CommunicationSocket;

import org.json.JSONObject;

/**
 * Created by jspiker on 11/1/16.
 */

public class AccessControlCommunicationApi {

    private static final String passCodeRequiredKey = "passcodeRequired";
    private static final String passcodeKey = "passcode";
    private static final String tokenKey = "token";
    private static final String serverConfirmationKey = "serverConfirmation";
    private static final String clientConfirmationKey = "clientConfirmation";
    private static final String finalAckKey = "finalAck";
    private static final String serverSaltForAuthentication = "serverSaltForAuthentication";
    private static final String hashedToken = "hashedToken";

    public static ListenableFuture<Void> sendTokenAndPasscode(final CommunicationSocket socket, final String token, final boolean passcodeRequired){
        return Futures.transformAsync(Threading.switchToBackground(),
                new AsyncFunction<Void, Void>() {
                    @Override
                    public ListenableFuture<Void> apply(Void input) throws Exception {
                        JSONObject jsonObject = new JSONObject();
                        jsonObject.put(passCodeRequiredKey, passcodeRequired);
                        jsonObject.put(tokenKey, token);
                        socket.write(jsonObject.toString().getBytes());
                        return Futures.immediateFuture(null);
                    }
                });
    }


    public static ListenableFuture<String> receivePasscode(final CommunicationSocket socket){
        return Futures.transformAsync(Threading.switchToBackground(), new AsyncFunction<Void, String>() {
            @Override
            public ListenableFuture<String> apply(Void input) throws Exception {
                byte[] bytes = new byte[500];
                socket.read(bytes);
                JSONObject json = new JSONObject(new String(bytes));
                String passcode = json.getString(passcodeKey);
                return Futures.immediateFuture(passcode);
            }
        });
    }

    public static ListenableFuture<Void> sendConfirmation(final CommunicationSocket socket){
        return Futures.transformAsync(Threading.switchToBackground(),
                new AsyncFunction<Void, Void>() {
                    @Override
                    public ListenableFuture<Void> apply(Void input) throws Exception {
                        JSONObject jsonObject = new JSONObject();
                        jsonObject.put(serverConfirmationKey, true);
                        socket.write(jsonObject.toString().getBytes());
                        return Futures.immediateFuture(null);
                    }
                });
    }

    public static ListenableFuture<Boolean> receiveConfirmationResponse(final CommunicationSocket socket){
        return Futures.transformAsync(Threading.switchToBackground(), new AsyncFunction<Void, Boolean>() {
            @Override
            public ListenableFuture<Boolean> apply(Void input) throws Exception {
                byte[] bytes = new byte[500];
                socket.read(bytes);
                JSONObject json = new JSONObject(new String(bytes));
                Boolean confirmation = json.optBoolean(clientConfirmationKey, false);
                return Futures.immediateFuture(confirmation);
            }
        });
    }

    public static ListenableFuture<Void> sendFinalAck(final CommunicationSocket socket, final boolean positive){
        return Futures.transformAsync(Threading.switchToBackground(),
                new AsyncFunction<Void, Void>() {
                    @Override
                    public ListenableFuture<Void> apply(Void input) throws Exception {
                        JSONObject jsonObject = new JSONObject();
                        jsonObject.put(finalAckKey, positive);
                        socket.write(jsonObject.toString().getBytes());
                        return Futures.immediateFuture(null);
                    }
                });
    }

    public static ListenableFuture<Void> sendAuthenicationRequest(final CommunicationSocket socket, final String message){
        return Futures.transformAsync(Threading.switchToBackground(),
                new AsyncFunction<Void, Void>() {
                    @Override
                    public ListenableFuture<Void> apply(Void input) throws Exception {
                        JSONObject jsonObject = new JSONObject();
                        jsonObject.put(serverSaltForAuthentication, message);
                        socket.write(jsonObject.toString().getBytes());
                        return Futures.immediateFuture(null);
                    }
                });
    }

    public static ListenableFuture<byte[]> receiveAuthenticationResponse (final CommunicationSocket socket){
        return Futures.transformAsync(Threading.switchToBackground(), new AsyncFunction<Void, byte[]>() {
            @Override
            public ListenableFuture<byte[]> apply(Void input) throws Exception {
                byte[] bytes = new byte[500];
                socket.read(bytes);
                return Futures.immediateFuture(bytes);
            }
        });
    }
}
