package com.wolfpakapp.wolfpak2.camera.preview;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Surface;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * Controls camera actions using the Camera2 API
 * @author Roland Fong
 */
public class CameraController2 extends CameraController implements CameraView.StateCallback {

    private static final String TAG = "TAG-CameraController2";

    /**
     * An enumeration of various states of capturing still images
     */
    public enum CaptureState    {
        STATE_PREVIEW,
        STATE_WAITING_LOCK,
        STATE_WAITING_PRECAPTURE,
        STATE_WAITING_NON_PRECAPTURE,
        STATE_PICTURE_TAKEN
    }

    private CaptureState mState = CaptureState.STATE_PREVIEW;

    /**
     * Conversions from screen rotation to JPEG Orientation
     */
    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();
    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    private String mVideoPath;

    private String mCameraId;
    private CameraManager mCameraManager;
    private CameraCaptureSession mCaptureSession;
    private CameraDevice mCameraDevice;
    private Size mPreviewSize;
    private Size mVideoSize;
    private MediaRecorder mMediaRecorder;

    /**
     * Prevents app from exiting before closing camera
     */
    private Semaphore mCameraOpenCloseLock = new Semaphore(1);

    private final CameraDevice.StateCallback mStateCallback = new CameraDevice.StateCallback()  {
        @Override
        public void onOpened(CameraDevice cameraDevice) {
            Log.d(TAG, "On opened, will start preview");
            // starts camera preview
            mCameraOpenCloseLock.release();
            mCameraDevice = cameraDevice;
            mCameraActionCallback.onCameraOpened();
            startPreview();
        }

        @Override
        public void onDisconnected(CameraDevice cameraDevice) {
            Log.d(TAG, "Camera Disconnected");
            mCameraOpenCloseLock.release();
            cameraDevice.close();
            mCameraDevice = null;
        }

        @Override
        public void onError(CameraDevice cameraDevice, int error) {
            Log.e(TAG, "An Error Occurred: " + error);
            if(error != CameraDevice.StateCallback.ERROR_CAMERA_IN_USE) {
                mCameraOpenCloseLock.release();
                cameraDevice.close();
                mCameraDevice = null;
                mCameraActionCallback.onCameraError();
            }
        }

        @Override
        public void onClosed(CameraDevice camera) {
            super.onClosed(camera);
            mCameraActionCallback.onCameraClosed();
        }
    };

    /*THREAD & IMAGE HANDLING*/
    private HandlerThread mBackgroundThread;
    private Handler mBackgroundHandler;
    private ImageReader mImageReader;

    /*CAMERA CAPTURE HANDLING*/
    private CaptureRequest.Builder mPreviewRequestBuilder;
    private CaptureRequest mPreviewRequest;

    /**
     * The callback for handling still image capture
     */
    private CameraCaptureSession.CaptureCallback mCaptureCallback
            = new CameraCaptureSession.CaptureCallback()    {
        private void process(CaptureResult result)  {
            // process result
            switch(mState)  {
                case STATE_PREVIEW: break;
                case STATE_WAITING_LOCK:
                    Log.d(TAG, "Waiting lock");
                    int afState = result.get(CaptureResult.CONTROL_AF_STATE);
                    // front camera is always autofocused, so ignore focus if front camera
                    if (CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED == afState ||
                            CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED == afState ||
                            CameraStates.isFrontCamera()) {
                        Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
                        if (aeState == null ||
                                aeState == CaptureResult.CONTROL_AE_STATE_CONVERGED) {
                            mState = CaptureState.STATE_WAITING_NON_PRECAPTURE;
                            //mState = STATE_PICTURE_TAKEN;
                            captureStillPicture();
                        } else {
                            runPrecaptureSequence();
                        }
                    }
                    break;
                case STATE_WAITING_PRECAPTURE:
                    Log.d(TAG, "Waiting Precapture");
                    Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
                    if (aeState == null ||
                            aeState == CaptureResult.CONTROL_AE_STATE_PRECAPTURE ||
                            aeState == CaptureRequest.CONTROL_AE_STATE_FLASH_REQUIRED) {
                        mState = CaptureState.STATE_WAITING_NON_PRECAPTURE;
                    }
                    break;
                case STATE_WAITING_NON_PRECAPTURE:
                    Log.d(TAG, "Waiting non precapture");
                    Integer aeState1 = result.get(CaptureResult.CONTROL_AE_STATE);
                    if (aeState1 == null || aeState1 != CaptureResult.CONTROL_AE_STATE_PRECAPTURE) {
                        mState = CaptureState.STATE_PICTURE_TAKEN;
                        captureStillPicture();
                    }
                    break;
            }
        }

        @Override
        public void onCaptureProgressed(CameraCaptureSession session,
                                        CaptureRequest request, CaptureResult partialResult) {
            super.onCaptureProgressed(session, request, partialResult);
            process(partialResult);
        }

        @Override
        public void onCaptureCompleted(CameraCaptureSession session,
                                       CaptureRequest request, TotalCaptureResult result) {
            super.onCaptureCompleted(session, request, result);
            process(result);
        }
    };

