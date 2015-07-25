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
    private static boolean isAllInitialized = false;

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
                // Call the isAllInitialized method so that all ServiceManagers can be checked!
                if (WolfpakServiceProvider.isAllInitialized()) {
                    for (OnAllInitializedCallback callback : mCallbacks) {
                        callback.onAllInitialized();
                    }
                    // Clear all current callbacks (because there's no need to hold onto them...)
                    mCallbacks.clear();
                }
            }
        });
    }

    public static void setOnAllInitializedCallback(OnAllInitializedCallback callback) {
        if (!isAllInitialized()) {
            mCallbacks.add(callback);
        } else {
            callback.onAllInitialized();
        }
    }

    /**
     * Check if all registered ServiceManagers are initialized.
     * @return True if all registered ServiceManagers are initialized.
     */
    private static boolean isAllInitialized() {
        // Set isAllInitialized to true; innocent unless proven guilty...
        isAllInitialized = true;

        Iterator iterator = managerHashMap.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry pair = (Map.Entry) iterator.next();
            if (!((ServiceManager) pair.getValue()).isInitialized()) {
                isAllInitialized = false;
                break;
            }
        }

        return isAllInitialized;
    }

    public interface OnAllInitializedCallback {
        void onAllInitialized();
    }
}
