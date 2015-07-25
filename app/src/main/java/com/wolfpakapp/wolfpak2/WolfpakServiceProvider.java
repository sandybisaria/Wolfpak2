package com.wolfpakapp.wolfpak2;

import com.wolfpakapp.wolfpak2.service.NullManager;
import com.wolfpakapp.wolfpak2.service.ServiceManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class WolfpakServiceProvider {

    public static final String USERIDMANAGER = "Wolfpak.UserIdManager";
    private static ServiceManager nullManager = new NullManager();

    private static HashMap<String, ServiceManager> managerHashMap = new HashMap<>();

    private static ArrayList<OnAllInitializedCallback> mCallbacks = new ArrayList<>();

    /**
     * Get the requested ServiceManager (or a NullManager if it does not exist).
     * @param tag The tag of the service manager.
     * @return The requested service manager (if null, then a NullManager).
     */
    public static ServiceManager getServiceManager(String tag) {
        ServiceManager serviceManager = managerHashMap.get(tag);
        if (serviceManager != null) {
            return serviceManager;
        } else {
            return nullManager;
        }

    }

    /**
     * Register a new ServiceManager.
     * @param tag The tag of the service manager.
     * @param manager The initialized service manager.
     */
    public static void registerServiceManager(String tag, ServiceManager manager) {
        managerHashMap.put(tag, manager);
        manager.setOnInitializedCallback(new ServiceManager.OnInitializedCallback() {
            @Override
            public void onInitialized() {
                if (WolfpakServiceProvider.isAllInitialized()) {
                    for (OnAllInitializedCallback callback : mCallbacks) {
                        callback.onAllInitialized();
                    }
                }
            }
        });
    }

    public static void setOnAllInitializedCallback(OnAllInitializedCallback callback) {
        mCallbacks.add(callback);
    }

    private static boolean isAllInitialized() {
        Iterator iterator = managerHashMap.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry pair = (Map.Entry) iterator.next();
            if (!((ServiceManager) pair.getValue()).isInitialized()) {
                return false;
            }
        }

        return true;
    }

    public interface OnAllInitializedCallback {
        void onAllInitialized();
    }
}
