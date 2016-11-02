package com.jspiker.phoneauthnticator;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Created by jspiker on 11/1/16.
 */

public class StorageAccess {

    private static final String tokenPref = "TOKEN_PREF";

    public static boolean containsToken(Context context){
        return context.getSharedPreferences(tokenPref, Context.MODE_PRIVATE).contains(tokenPref);
    }

    public static synchronized String getToken(Context context){
        return context.getSharedPreferences(tokenPref, Context.MODE_PRIVATE).getString(tokenPref, null);
    }

    public static synchronized void setToken(Context context, String token){
        SharedPreferences.Editor editor =  context.getSharedPreferences(tokenPref, Context.MODE_PRIVATE).edit();
        editor.putString(tokenPref, token);
        editor.commit();
    }

    public static synchronized void clearToken(Context context){
        SharedPreferences.Editor editor =  context.getSharedPreferences(tokenPref, Context.MODE_PRIVATE).edit();
        editor.putString(tokenPref, null);
        editor.commit();
    }
}
