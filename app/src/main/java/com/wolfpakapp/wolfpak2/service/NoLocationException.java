package com.wolfpakapp.wolfpak2.service;

/**
 * The NoLocationException is thrown whenever a null location is supposed to be accessed.
 */
public class NoLocationException extends Exception {

    public NoLocationException(String detailMessage) {
        super(detailMessage);
    }
}