    /**
     * The listener for when the camera image is available
     */
    private final ImageReader.OnImageAvailableListener mOnImageAvailableListener
            = new ImageReader.OnImageAvailableListener() {
        @Override
        public void onImageAvailable(ImageReader reader) {
            Log.d(TAG, "Image available!");
            mCameraActionCallback.onImageAvailable(imageToBitmap(reader.acquireNextImage()));
        }
    };

    /**
     * Camera Controller constructor
     * @param context the application context
     */
    public CameraController2(Context context)    {
        super(context);
        Log.d(TAG, "Initing Camera2Controller");
        mCameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
        mVideoPath = (new File(mContext.getExternalFilesDir(null), "video.mp4")).getAbsolutePath();
    }

    /**
     * Sets the id of the camera that will be used on the basis of
     * {@link CameraStates} CAMERA_FACE value
     */
    private void setCamera()    {
        Log.d(TAG, "Setting camera");
        int lensFacing = (CameraStates.CAMERA_FACE == CameraStates.FRONT) ?
                CameraCharacteristics.LENS_FACING_FRONT : CameraCharacteristics.LENS_FACING_BACK;

        if(lensFacing == CameraCharacteristics.LENS_FACING_BACK)    {
            Log.d(TAG, "BACK CAMERA TO SET");
        }
        try {
            // cycle through all the cameras go obtain the correct (front or back) one
            for (String cameraId : mCameraManager.getCameraIdList()) {
                mCameraId = cameraId; // set global var here in case lensFacing never evaluates true
                CameraCharacteristics characteristics
                        = mCameraManager.getCameraCharacteristics(cameraId);

                if (characteristics.get(CameraCharacteristics.LENS_FACING) == lensFacing) {
                    return;
                } else  {
                    continue;
                }
            }
            return;
        } catch (CameraAccessException e) {
            e.printStackTrace();
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
    }

    /**
     * Selects the optimal image, preview, and video sizes
     */
    private void configureSizes()  {

        List<Size> imageSizes = getSupportedImageSizes();
        List<Size> previewSizes = getSupportedPreviewSizes();
        List<Size> videoSizes = getSupportedVideoSizes();
        Size largest = CameraUtils.getLargestSize(imageSizes);

        mPreviewSize = CameraUtils.chooseOptimalSize(previewSizes,
                mScreenSize.getWidth(), mScreenSize.getHeight(), largest);
        mVideoSize = CameraUtils.chooseOptimalSize(videoSizes,
                mScreenSize.getWidth(), mScreenSize.getHeight(), largest);
        mImageReader = ImageReader.newInstance(largest.getWidth(), largest.getHeight(),
                ImageFormat.JPEG, /*maxImages*/2); // TODO try using a smaller size, not necessarily largest
        mImageReader.setOnImageAvailableListener(
                mOnImageAvailableListener, mBackgroundHandler);

        mCameraView.setPreviewSize(mPreviewSize);

        Log.i(TAG, "Preview Size: " + mPreviewSize.getWidth() + ", " + mPreviewSize.getHeight());
        Log.i(TAG, "Video Size: " + mVideoSize.getWidth() + ", " + mPreviewSize.getHeight());
        Log.i(TAG, "Image Size: " + largest.getWidth() + ", " + largest.getHeight());
    }

    /**
     * Starts a background thread and its {@link Handler}.
     */
    private void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("CameraBackground");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }

