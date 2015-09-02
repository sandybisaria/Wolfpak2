package com.wolfpakapp.wolfpak2;

import com.wolfpakapp.wolfpak2.service.NullManager;
import com.wolfpakapp.wolfpak2.service.ServiceManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * The WolfpakServiceProvider provides a way for different classes in the project to access common
 * services such as the REST client, GPS provider, and user ID manager.
 */
public class WolfpakServiceProvider {

    public static final String USERIDMANAGER = "Wolfpak.UserIdManager";
    public static final String SQLITEMANAGER = "Wolfpak.SQLiteManager";
    public static final String SERVERRESTCLIENT = "Wolfpak.ServerRestClient";
    public static final String LOCATIONPROVIDER = "Wolfpak.LocationProvider";

    private static ServiceManager nullManager = new NullManager();

    private static HashMap<String, ServiceManager> managerHashMap = new HashMap<String, ServiceManager>();
    private static HashMap<String, Boolean> initializedHashMap = new HashMap<String, Boolean>();

    private static ArrayList<OnAllInitializedCallback> mCallbacks = new ArrayList<OnAllInitializedCallback>();
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
        initializedHashMap.put(tag, false);
    }

    /**
     * Set a callback for when all registered ServiceManagera have finished initialization.
     * @param callback The callback to be invoked.
     */
    public static void setOnAllInitializedCallback(OnAllInitializedCallback callback) {
        if (!isAllInitialized) {
            // If not all currently registered ServiceManagers are initialized, add the callback.
            mCallbacks.add(callback);
        } else {
            // Invoke the callback immediately.
            callback.onAllInitialized();
        }
    }

    /**
     * Start waiting for the ServiceManagers to be initialized.
     */
    public static void startWaiting() {
        Iterator iterator = managerHashMap.entrySet().iterator();
        while (iterator.hasNext()) {
            final Map.Entry pair = (Map.Entry) iterator.next();
            ServiceManager manager = (ServiceManager) pair.getValue();
            manager.setOnInitializedCallback(new ServiceManager.OnInitializedCallback() {
                @Override
                public void onInitialized() {
                    initializedHashMap.put((String) pair.getKey(), true);

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
    }

    /**
     * Check if all registered ServiceManagers are initialized.
     * @return True if all registered ServiceManagers are initialized.
     */
    private static boolean isAllInitialized() {
        // Set isAllInitialized to true; innocent unless proven guilty...
        isAllInitialized = true;

        for (Map.Entry pair : initializedHashMap.entrySet()) {
            if (!(Boolean) pair.getValue()) {
                isAllInitialized = false;
                break;
            }
        }

        return isAllInitialized;
    }

    /**
     * The OnAllInitializedCallback is an interface for a callback that wants to be called when all
     * registered ServiceManagers have finished initialization.
     */
    public interface OnAllInitializedCallback {
        void onAllInitialized();
    }
}
