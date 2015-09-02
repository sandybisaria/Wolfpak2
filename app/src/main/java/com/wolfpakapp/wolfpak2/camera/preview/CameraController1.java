package com.wolfpakapp.wolfpak2.camera.preview;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.util.Log;

import com.wolfpakapp.wolfpak2.Size;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Controls camera actions using the deprecated Camera API
 *
 * @author Roland Fong
 */
@SuppressWarnings("deprecation")
public class CameraController1 extends CameraController {

    private static final String TAG = "TAG-CameraController1";

    private Camera mCamera;
    private int mCameraId;

    private String mVideoPath;
    private Size mVideoSize;
    private Size mImageSize;

    private MediaRecorder mMediaRecorder;

    public CameraController1(Context context) {
        super(context);
        mVideoPath = (new File(mContext.getExternalFilesDir(null), "video.mp4")).getAbsolutePath();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void openCamera() {
        Camera.CameraInfo info = new Camera.CameraInfo();
        if (mCamera != null) // stop the camera if it had already been inited
            stopPreviewAndFreeCamera();

        // find the correct facing camera
        for (int id = 0; id <= Camera.getNumberOfCameras(); id++) {
            Camera.getCameraInfo(id, info);
            int lensFacing = (CameraStates.CAMERA_FACE == CameraStates.FRONT) ?
                    Camera.CameraInfo.CAMERA_FACING_FRONT : Camera.CameraInfo.CAMERA_FACING_BACK;
            if (info.facing == lensFacing) {
                try {
                    mCameraId = id;
                    mCamera = Camera.open(id); // open the camera
                } catch (RuntimeException e) {
                    Log.e(TAG, "Could not open camera with id: " + id);
                    e.printStackTrace();
                }
                setupCamera();
                return;
            }
        }
        // if no camera matching correct direction is found, open default
        mCamera = Camera.open();
        mCameraId = 0; // most likely...
        setupCamera();
    }

    /**
     * Sets up the camera related outputs etc. and begins preview
     */
    private void setupCamera() {
        CameraUtils.setCameraDisplayOrientation(mContext, mCameraId, mCamera);
        // set the error callback
        mCamera.setErrorCallback(new Camera.ErrorCallback() {
            @Override
            public void onError(int error, Camera camera) {
                Log.e(TAG, "Camera experienced error w/ code: " + error);
                mCameraActionCallback.onCameraError();
            }
        });
        // get video size
        Size largest = CameraUtils.getBestSize(cameraSizeToUtilSize(getSupportedImageSizes()));
        if (getSupportedVideoSizes() != null) {
            mVideoSize = CameraUtils.chooseOptimalSize(cameraSizeToUtilSize(getSupportedVideoSizes()),
                    mScreenSize.getWidth(), mScreenSize.getHeight(), largest);
        } else {
            mVideoSize = CameraUtils.chooseOptimalSize(cameraSizeToUtilSize(getSupportedPreviewSizes()),
                    mScreenSize.getWidth(), mScreenSize.getHeight(), largest);
        }
        mImageSize = largest;
        mCameraActionCallback.onCameraOpened();
        startPreview();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void closeCamera() {
        releaseMediaRecorder();
        stopPreviewAndFreeCamera();
        mCameraActionCallback.onCameraClosed();
    }

    /**
     * Kills the preview and releases camera resources.  {@link #mCamera} will return null
     */
    private void stopPreviewAndFreeCamera() {
        if (mCamera != null) {
            try {
                // stop updating the preview surface.
                mCamera.stopPreview();

                // Important: Call release() to release the camera for use by other
                // applications. Applications should release the camera immediately
                // during onPause() and re-open() it during onResume()).
                mCamera.release();
            } catch (RuntimeException e) {
                Log.e(TAG, "Couldn't release camera");
                e.printStackTrace();
            } finally {
                mCamera = null;
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void startPreview() {
        try {
            mCamera.setPreviewTexture(mCameraView.getSurfaceTexture());
            mCamera.startPreview();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Prepares the media recorder
     *
     * @return whether prepare was successful
     */
    private boolean setUpMediaRecorder() {
        if (mCamera == null) return false; // fail!
        CameraStates.FILE_TYPE = CameraStates.FILE_VIDEO;
        // unlock the camera so mediarecorder can use it
        mCamera.unlock();

        mMediaRecorder = new MediaRecorder();
        mMediaRecorder.setCamera(mCamera);
        // set audio/video sources

        if (CameraStates.IS_SOUND)
            mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);

        // supposedly more reliable? CamcorderProfile, used in conjunction with setCamera()
        CamcorderProfile profile = CamcorderProfile.get(mCameraId, CamcorderProfile.QUALITY_HIGH);
//        Log.d(TAG, "Camcorder profile defaults:**************************************");
//        Log.d(TAG, "Camcorder profile file format: " + profile.fileFormat);
//        Log.d(TAG, "Camcorder profile video height: " + profile.videoFrameHeight);
//        Log.d(TAG, "Required video height: " + mVideoSize.getHeight());
//        Log.d(TAG, "Camcorder profile video width: " + profile.videoFrameWidth);
//        Log.d(TAG, "Required video width: " + mVideoSize.getWidth());
//        Log.d(TAG, "Camcorder profile quality: " + profile.quality);
//        Log.d(TAG, "Camcorder profile vid codec:" + profile.videoCodec);
//        Log.d(TAG, "Camcorder profile audio codec: " + profile.audioCodec);
//        Log.d(TAG, "Camcorder profile video bitrate: " + profile.videoBitRate);
//        Log.d(TAG, "Camcorder profile video framerate: " + profile.videoFrameRate);

        if (CameraStates.IS_SOUND) { // with audio
            mMediaRecorder.setProfile(profile); // use profile directly
        } else { // without audio
            mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            mMediaRecorder.setVideoFrameRate(profile.videoFrameRate);
            mMediaRecorder.setVideoSize(profile.videoFrameWidth, profile.videoFrameHeight);
            mMediaRecorder.setVideoEncodingBitRate(profile.videoBitRate);
            mMediaRecorder.setVideoEncoder(profile.videoCodec);

            mMediaRecorder.setAudioEncodingBitRate(profile.audioBitRate);
            mMediaRecorder.setAudioChannels(profile.audioChannels);
            mMediaRecorder.setAudioSamplingRate(profile.audioSampleRate);
            //mMediaRecorder.setAudioEncoder(profile.audioCodec);
        }

        mMediaRecorder.setOutputFile(mVideoPath);

        int rotation = CameraUtils.getRotation(mContext);
        int orientation = ORIENTATIONS.get(rotation);
        if (CameraStates.isFrontCamera()) {
            orientation += 180; // hopefully this will cause video to save right side up
        }
        mMediaRecorder.setOrientationHint(orientation);

        try {
            mMediaRecorder.prepare();
        } catch (IOException e) {
            Log.e(TAG, "Mediarecorder IOexception while preparing");
            e.printStackTrace();
            return false;
        } catch (IllegalStateException e1) {
            Log.e(TAG, "Mediarecorder illegal state exception while preparing");
            e1.printStackTrace();
            return false;
        }
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void startRecording() {
        //mCamera.stopPreview();
        if (setUpMediaRecorder()) {
            try {
                mMediaRecorder.start();
            } catch (RuntimeException e) {
                Log.d(TAG, "Mediarecorder start failed");
                e.printStackTrace();
                releaseMediaRecorder();
                mCamera.startPreview();
            }
        } else { // exceptions occurred
            releaseMediaRecorder();
            mCamera.startPreview();
        }
    }

    /**
     * Release media recorder resources
     */
    private void releaseMediaRecorder() {
        if (mMediaRecorder != null) {
            mMediaRecorder.reset();
            mMediaRecorder.release();
            mMediaRecorder = null;
            mCamera.lock();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void stopRecording() {
        try {
            if (mMediaRecorder != null) {
                Log.d(TAG, "Media recorder stop");
                mMediaRecorder.stop();// Stop recording
                Log.d(TAG, "media Recorder reset");
                mMediaRecorder.reset();
                releaseMediaRecorder();
            }
        } catch (IllegalStateException e) {
            Log.d(TAG, "Media recorder could not be stopped correctly");
            e.printStackTrace();
            startPreview(); // restart the preview
        }
        mCameraActionCallback.onRecordingCompleted(mVideoPath);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void takePicture() {
        CameraStates.FILE_TYPE = CameraStates.FILE_IMAGE;
        // called when shutter closes (closest time to when image is taken)
        Camera.ShutterCallback shutterCallback = new Camera.ShutterCallback() {
            @Override
            public void onShutter() {
                mCameraActionCallback.onCaptureCompleted();
            }
        };
        // called when picture is available
        Camera.PictureCallback pictureCallback = new Camera.PictureCallback() {
            @Override
            public void onPictureTaken(byte[] data, Camera camera) {
                Bitmap img = BitmapFactory.decodeByteArray(data, 0, data.length);
                mCameraActionCallback.onImageAvailable(img);
            }
        };
        Camera.Parameters params = mCamera.getParameters();
        switch (CameraStates.FLASH_STATE) {
            case CameraStates.AUTO_FLASH:
                params.setFlashMode(Camera.Parameters.FLASH_MODE_AUTO);
                break;
            case CameraStates.NO_FLASH:
                params.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                break;
            case CameraStates.ALWAYS_FLASH:
                params.setFlashMode(Camera.Parameters.FLASH_MODE_ON);
                break;
        }
        params.setPictureSize(mImageSize.getWidth(), mImageSize.getHeight());
        mCamera.setParameters(params);
        mCamera.takePicture(shutterCallback, null, pictureCallback);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void toggleCamera() {
        CameraStates.CAMERA_FACE = CameraStates.isFrontCamera() ? CameraStates.BACK : CameraStates.FRONT;
        Log.d(TAG, "Toggling camera, reopening");
        closeCamera();
        openCamera(); // need to select different camera, then when it finishes opening, preview starts
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void toggleFlash() {
        switch (CameraStates.FLASH_STATE) {
            case CameraStates.AUTO_FLASH:
                CameraStates.FLASH_STATE = CameraStates.NO_FLASH;
                break;
            case CameraStates.NO_FLASH:
                CameraStates.FLASH_STATE = CameraStates.ALWAYS_FLASH;
                break;
            case CameraStates.ALWAYS_FLASH:
                CameraStates.FLASH_STATE = CameraStates.AUTO_FLASH;
                break;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void toggleSound() {
        CameraStates.IS_SOUND = !CameraStates.IS_SOUND;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected List getSupportedPreviewSizes() {
        return mCamera.getParameters().getSupportedPreviewSizes();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected List getSupportedVideoSizes() {
        return mCamera.getParameters().getSupportedVideoSizes();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected List getSupportedImageSizes() {
        return mCamera.getParameters().getSupportedPictureSizes();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onReady(CameraView cv, int width, int height) {
        Log.d(TAG, "CameraView ready");
        mCameraView = cv;
        CameraStates.SCREEN_SIZE = new Size(width, height);
        mScreenSize = CameraStates.SCREEN_SIZE;
        openCamera();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onDestroyed(CameraView cv) {

    }

    /**
     * Converts horrid {@link android.hardware.Camera.Size} to the usual {@link Size} objects
     *
     * @param camSizes the list of {@link android.hardware.Camera.Size} objects
     * @return the list of {@link Size} objects
     */
    private List<Size> cameraSizeToUtilSize(List<Camera.Size> camSizes) {
        List<Size> utilSizes = new ArrayList<Size>();
        for (Camera.Size size : camSizes) {
            utilSizes.add(new Size(size.width, size.height));
        }
        return utilSizes;
    }
}