    /**
     * Stops the background thread and its {@link Handler}.
     */
    private void stopBackgroundThread() {
        try {
            mBackgroundThread.quitSafely();
            mBackgroundThread.join(); // if it hangs, just go ahead and end it
            mBackgroundThread = null;
            mBackgroundHandler = null;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void openCamera() {
        Log.d(TAG, "Opening Camera");
        startBackgroundThread(); // start camera thread
        setCamera(); // choose the front or back (based on global camera state
        configureSizes(); // choose correct preview, video, image sizes

        // attempt to open selected camera
        try {
            if (!mCameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                throw new RuntimeException("Time out waiting to lock camera opening.");
            }
            mCameraManager.openCamera(mCameraId, mStateCallback, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while trying to lock camera opening.", e);
        }

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void closeCamera() {
        try {
            Log.d(TAG, "Acquiring lock");
            mCameraOpenCloseLock.acquire();
            Log.d(TAG, "Closing capture Session");
            if (null != mCaptureSession) {
                mCaptureSession.close();
                mCaptureSession = null;
            }
            Log.d(TAG, "Closing camera device");
            if (null != mCameraDevice) {
                mCameraDevice.close();
                mCameraDevice = null;
            }
            Log.d(TAG, "Releasing media recoder");
            if (null != mMediaRecorder) {
                mMediaRecorder.release();
                mMediaRecorder = null;
            }
            Log.d(TAG, "Closing image reader");
            if (null != mImageReader) {
                mImageReader.close();
                mImageReader = null;
            }
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while trying to lock camera closing.", e);
        } finally {
            mCameraOpenCloseLock.release();
            //stopBackgroundThread();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void startPreview() {
        // TODO create a separate "requestPreview" function? avoids creating new capture session
        Log.d(TAG, "Creating cam preview session");
        try {
            SurfaceTexture texture = mCameraView.getSurfaceTexture();

            // We configure the size of default buffer to be the size of camera preview we want.
            texture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());

            List<Surface> surfaces = new ArrayList<Surface>(); // the surfaces to pass to capturesession

            // This is the output Surface we need to start preview.
            Surface previewSurface = new Surface(texture);
            surfaces.add(previewSurface);

            mMediaRecorder = new MediaRecorder();
            setUpMediaRecorder();
            Surface recorderSurface = mMediaRecorder.getSurface();
            surfaces.add(recorderSurface);

            // We set up a CaptureRequest.Builder with the output Surface.
            surfaces.add(mImageReader.getSurface());
            mPreviewRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            mPreviewRequestBuilder.addTarget(previewSurface);

            Log.d(TAG, "Finishing camera preview session creation");

            try {
                // Here, we create a CameraCaptureSession for camera preview.
                mCameraDevice.createCaptureSession(surfaces,
                        new CameraCaptureSession.StateCallback() {

                            @Override
                            public void onConfigured(CameraCaptureSession cameraCaptureSession) {
                                // The camera is already closed
                                if (null == mCameraDevice) {
                                    return;
                                }

                                // When the session is ready, we start displaying the preview.
                                mCaptureSession = cameraCaptureSession;
                                try {
                                    // Auto focus should be continuous for camera preview.
                                    mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                                            CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                                    mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE,
                                            CaptureRequest.CONTROL_AE_MODE_ON);


                                    // Finally, we start displaying the camera preview.
                                    mPreviewRequest = mPreviewRequestBuilder.build();
                                    Log.d(TAG, "Starting preview repeating request");
                                    try {
                                        mCaptureSession.setRepeatingRequest(mPreviewRequest,
                                                mCaptureCallback, mBackgroundHandler);
                                    } catch (IllegalStateException e) {
                                        Log.e(TAG, "Preview request failed, trying again");
                                        startPreview();
                                        e.printStackTrace();
                                    }
                                } catch (CameraAccessException e) {
                                    e.printStackTrace();
                                }
                            }

                            @Override
                            public void onConfigureFailed(CameraCaptureSession cameraCaptureSession) {
                                Log.e(TAG, "Configure Failed");
                            }
                        }, null
                );
            } catch(IllegalStateException e)    {
                e.printStackTrace();
            }
            Log.d(TAG, "Finished createcapturesession");
        } catch (CameraAccessException e) {
            e.printStackTrace();
        } catch (IOException e1)  {
            // TODO handle media recorder IOexception
            e1.printStackTrace();
        } catch (NullPointerException e2)   {
            // TODO handle null pointer (such as null camera device)
            e2.printStackTrace();
        }
    }

    /**
     * Sets up video specs
     * @throws IOException
     */
    private void setUpMediaRecorder() throws IOException {
        if(CameraStates.IS_SOUND) {
            mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        }
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mMediaRecorder.setOutputFile(mVideoPath);
        mMediaRecorder.setVideoEncodingBitRate(10000000);
        mMediaRecorder.setVideoFrameRate(16);
        mMediaRecorder.setVideoSize(mVideoSize.getWidth(), mVideoSize.getHeight());
        mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
        if(CameraStates.IS_SOUND)
            mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        int rotation = CameraUtils.getRotation(mContext);
        int orientation = ORIENTATIONS.get(rotation);
        if(CameraStates.isFrontCamera())    {
            orientation += 180; // hopefully this will cause video to save right side up
        }
        mMediaRecorder.setOrientationHint(orientation);
        mMediaRecorder.prepare();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void startRecording() {
        CameraStates.FILE_TYPE = CameraStates.FILE_VIDEO;
        try {
            // set record request parameters
            CaptureRequest.Builder mRecordRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);
            mRecordRequestBuilder.addTarget(mMediaRecorder.getSurface());
            mRecordRequestBuilder.addTarget(new Surface(mCameraView.getSurfaceTexture()));
            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE,
                    CaptureRequest.CONTROL_AE_MODE_ON);
            // request capture session to show preview for recording
            try {
                mCaptureSession.setRepeatingRequest(mRecordRequestBuilder.build(),
                        mCaptureCallback, mBackgroundHandler);
            } catch (IllegalStateException e) {
                Log.e(TAG, "Preview request failed, trying again");
                startPreview();
                e.printStackTrace();
                return;
            }
            // reset and start mediarecorder
            //mMediaRecorder.reset();
            Log.d(TAG, "Starting media recorder");
            mMediaRecorder.start();
        } catch(CameraAccessException e)    {
            e.printStackTrace();
        } catch(IllegalStateException e1)    {
            Log.e(TAG, "MediaRecorder could not be started correctly");
            e1.printStackTrace();
            // stop the mediarecorder and restart the preview
            mMediaRecorder.stop();
            startPreview();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void stopRecording() {
        try {
            Log.d(TAG, "Media recorder stop");
            mMediaRecorder.stop();// Stop recording
            Log.d(TAG, "media Recorder reset");
            mMediaRecorder.reset();
        } catch(IllegalStateException e)    {
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
        lockFocus();
    }

    /**
     * Lock the focus as the first step for a still image capture.
     */
    private void lockFocus() {
        try {
            // This is how to tell the camera to lock focus.
            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,
                    CameraMetadata.CONTROL_AF_TRIGGER_START);
            // Tell #mCaptureCallback to wait for the lock.
            mState = CaptureState.STATE_WAITING_LOCK;
            try {
                mCaptureSession.setRepeatingRequest(mPreviewRequestBuilder.build(), mCaptureCallback,
                        mBackgroundHandler);
            } catch(IllegalArgumentException e) {
                Log.e(TAG, "Lock Focus Request failed");
                startPreview();
                e.printStackTrace();
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * Run the precapture sequence for capturing a still image. This method should be called when we
     * get a response in {@link #mCaptureCallback} from {@link #lockFocus()}.
     */
    private void runPrecaptureSequence() {
        try {
            // This is how to tell the camera to trigger.
            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER,
                    CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_START);
            // Tell #mCaptureCallback to wait for the precapture sequence to be set.
            mState = CaptureState.STATE_WAITING_PRECAPTURE;
            mCaptureSession.capture(mPreviewRequestBuilder.build(), mCaptureCallback,
                    mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * Capture a still picture. This method should be called when we get a response in
     * {@link #mCaptureCallback} from both {@link #lockFocus()}.
     */
    private void captureStillPicture() {
        Log.d(TAG, "In capture still picture");
        try {
            if (null == mCameraDevice) return;
            // This is the CaptureRequest.Builder that we use to take a picture.
            final CaptureRequest.Builder captureBuilder =
                    mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureBuilder.addTarget(mImageReader.getSurface());

            // Use the same AE and AF modes as the preview.
            captureBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                    CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);

            if(CameraStates.FLASH_STATE == CameraStates.AUTO_FLASH) {
                captureBuilder.set(CaptureRequest.CONTROL_AE_MODE,
                        CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH); // auto flash
            } else if(CameraStates.FLASH_STATE == CameraStates.NO_FLASH)  {
                captureBuilder.set(CaptureRequest.CONTROL_AE_MODE,
                        CaptureRequest.CONTROL_AE_MODE_ON); // no flash
            } else  {
                captureBuilder.set(CaptureRequest.CONTROL_AE_MODE,
                        CaptureRequest.CONTROL_AE_MODE_ON_ALWAYS_FLASH); // always flash
            }

            // Orientation; device is always portrait
            int rotation = CameraUtils.getRotation(mContext);
            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, ORIENTATIONS.get(rotation));

            CameraCaptureSession.CaptureCallback CaptureCallback
                    = new CameraCaptureSession.CaptureCallback() {

                @Override
                public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request,
                                               TotalCaptureResult result) {
                    // TODO if flash, flash animation would go here
                    Log.d(TAG, "Capture completed!");
                    // unlockFocus(); // don't need to unlock focus since exiting camera
                    mCameraActionCallback.onCaptureCompleted();
                }
            };

            mCaptureSession.stopRepeating();
            try {
                mCaptureSession.capture(captureBuilder.build(), CaptureCallback, null);
            } catch(IllegalArgumentException e) {
                Log.e(TAG, "Capture threw exception");
                startPreview(); // restart camera
                e.printStackTrace();
            }
            // TODO flash animation would go here...
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * Unlock the focus. This method should be called when still image capture sequence is finished.
     */
    private void unlockFocus() {
        Log.d(TAG, "Unlocking focus");
        try {
            // Reset the autofucos trigger
            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,
                    CameraMetadata.CONTROL_AF_TRIGGER_CANCEL);

            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE,
                    CaptureRequest.CONTROL_AE_MODE_ON);

            mCaptureSession.capture(mPreviewRequestBuilder.build(), mCaptureCallback,
                    mBackgroundHandler);

            // After this, the camera will go back to the normal state of preview.
            mState = CaptureState.STATE_PREVIEW;
            // TODO since editor is starting, does it need to go back?
            mCaptureSession.setRepeatingRequest(mPreviewRequest, mCaptureCallback,
                    mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
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
        switch(CameraStates.FLASH_STATE)    {
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
        //startPreview(); // TODO is it necessary to restart the preview to update flash requirements?
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void toggleSound() {
        CameraStates.IS_SOUND = !CameraStates.IS_SOUND;
        mMediaRecorder.reset();
        // create a new camera capture session to pass media surface after re-preparing
        closeCamera();
        openCamera();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected List<Size> getSupportedPreviewSizes() {
        try {
            CameraCharacteristics cc = mCameraManager.getCameraCharacteristics(mCameraId);
            StreamConfigurationMap map = cc.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            return Arrays.asList(map.getOutputSizes(SurfaceTexture.class));
        } catch(CameraAccessException e)    {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected List<Size> getSupportedVideoSizes() {
        try {
            CameraCharacteristics cc = mCameraManager.getCameraCharacteristics(mCameraId);
            StreamConfigurationMap map = cc.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            return Arrays.asList(map.getOutputSizes(MediaRecorder.class));
        } catch(CameraAccessException e)    {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected List<Size> getSupportedImageSizes() {
        try {
            CameraCharacteristics cc = mCameraManager.getCameraCharacteristics(mCameraId);
            StreamConfigurationMap map = cc.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            return Arrays.asList(map.getOutputSizes(ImageFormat.JPEG));
        } catch(CameraAccessException e)    {
            e.printStackTrace();
        }
        return null;
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
     * Converts image buffer to bitmap
     * @param img the image buffer
     * @return the output bitmap
     */
    private Bitmap imageToBitmap(Image img) {
        ByteBuffer buffer = img.getPlanes()[0].getBuffer();
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);
        Bitmap bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        img.close();
        return bmp;
    }
}
