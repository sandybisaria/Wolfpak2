package com.wolfpakapp.wolfpak2.service;

import java.util.ArrayList;

/**
 * ServiceManager is an abstract class for managers that provide application services.
 */
public abstract class ServiceManager {
    protected boolean isInitialized = false;
    protected ArrayList<OnInitializedCallback> mCallbacks = new ArrayList<>();

    public ServiceManager() {

    }

    public boolean isInitialized() {
        return isInitialized;
    }

    /**
     * Set a callback for when the ServiceManager is finished initialization.
     * @param callback The callback to be invoked.
     */
    public void setOnInitializedCallback(OnInitializedCallback callback) {
        if (!isInitialized) {
            // If the ServiceManager wasn't initialized yet, then add it to the callback list.
            mCallbacks.add(callback);
        } else {
            // Otherwise, call it immediately.
            callback.onInitialized();
        }
    }

    /**
     * Finish the ServiceManager initialization and notify all callbacks. This function should be
     * invoked within the constructor so that the application can be notified if the manager has
     * initialized.
     */
    protected final void finishInitialize() {
        isInitialized = true;
        for (OnInitializedCallback callback : mCallbacks) {
            callback.onInitialized();
        }
        // Clear all the callbacks once we are done with them...
        mCallbacks.clear();
    }

    /**
     * The OnInitializedCallback is an interface for a callback that wants to be called when the
     * ServiceManager has finished initialization.
     */
    public interface OnInitializedCallback {
        void onInitialized();
    }
}
