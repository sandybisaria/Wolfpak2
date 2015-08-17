package com.wolfpakapp.wolfpak2.camera.preview;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Size;

import java.util.List;

/**
 * An encapsulation of camera functions to allow usage of both Camera and Camera2 APIs
 * @author Roland Fong
 */
public abstract class CameraController {
    /**
     * Callback for all camera actions
     */
    interface CameraActionCallback	{
        void onCameraOpened();
        void onCameraClosed();
        void onCameraError();
        void onCaptureCompleted();
        void onImageAvailable(Bitmap img);
        void onRecordingCompleted(String path);
    }
    /**
     * The {@link com.wolfpakapp.wolfpak2.camera.preview.CameraController.CameraActionCallback} object
     */
    protected CameraActionCallback mCameraActionCallback;
    /**
     * The application's context
     */
    protected Context mContext;
    /**
     * The {@link CameraView} that preview is to be rendered on
     */
    protected CameraView mCameraView;
    /**
     * The screen size, which is determined by the camera view's initial size
     */
    protected Size mScreenSize;
    /**
     * Public constructor
     * @param context the application context
     */
    public CameraController(Context context)    {
        mContext = context;
    }
    /**
     * @param cameraActionCallback the {@link com.wolfpakapp.wolfpak2.camera.preview.CameraController.CameraActionCallback}
     */
    public void setCameraActionCallback(CameraActionCallback cameraActionCallback)  {
        mCameraActionCallback = cameraActionCallback;
    }
    /**
     * Sets the camera based on global {@link CameraStates} variable CAMERA_FACE and attempts
     * to open it
     */
    public abstract void openCamera();
    /**
     * Closes the camera and releases camera resources
     */
    public abstract void closeCamera();
    /**
     * Starts the camera preview
     */
    public abstract void startPreview();
    /**
     * Starts video recording
     */
    public abstract void startRecording();
    /**
     * Stops video recording
     */
    public abstract void stopRecording();
    /**
     * Initiates still image capture
     */
    public abstract void takePicture();
    /**
     * Switches the camera, reflecting {@link CameraStates} CAMERA_FACE variable and opens new camera
     */
    public abstract void toggleCamera();
    /**
     * Toggles through flash states as specified in {@link CameraStates}
     */
    public abstract void toggleFlash();
    /**
     * Toggles whether sound is to be recorded with video as specified in {@link CameraStates}
     */
    public abstract void toggleSound();
    /**
     * Returns sizes supported for the camera preview (the screen)
     * @return supported preview sizes
     */
    protected abstract List<Size> getSupportedPreviewSizes();
    /**
     * Returns sizes supported for video
     * @return supported video sizes
     */
    protected abstract List<Size> getSupportedVideoSizes();
    /**
     * Returns sizes supported for saved still images
     * @return supported image sizes
     */
    protected abstract List<Size> getSupportedImageSizes();
}
