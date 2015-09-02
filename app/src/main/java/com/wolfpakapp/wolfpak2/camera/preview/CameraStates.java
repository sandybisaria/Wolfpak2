package com.wolfpakapp.wolfpak2.camera.preview;

import com.wolfpakapp.wolfpak2.Size;

/**
 * Holds the global states of the camera for access across all camera classes
 * @author Roland Fong
 */
public class CameraStates {

    public static final int GLOBAL_STATE_PREVIEW = 0;
    public static final int GLOBAL_STATE_EDITOR = 1;

    public static final int AUTO_FLASH = 0;
    public static final int NO_FLASH = 1;
    public static final int ALWAYS_FLASH = 2;

    public static final int BACK = 0;
    public static final int FRONT = 1;

    public static final int FILE_IMAGE = 0;
    public static final int FILE_VIDEO = 1;

    public static int CAMERA_GLOBAL_STATE;

    public static int FLASH_STATE; // what state of flash to use
    public static int CAMERA_FACE; // which direction camera is facing
    public static boolean IS_SOUND; // true if sound is on

    public static int FILE_TYPE; // whether image or video is taken

    public static Size SCREEN_SIZE;

    public static boolean isFrontCamera()   {
        return (CAMERA_FACE == FRONT);
    }
}
