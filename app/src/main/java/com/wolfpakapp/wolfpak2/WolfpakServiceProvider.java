package com.wolfpakapp.wolfpak2;

import com.wolfpakapp.wolfpak2.service.NullManager;
import com.wolfpakapp.wolfpak2.service.ServiceManager;

import java.util.HashMap;

public class WolfpakServiceProvider {

    private static final String NULLMANAGER = "Wolfpak.NullManager";
    public static final String USERIDMANAGER = "Wolfpak.UserIdManager";

    private static HashMap<String, ServiceManager> managerHashMap = new HashMap<>();

    /**
     * Get the requested ServiceManager (or a NullManager if it does not exist).
     * @param tag The tag of the service manager.
     * @return The requested service manager (if null, then a NullManager).
     */
    public static ServiceManager getServiceManager(String tag) {
        try {
            return managerHashMap.get(tag);
        } catch (Exception e) {
            if (!managerHashMap.containsKey(NULLMANAGER)) {
                managerHashMap.put(NULLMANAGER, new NullManager());
            }
            return managerHashMap.get(NULLMANAGER);
        }

    }

    /**
     * Register a new ServiceManager.
     * @param tag The tag of the service manager.
     * @param manager The initialized service manager.
     */
    public static void registerServiceManager(String tag, ServiceManager manager) {
        managerHashMap.put(tag, manager);
    }

}
