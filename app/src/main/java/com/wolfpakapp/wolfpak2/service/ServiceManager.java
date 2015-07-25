package com.wolfpakapp.wolfpak2.service;

/**
 * ServiceManager is an abstract class for managers that provide application services.
 */
public abstract class ServiceManager {
    boolean isInitialized = false;

    public ServiceManager() {

    }

    public boolean isInitialized() {
        return isInitialized;
    }
}
