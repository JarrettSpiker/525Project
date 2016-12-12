package com.jspiker.accesscontrolsystem.model;

/**
 * Created by jspiker on 12/12/16.
 */

import java.util.Set;

/**
 * This is mostly a stub in our framework implementation, For any real implementation it should
 * be extended to actually manage access to resources
 */
public class ResourceManager {

    private static Set<String> allowedDevices;
    private static boolean allowed = false;

    public static void allowAccessToDevices(Set<String> devices){
        allowedDevices = devices;
        allowed= true;
    }
    public static void revokeAccess(){
        allowedDevices = null;
        allowed  = false;
    }

    //This implements the principle of failsafe defaults
    public static boolean shouldAllowAccessToDevice(String deviceAddress){
        if(!allowed){
            return false;
        }
        for(String allowedDevice : allowedDevices){
            if(deviceAddress.equals(allowedDevice)){
                return  true;
            }
        }
        return false;
    }


}
