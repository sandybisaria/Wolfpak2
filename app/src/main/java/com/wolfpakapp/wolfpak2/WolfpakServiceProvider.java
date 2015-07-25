package com.wolfpakapp.wolfpak2;

import com.wolfpakapp.wolfpak2.service.ServiceManager;

import java.util.HashMap;

public class WolfpakServiceProvider {

    public static final String USERIDMANAGER = "Wolfpak.UserIdManager";

    private static HashMap<String, ServiceManager> managerHashMap = new HashMap<>();

    public static ServiceManager getServiceManager(String tag) {
        try {
            return managerHashMap.get(tag);
        } catch (Exception e) {
            return null;
        }

    }

    public static void registerServiceManager(String tag, ServiceManager manager) {
        managerHashMap.put(tag, manager);
    }

}
