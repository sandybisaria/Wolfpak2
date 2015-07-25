package com.wolfpakapp.wolfpak2.service;

import java.util.ArrayList;

/**
 * ServiceManager is an abstract class for managers that provide application services.
 */
public abstract class ServiceManager {
    protected boolean isInitialized = false;
    protected ArrayList<onInitializedCallback> mCallbacks = new ArrayList<>();

    public ServiceManager() {

    }

    public boolean isInitialized() {
        return isInitialized;
    }

    public void setOnInitializedCallback(onInitializedCallback callback) {
        mCallbacks.add(callback);
    }

    protected final void initialize() {
        isInitialized = true;
        for (onInitializedCallback callback : mCallbacks) {
            callback.onInitialized();
        }
    }

    public interface onInitializedCallback {
        void onInitialized();
    }
}
