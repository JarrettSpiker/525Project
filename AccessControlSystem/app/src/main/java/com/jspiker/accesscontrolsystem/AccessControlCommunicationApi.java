package com.jspiker.accesscontrolsystem;

import android.bluetooth.BluetoothSocket;

import com.google.common.base.Function;
import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

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

    public static ListenableFuture<Void> sendTokenAndPasscode(final BluetoothSocket socket, final String token, final boolean passcodeRequired){
        return Futures.transformAsync(Threading.switchToBackground(),
                new AsyncFunction<Void, Void>() {
                    @Override
                    public ListenableFuture<Void> apply(Void input) throws Exception {
                        JSONObject jsonObject = new JSONObject();
                        jsonObject.put(passCodeRequiredKey, passcodeRequired);
                        jsonObject.put(tokenKey, token);
                        OutputStream out = socket.getOutputStream();
                        out.write(jsonObject.toString().getBytes());
                        return Futures.immediateFuture(null);
                    }
                });
    }


    public static ListenableFuture<String> receivePasscode(final BluetoothSocket socket){
        return Futures.transformAsync(Threading.switchToBackground(), new AsyncFunction<Void, String>() {
            @Override
            public ListenableFuture<String> apply(Void input) throws Exception {
                InputStream in = socket.getInputStream();
                byte[] bytes = new byte[500];
                in.read(bytes);
                JSONObject json = new JSONObject(new String(bytes));
                String passcode = json.getString(passcodeKey);
                return Futures.immediateFuture(passcode);
            }
        });
    }

    public static ListenableFuture<Void> sendConfirmation(final BluetoothSocket socket){
        return Futures.transformAsync(Threading.switchToBackground(),
                new AsyncFunction<Void, Void>() {
                    @Override
                    public ListenableFuture<Void> apply(Void input) throws Exception {
                        JSONObject jsonObject = new JSONObject();
                        jsonObject.put(serverConfirmationKey, true);
                        OutputStream out = socket.getOutputStream();
                        out.write(jsonObject.toString().getBytes());
                        return Futures.immediateFuture(null);
                    }
                });
    }

    public static ListenableFuture<Boolean> receiveConfirmationResponse(final BluetoothSocket socket){
        return Futures.transformAsync(Threading.switchToBackground(), new AsyncFunction<Void, Boolean>() {
            @Override
            public ListenableFuture<Boolean> apply(Void input) throws Exception {
                InputStream in = socket.getInputStream();
                byte[] bytes = new byte[500];
                in.read(bytes);
                JSONObject json = new JSONObject(new String(bytes));
                Boolean confirmation = json.optBoolean(clientConfirmationKey, false);
                return Futures.immediateFuture(confirmation);
            }
        });
    }

    public static ListenableFuture<Void> sendFinalAck(final BluetoothSocket socket, final boolean positive){
        return Futures.transformAsync(Threading.switchToBackground(),
                new AsyncFunction<Void, Void>() {
                    @Override
                    public ListenableFuture<Void> apply(Void input) throws Exception {
                        JSONObject jsonObject = new JSONObject();
                        jsonObject.put(finalAckKey, positive);
                        OutputStream out = socket.getOutputStream();
                        out.write(jsonObject.toString().getBytes());
                        return Futures.immediateFuture(null);
                    }
                });
    }

    public static ListenableFuture<Void> sendAuthenicationRequest(final BluetoothSocket socket, final String message){
        return Futures.transformAsync(Threading.switchToBackground(),
                new AsyncFunction<Void, Void>() {
                    @Override
                    public ListenableFuture<Void> apply(Void input) throws Exception {
                        JSONObject jsonObject = new JSONObject();
                        jsonObject.put(serverSaltForAuthentication, message);
                        OutputStream out = socket.getOutputStream();
                        out.write(jsonObject.toString().getBytes());
                        return Futures.immediateFuture(null);
                    }
                });
    }

    public static ListenableFuture<String> receiveAuthenticationResponse (final BluetoothSocket socket){
        return Futures.transformAsync(Threading.switchToBackground(), new AsyncFunction<Void, String>() {
            @Override
            public ListenableFuture<String> apply(Void input) throws Exception {
                InputStream in = socket.getInputStream();
                byte[] bytes = new byte[500];
                in.read(bytes);
                JSONObject json = new JSONObject(new String(bytes));
                String receivedHashedToken = json.getString(hashedToken);
                return Futures.immediateFuture(receivedHashedToken);
            }
        });
    }
}
