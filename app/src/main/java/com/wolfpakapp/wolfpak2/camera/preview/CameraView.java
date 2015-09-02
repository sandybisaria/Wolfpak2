package com.wolfpakapp.wolfpak2.camera.preview;

import android.app.Activity;
import android.content.Context;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.util.AttributeSet;
import android.view.Surface;
import android.view.TextureView;

import com.wolfpakapp.wolfpak2.Size;

/**
 * A custom TextureView that handles its own transformation and has its own callback interface
 * Adapted from
 * https://github.com/commonsguy/cwac-cam2/blob/master/cam2/src/main/java/com/commonsware/cwac/cam2/CameraView.java
 *
 * @author Roland Fong
 */
public class CameraView extends TextureView implements TextureView.SurfaceTextureListener {
    interface StateCallback {
        void onReady(CameraView cv, int width, int height);

        void onDestroyed(CameraView cv);
    }

    /**
     * The requested size of the preview frames, or null to just
     * use the size of the view itself
     */
    private Size previewSize;

    private StateCallback stateCallback;

    /**
     * Constructor, used for creating instances from Java code.
     *
     * @param context the Activity that will host this View
     */
    public CameraView(Context context) {
        super(context, null);
        setSurfaceTextureListener(this);
    }

    /**
     * Constructor, used by layout inflation.
     *
     * @param context the Activity that will host this View
     * @param attrs   the parsed attributes from the layout resource tag
     */
    public CameraView(Context context, AttributeSet attrs) {
        super(context, attrs, 0);
        setSurfaceTextureListener(this);
    }

    /**
     * @param context  the Activity that will host this View
     * @param attrs    the parsed attributes from the layout resource tag
     * @param defStyle "An attribute in the current theme that
     *                 contains a reference to a style resource
     *                 that supplies default values for the view.
     *                 Can be 0 to not look for defaults."
     */
    public CameraView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setSurfaceTextureListener(this);
    }

    /**
     * @return the requested preview size
     */
    public Size getPreviewSize() {
        return previewSize;
    }

    /**
     * @param previewSize the requested preview size
     */
    public void setPreviewSize(Size previewSize) {
        this.previewSize = previewSize;

        enterTheMatrix();
    }

    public void setStateCallback(StateCallback cb) {
        stateCallback = cb;
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int width, int height) {
        if (stateCallback != null) {
            stateCallback.onReady(this, width, height);
        }
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int width, int height) {
        enterTheMatrix();
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
        if (stateCallback != null) {
            stateCallback.onDestroyed(this);
        }

        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {
    }

    private void enterTheMatrix() {
        if (previewSize != null) {
            adjustAspectRatio(previewSize.getWidth(),
                    previewSize.getHeight(),
                    ((Activity) getContext()).getWindowManager().getDefaultDisplay().getRotation());
        }
    }

    // inspired by https://github.com/google/grafika/blob/master/src/com/android/grafika/PlayMovieActivity.java

    private void adjustAspectRatio(int videoWidth, int videoHeight,
                                   int rotation) {
        // Log.e("CameraView", String.format("video=%d x %d", videoWidth, videoHeight));

        int temp = videoWidth;
        videoWidth = videoHeight;
        videoHeight = temp;
        // Log.e("CameraView", String.format("video after flip=%d x %d", videoWidth, videoHeight));

        int viewWidth = getWidth();
        int viewHeight = getHeight();
        double aspectRatio = (double) videoHeight / (double) videoWidth;
        int newWidth, newHeight;

        if (getHeight() > (int) (viewWidth * aspectRatio)) {
            newWidth = (int) (viewHeight / aspectRatio);
            newHeight = viewHeight;
        } else {
            newWidth = viewWidth;
            newHeight = (int) (viewWidth * aspectRatio);
        }

        // Log.e("CameraView", String.format("view=%d x %d", viewWidth, viewHeight));
        // Log.e("CameraView", String.format("new=%d x %d", newWidth, newHeight));

        int xoff = (viewWidth - newWidth) / 2;
        int yoff = (viewHeight - newHeight) / 2;

        Matrix txform = new Matrix();

        getTransform(txform);

        float xscale = (float) newWidth / (float) viewWidth;
        float yscale = (float) newHeight / (float) viewHeight;

        // Log.e("CameraView", String.format("scale=%f x %f", xscale, yscale));
        txform.setScale(xscale, yscale);

        switch (rotation) {
            case Surface.ROTATION_90:
                txform.postRotate(270, newWidth / 2, newHeight / 2);
                break;

            case Surface.ROTATION_270:
                txform.postRotate(90, newWidth / 2, newHeight / 2);
                break;
        }

        txform.postTranslate(xoff, yoff);
        // android.util.Log.e("CameraView", String.format("translate=%d x %d", xoff, yoff));

        setTransform(txform);
    }
}
