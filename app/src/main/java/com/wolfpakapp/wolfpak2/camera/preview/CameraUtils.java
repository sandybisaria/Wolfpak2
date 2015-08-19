package com.wolfpakapp.wolfpak2.camera.preview;

import android.content.Context;
import android.content.res.Configuration;
import android.hardware.Camera;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.WindowManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Camera utility methods
 * @author Roland Fong
 */
public class CameraUtils {

    private static final String TAG = "TAG-CameraUtils";

    /**
     * Returns the device rotation; Surface.ROTATION_0, ROTATION_90, etc.
     * @param context
     * @return
     */
    public static int getRotation(Context context) {
        WindowManager windowManager =  (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        return windowManager.getDefaultDisplay().getRotation();
    }



    /**
     * Sets the camera's display orientation based on device orientation
     * @param context
     * @param cameraId
     * @param camera
     */
    @SuppressWarnings("deprecation")
    public static void setCameraDisplayOrientation(Context context,
                                                   int cameraId, android.hardware.Camera camera) {
        android.hardware.Camera.CameraInfo info =
                new android.hardware.Camera.CameraInfo();
        android.hardware.Camera.getCameraInfo(cameraId, info);
        int rotation = getRotation(context);
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0: degrees = 0; break;
            case Surface.ROTATION_90: degrees = 90; break;
            case Surface.ROTATION_180: degrees = 180; break;
            case Surface.ROTATION_270: degrees = 270; break;
        }

        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;  // compensate the mirror
        } else {  // back-facing
            result = (info.orientation - degrees + 360) % 360;
        }
        camera.setDisplayOrientation(result);
    }

    /**
     * Returns the largest size given a list of choices
     * @param choices
     * @return
     */
    public static Size getLargestSize(List<Size> choices)   {
        return Collections.max(choices, new CameraUtils.CompareSizesByArea());
    }

    /**
     * Given sizes supported by camera, chooses smallest one whose width and height are at least as
     * large as respective requested values and whose aspect ratio matches specified value
     * @param choices   list of choices supported by camera
     * @param width     minimum width
     * @param height    minimum height
     * @param aspectRatio
     * @return  Optimal size or otherwise arbitrary
     */
    public static Size chooseOptimalSize(List<Size> choices, int width, int height, Size aspectRatio)  {
        // Collect supported resolutions at least as big as preview surface
        List<Size> bigEnough = new ArrayList<Size>();
        int w = aspectRatio.getWidth();
        int h = aspectRatio.getHeight();
        for(Size option : choices)  {
            if (option.getHeight() == option.getWidth() * h / w &&
                    option.getWidth() >= width && option.getHeight() >= height) {
                bigEnough.add(option);
            }
        }

        // Pick the smallest of those, assuming we found any
        if (bigEnough.size() > 0) {
            return Collections.min(bigEnough, new CompareSizesByArea());
        } else {
            Log.e(TAG, "Couldn't find any suitable preview size");
            return Collections.max(choices, new CompareSizesByArea());
        }
    }

    /**
     * Compares two {@code Size}s based on their areas.
     */
    public static class CompareSizesByArea implements Comparator<Size> {
        @Override
        public int compare(Size lhs, Size rhs) {
            // We cast here to ensure the multiplications won't overflow
            return Long.signum((long) lhs.getWidth() * lhs.getHeight() -
                    (long) rhs.getWidth() * rhs.getHeight());
        }
    }
}