package com.wolfpakapp.wolfpak2.camera.preview;

import android.content.Context;
import android.graphics.Bitmap;
import com.wolfpakapp.wolfpak2.Size;
import android.util.SparseIntArray;
import android.view.Surface;

import java.util.List;

/**
 * An encapsulation of camera functions to allow usage of both Camera and Camera2 APIs
 * @author Roland Fong
 */
public abstract class CameraController implements CameraView.StateCallback {
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
     * Conversions from screen rotation to JPEG Orientation
     */
    protected static final SparseIntArray ORIENTATIONS = new SparseIntArray();
    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }
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
    public abstract void toggleCamera(); // TODO consider changing CameraState vars here
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
    protected abstract List getSupportedPreviewSizes();
    /**
     * Returns sizes supported for video
     * @return supported video sizes
     */
    protected abstract List getSupportedVideoSizes();
    /**
     * Returns sizes supported for saved still images
     * @return supported image sizes
     */
    protected abstract List getSupportedImageSizes();
}
