package com.jspiker.accesscontrolsystem;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Created by jspiker on 11/8/16.
 */

public class AccessControlStorage {


    private static final String PREFERENCE_KEY = "AUTH_PREFS";
    private static final String TOKEN_KEY_PREFIX = "TOKEN_PREF_";
    private static final String PASSCODE_KEY_PREFIX = "PASSCODE_PREF_";

    public static void setTokenForDevice(Context context, String macAddress, String token){
        SharedPreferences.Editor editor =  context.getSharedPreferences(PREFERENCE_KEY, Context.MODE_PRIVATE).edit();
        editor.putString(TOKEN_KEY_PREFIX+macAddress, token);
        editor.commit();
    }

    public static String getTokenForDevice(Context context, String macAddress){
        return context.getSharedPreferences(PREFERENCE_KEY, Context.MODE_PRIVATE).getString(TOKEN_KEY_PREFIX+macAddress, null);
    }

    public static void clearTokenForDevice(Context context, String macAddress){
        SharedPreferences.Editor editor =  context.getSharedPreferences(PREFERENCE_KEY, Context.MODE_PRIVATE).edit();
        editor.putString(TOKEN_KEY_PREFIX+macAddress, null);
        editor.commit();
    }

    public static void setPasscodeForDevice(Context context, String macAddress, String passcode){
        SharedPreferences.Editor editor =  context.getSharedPreferences(PREFERENCE_KEY, Context.MODE_PRIVATE).edit();
        editor.putString(PASSCODE_KEY_PREFIX+macAddress, passcode);
        editor.commit();
    }

    public static String getPasscodeForDevice(Context context, String macAddress){
        return context.getSharedPreferences(PREFERENCE_KEY, Context.MODE_PRIVATE).getString(PASSCODE_KEY_PREFIX+macAddress, null);
    }

    public static void clearPasscodeForDevice(Context context, String macAddress){
        SharedPreferences.Editor editor =  context.getSharedPreferences(PREFERENCE_KEY, Context.MODE_PRIVATE).edit();
        editor.putString(PASSCODE_KEY_PREFIX+macAddress, null);
        editor.commit();
    }


}
