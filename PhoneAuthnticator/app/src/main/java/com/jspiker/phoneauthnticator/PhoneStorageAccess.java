package com.jspiker.phoneauthnticator;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Created by jspiker on 11/1/16.
 */

public class PhoneStorageAccess {

    private static final String PREFERENCE_KEY = "AUTH_PREFS";

    private static final String tokenPref = "TOKEN_PREF";
    private static final String macAddressPref = "SERVER_MAC_ADDRESS";


    public static boolean containsToken(Context context){
        return context.getSharedPreferences(PREFERENCE_KEY, Context.MODE_PRIVATE).contains(tokenPref);
    }

    public static synchronized String getToken(Context context){
        return context.getSharedPreferences(PREFERENCE_KEY, Context.MODE_PRIVATE).getString(tokenPref, null);
    }

    public static synchronized void setToken(Context context, String token){
        SharedPreferences.Editor editor =  context.getSharedPreferences(PREFERENCE_KEY, Context.MODE_PRIVATE).edit();
        editor.putString(tokenPref, token);
        editor.commit();
    }

    public static synchronized void clearToken(Context context){
        SharedPreferences.Editor editor =  context.getSharedPreferences(PREFERENCE_KEY, Context.MODE_PRIVATE).edit();
        editor.putString(tokenPref, null);
        editor.commit();
    }

    public static synchronized String getServerMacAddress(Context context){
        return context.getSharedPreferences(PREFERENCE_KEY, Context.MODE_PRIVATE).getString(macAddressPref, null);
    }

    public static synchronized void setServerMacAddress(Context context, String macAddress){
        SharedPreferences.Editor editor =  context.getSharedPreferences(PREFERENCE_KEY, Context.MODE_PRIVATE).edit();
        editor.putString(macAddress, macAddress);
        editor.commit();
    }

    public static synchronized void clearServerMacAddress(Context context){
        SharedPreferences.Editor editor =  context.getSharedPreferences(PREFERENCE_KEY, Context.MODE_PRIVATE).edit();
        editor.putString(macAddressPref, null);
        editor.commit();
    }

}
