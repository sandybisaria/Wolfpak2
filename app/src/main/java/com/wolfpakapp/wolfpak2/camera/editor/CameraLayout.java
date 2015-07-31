package com.wolfpakapp.wolfpak2.camera.editor;

import android.app.Activity;
import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.RectF;
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
import android.os.AsyncTask;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.wolfpakapp.wolfpak2.R;
import com.wolfpakapp.wolfpak2.camera.preview.AutoFitTextureView;
import com.wolfpakapp.wolfpak2.camera.preview.CameraFragment;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * Layout handler that contains camera preview.  Performs photo capture and video recording
 * Adapted from Google Sample code "android-Camera2Basic" @
 * https://github.com/googlesamples/android-Camera2Basic/blob/master/Application/src/main/java/com/example/android/camera2basic/Camera2BasicFragment.java
 * and Google Sample code "android-Video2Basic" @
 * https://github.com/googlesamples/android-Camera2Video/blob/master/Application/src/main/java/com/example/android/camera2video/Camera2VideoFragment.java
 *
 * @author Roland Fong
 */
public class CameraLayout {

    private static final String TAG = "TAG-CameraLayout";

    /* CAMERA STILL CAPTURE STATES*/
    private static final int STATE_PREVIEW = 0;
    private static final int STATE_WAITING_LOCK = 1;
    private static final int STATE_WAITING_PRECAPTURE = 2;
    private static final int STATE_WAITING_NON_PRECAPTURE= 3;
    private static final int STATE_PICTURE_TAKEN = 4;

    private static int mState = STATE_PREVIEW;

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

    /**
     * The Fragment container
     */
    private CameraFragment mFragment;
    private AutoFitTextureView mTextureView;

    /*CAMERA DEVICE DETAILS*/
    private String mCameraId;
    private CameraCaptureSession mCaptureSession;
    private CameraDevice mCameraDevice;
    private Size mPreviewSize;
    private Size mVideoSize;
    private MediaRecorder mMediaRecorder;

    private static int mFace; // which direction camera is facing
    private static boolean mFlash; // true if flash is on
    private static boolean mSound; // true if sound is on
    private static boolean mLockingForEditor; // true if about to switch to picture editor
    private static boolean mIsRecordingVideo;

    private Button mCaptureButton;
    private ImageButton mSwitchButton;
    private ImageButton mFlashButton;
    private ImageButton mSoundButton;
    private ProgressBar mProgressBar;
    private ImageView mScreenFlash;

    private CountDownTimer mCountDownTimer; // to limit video recording to 10s
    private int count;

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
            createCameraPreviewSession();
        }

        @Override
        public void onDisconnected(CameraDevice cameraDevice) {
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
                if(null != mFragment.getActivity())   {
                    mFragment.getActivity().finish();
                }
            }
        }
    };

    /*THREAD & IMAGE HANDLING*/
    private Handler mTouchHandler;
    private HandlerThread mVideoStarterThread;
    private Handler mVideoStarterHandler;
    private HandlerThread mBackgroundThread;
    private Handler mBackgroundHandler;
    private ImageReader mImageReader;

    private Runnable videoRunner = new Runnable()   {
        @Override
        public void run() {
            if(!mIsRecordingVideo)  {
                Log.d(TAG, "Will record video");
                startRecordingVideo();
            }
        }
    };

    private final ImageReader.OnImageAvailableListener mOnImageAvailableListener
            = new ImageReader.OnImageAvailableListener() {
        @Override
        public void onImageAvailable(ImageReader reader) {
            Log.d(TAG, "Image Avail and set");
            mFragment.setImage(reader.acquireNextImage());
        }
    };

    /*CAMERA CAPTURE HANDLING*/
    private CaptureRequest.Builder mPreviewRequestBuilder;
    private CaptureRequest mPreviewRequest;

    private CameraCaptureSession.CaptureCallback mCaptureCallback
            = new CameraCaptureSession.CaptureCallback()    {
        private void process(CaptureResult result)  {
            switch(mState)  {
                case STATE_PREVIEW: break;
                case STATE_WAITING_LOCK:
                    Log.d(TAG, "In STATE_WAITING_LOCK");
                    int afState = result.get(CaptureResult.CONTROL_AF_STATE);
                    // front camera is always autofocused, so ignore focus if front camera
                    if (CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED == afState ||
                            CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED == afState ||
                            mFace == CameraCharacteristics.LENS_FACING_FRONT) {
                        Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
                        if (aeState == null ||
                                aeState == CaptureResult.CONTROL_AE_STATE_CONVERGED) {
                            mState = STATE_WAITING_NON_PRECAPTURE;
                            //mState = STATE_PICTURE_TAKEN;
                            captureStillPicture();
                        } else {
                            runPrecaptureSequence();
                        }
                    }
                    break;
                case STATE_WAITING_PRECAPTURE:
                    Log.d(TAG, "In STATE_WAITING_PRECAP");
                    Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
                    if (aeState == null ||
                            aeState == CaptureResult.CONTROL_AE_STATE_PRECAPTURE ||
                            aeState == CaptureRequest.CONTROL_AE_STATE_FLASH_REQUIRED) {
                        mState = STATE_WAITING_NON_PRECAPTURE;
                    }
                    break;
                case STATE_WAITING_NON_PRECAPTURE:
                    Log.d(TAG, "In STATE_WAITING_NON_PRECAP");
                    Integer aeState1 = result.get(CaptureResult.CONTROL_AE_STATE);
                    if (aeState1 == null || aeState1 != CaptureResult.CONTROL_AE_STATE_PRECAPTURE) {
                        mState = STATE_PICTURE_TAKEN;
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
     * Creates the layout.
     * @param fragment the fragment container
     * @param view to locate components
     */
    public CameraLayout(CameraFragment fragment, View view) {
        mFragment = fragment;
        mTextureView = fragment.getTextureView();
        // set up buttons
        mCaptureButton = (Button) view.findViewById(R.id.btn_takepicture); // take picture button
        mCaptureButton.setOnTouchListener(fragment);

        mSwitchButton = (ImageButton) view.findViewById(R.id.btn_switch); // switch camera button
        mSwitchButton.setOnClickListener(fragment);

        mFlashButton = (ImageButton) view.findViewById(R.id.btn_flash); // flash button
        mFlashButton.setOnClickListener(fragment);
        mFlash = false; // set to no flash default
        mFlashButton.setImageResource(R.drawable.no_flash);

        mSoundButton = (ImageButton) view.findViewById(R.id.btn_sound); // sound button
        mSoundButton.setOnClickListener(fragment);
        mSound = true; // set to sound on default;

        mScreenFlash = (ImageView) view.findViewById(R.id.screen_flash);

        // progress bar and timer for video recording
        mProgressBar = (ProgressBar) view.findViewById(R.id.progress_bar); // progress bar for video
        count = 0;
        mCountDownTimer = new CountDownTimer(10000, 100) {
            @Override
            public void onTick(long millisUntilFinished) {
                count++;
                mProgressBar.setProgress(count);
            }
            @Override
            public void onFinish() {
                count++;
                mProgressBar.setProgress(count);
                stopRecordingVideo();
            }
        };
    }

    /**
     * Actions to perform when surface texture is available in fragment
     * @param width
     * @param height
     */
    public void onSurfaceTextureAvailable(int width, int height)   {
        mFace = CameraCharacteristics.LENS_FACING_BACK;
        openCamera(width, height, CameraCharacteristics.LENS_FACING_BACK);
    }

    public void onPause()   {
        closeCamera();
        stopBackgroundThread();
    }

    public void onResume()  {
        mLockingForEditor = false;
        startBackgroundThread();
        // if activity resumes from pause, OnSurfaceTextureAvailable may not be called
        if(mTextureView.isAvailable())
            openCamera(mTextureView.getWidth(), mTextureView.getHeight(), mFace);
    }

    public void onClick(int id) {
        switch(id)    {
            case R.id.btn_switch:
                switchCamera();
                break;
            case R.id.btn_flash:
                toggleFlash();
                break;
            case R.id.btn_sound:
                toggleSound();
                break;
        }
    }

    public boolean onTouch(int id, MotionEvent event) {
        switch(id)   {
            case R.id.btn_takepicture:
                if(event.getAction() == MotionEvent.ACTION_DOWN)   {
                    startTouchHandler();
                    Log.d(TAG, "Take Picture Action Down");
                    mTouchHandler.postDelayed(videoRunner, 600); // if hold lasts 0.6s, record video
                }
                else if(event.getAction() == MotionEvent.ACTION_UP) {
                    Log.d(TAG, "Take Picture Action Up");
                    mTouchHandler.removeCallbacks(videoRunner);
                    stopTouchHandler();
                    if(mIsRecordingVideo)   { // if indeed held for 0.6s, mIsRecordingVideo should be true
                        mCountDownTimer.cancel(); // needed if finished before 10s
                        stopRecordingVideo();
                    } else if (!mLockingForEditor)  {
                        // else mIsRecordingVideo is false, so take picture if not going to editor (from video)
                        Log.d(TAG, "About to call takePicture");
                        takePicture();
                    }
                }
                break;
        }
        return false; // when set true, the state_pressed won't activate!
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
    private static Size chooseOptimalSize(Size[] choices, int width, int height, Size aspectRatio)  {
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
            return choices[0];
        }
    }

    /**
     * Sets up member vars related to camera and picks specified camera
     * @param width
     * @param height
     */
    public void setUpCameraOutputs(int width, int height, int lensFacing)   {
        Activity activity = mFragment.getActivity();
        CameraManager manager = (CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);
        try {
            for (String cameraId : manager.getCameraIdList()) {
                CameraCharacteristics characteristics
                        = manager.getCameraCharacteristics(cameraId);

                if (characteristics.get(CameraCharacteristics.LENS_FACING) != lensFacing) {
                    continue;
                }

                StreamConfigurationMap map = characteristics.get(
                        CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

                Size largest = Collections.max(
                        Arrays.asList(map.getOutputSizes(ImageFormat.JPEG)),
                        new CompareSizesByArea());
//                mImageReader = ImageReader.newInstance(largest.getWidth(), largest.getHeight(),
//                        ImageFormat.JPEG, /*maxImages*/2);
//                mImageReader.setOnImageAvailableListener(
//                        mOnImageAvailableListener, mBackgroundHandler);

                mPreviewSize = chooseOptimalSize(map.getOutputSizes(SurfaceTexture.class),
                        width, height, largest);
                mVideoSize = chooseOptimalSize(map.getOutputSizes(MediaRecorder.class),
                        width, height, largest);
                // if app ever will support landscape, aspect ratio needs to be changed here.
                mTextureView.setAspectRatio(
                        mPreviewSize.getHeight(), mPreviewSize.getWidth());
                // set image size to be size of screen
                mImageReader = ImageReader.newInstance(mPreviewSize.getWidth(), mPreviewSize.getHeight(),
                        ImageFormat.JPEG, /*maxImages*/2);
                mImageReader.setOnImageAvailableListener(
                        mOnImageAvailableListener, mBackgroundHandler);

                mCameraId = cameraId;
                return;
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
    }

    /**
     * Opens the camera specified by ID
     */
    private void openCamera(int width, int height, int lensFacing) {
        Log.d(TAG, "Opening Camera");
        setUpCameraOutputs(width, height, lensFacing);
        configureTransform(width, height);
        mMediaRecorder = new MediaRecorder();
        Activity activity = mFragment.getActivity();
        CameraManager manager = (CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);
        try {
            if (!mCameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                throw new RuntimeException("Time out waiting to lock camera opening.");
            }
            manager.openCamera(mCameraId, mStateCallback, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while trying to lock camera opening.", e);
        }
    }

    /**
     * Closes the current {@link CameraDevice}.
     */
    private void closeCamera() {
        try {
            mCameraOpenCloseLock.acquire();
            if (null != mCaptureSession) {
                mCaptureSession.close();
                mCaptureSession = null;
            }
            if (null != mCameraDevice) {
                mCameraDevice.close();
                mCameraDevice = null;
            }
            if (null != mMediaRecorder) {
                mMediaRecorder.release();
                mMediaRecorder = null;
            }
            if (null != mImageReader) {
                mImageReader.close();
                mImageReader = null;
            }
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while trying to lock camera closing.", e);
        } finally {
            mCameraOpenCloseLock.release();
        }
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
            mBackgroundThread.join();
            mBackgroundThread = null;
            mBackgroundHandler = null;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void startVideoStarterThread()  {
        mVideoStarterThread = new HandlerThread("VideoStarter");
        mVideoStarterThread.start();
        mVideoStarterHandler = new Handler(mVideoStarterThread.getLooper());
    }

    private void stopVideoStarterThread()   {
        try {
            mVideoStarterThread.quitSafely();
            mVideoStarterThread.join();
            mVideoStarterThread = null;
            mVideoStarterHandler = null;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Creates a new {@link CameraCaptureSession} for camera preview.
     */
    private void createCameraPreviewSession() {
        try {
            SurfaceTexture texture = mTextureView.getSurfaceTexture();
            assert texture != null;

            // We configure the size of default buffer to be the size of camera preview we want.
            texture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());

            List<Surface> surfaces = new ArrayList<Surface>();
            // This is the output Surface we need to start preview.
            Surface previewSurface = new Surface(texture);
            surfaces.add(previewSurface);

            // We set up a CaptureRequest.Builder with the output Surface.
            if(mIsRecordingVideo)   {
                setUpMediaRecorder();
                Surface recorderSurface = mMediaRecorder.getSurface();
                surfaces.add(recorderSurface);
                mPreviewRequestBuilder
                        = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);
                mPreviewRequestBuilder.addTarget(recorderSurface);
            } else  {
                surfaces.add(mImageReader.getSurface());
                mPreviewRequestBuilder
                        = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            }
            mPreviewRequestBuilder.addTarget(previewSurface);

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

                                if(mFlash) {
                                    mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE,
                                            CaptureRequest.CONTROL_AE_MODE_ON_ALWAYS_FLASH); // auto flash
                                } else  {
                                    mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE,
                                            CaptureRequest.CONTROL_AE_MODE_ON); // no flash
                                }

                                // Finally, we start displaying the camera preview.
                                mPreviewRequest = mPreviewRequestBuilder.build();
                                Log.d(TAG, "Starting Preview Req");
                                mCaptureSession.setRepeatingRequest(mPreviewRequest,
                                        mCaptureCallback, mBackgroundHandler);
                            } catch (CameraAccessException e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onConfigureFailed(CameraCaptureSession cameraCaptureSession) {
                            Log.e(TAG, "Configure Failed");
                            createCameraPreviewSession(); // try again
                        }
                    }, null
            );
        } catch (CameraAccessException e) {
            e.printStackTrace();
        } catch(IOException e1) {
            e1.printStackTrace();
        }
    }

    /**
     * Configures the necessary {@link android.graphics.Matrix} transformation to `mTextureView`.
     * This method should be called after the camera preview size is determined in
     * setUpCameraOutputs and also the size of `mTextureView` is fixed.
     *
     * @param viewWidth  The width of `mTextureView`
     * @param viewHeight The height of `mTextureView`
     */
    public void configureTransform(int viewWidth, int viewHeight) {
        Activity activity = mFragment.getActivity();
        if (null == mTextureView || null == mPreviewSize || null == activity) {
            return;
        }
        int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        Matrix matrix = new Matrix();
        RectF viewRect = new RectF(0, 0, viewWidth, viewHeight);
        RectF bufferRect = new RectF(0, 0, mPreviewSize.getHeight(), mPreviewSize.getWidth());
        float centerX = viewRect.centerX();
        float centerY = viewRect.centerY();
        if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
            bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY());
            matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);
            float scale = Math.max(
                    (float) viewHeight / mPreviewSize.getHeight(),
                    (float) viewWidth / mPreviewSize.getWidth());
            matrix.postScale(scale, scale, centerX, centerY);
            matrix.postRotate(90 * (rotation - 2), centerX, centerY);
        }
        mTextureView.setTransform(matrix);
    }

    /**
     * Sets up video specs
     * @throws IOException
     */
    private void setUpMediaRecorder() throws IOException {
        final Activity activity = mFragment.getActivity();
        if (null == activity) {
            return;
        }
        if(mSound) {
            mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        }
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mFragment.setVideoPath((new File(activity.getExternalFilesDir(null), "video.mp4")).getAbsolutePath());
        mMediaRecorder.setOutputFile(mFragment.getVideoPath());
        mMediaRecorder.setVideoEncodingBitRate(10000000);
        mMediaRecorder.setVideoFrameRate(30);
        mMediaRecorder.setVideoSize(mVideoSize.getWidth(), mVideoSize.getHeight());
        mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.DEFAULT);
        if(mSound)
            mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        int orientation = ORIENTATIONS.get(rotation);
        if(mFace == CameraCharacteristics.LENS_FACING_FRONT)    {
            orientation += 180; // hopefully this will cause video to save right side up
        }
        mMediaRecorder.setOrientationHint(orientation);
        mMediaRecorder.prepare();
    }

    /**
     * Starts video Recording
     */
    private void startRecordingVideo() {
        mIsRecordingVideo = true;
        mFragment.setFileType(CameraFragment.FILE_TYPE_VIDEO);
        mMediaRecorder.reset();
        createCameraPreviewSession(); // open preview outside thread!
        mCountDownTimer.start();

        startVideoStarterThread();
        mVideoStarterHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    mMediaRecorder.start();// Start recording
                    Thread.sleep(1000); // make sure the video lasts at least a second
                    mVideoStarterThread.quitSafely();
                } catch (IllegalStateException e) {
                    e.printStackTrace();
                } catch (InterruptedException e1) {
                    e1.printStackTrace();
                }
            }
        });
    }

    /**
     * Stops video recording in separate thread to avoid disrupting UI in event of hang
     */
    private void stopRecordingVideo() {
        try {
            mVideoStarterThread.join(); // wait for video recording starting to finish!
        } catch(InterruptedException e) {
            e.printStackTrace();
        }
        stopVideoStarterThread();

        mIsRecordingVideo = false;
        mLockingForEditor = true; // prevent action_up from accidentally taking picture

        mMediaRecorder.stop();// Stop recording
        mMediaRecorder.reset();
        mProgressBar.setProgress(0);
        count=0;
        startEditor();
    }

    /**
     * Initiate a still image capture.
     */
    private void takePicture() {
        mFragment.setFileType(CameraFragment.FILE_TYPE_IMAGE);
        lockFocus();
    }

    /**
     * Lock the focus as the first step for a still image capture.
     */
    private void lockFocus() {
        try {
            Log.d(TAG, "Previewrequestbuilder lock focus");
            // This is how to tell the camera to lock focus.
            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,
                    CameraMetadata.CONTROL_AF_TRIGGER_START);
            // Tell #mCaptureCallback to wait for the lock.
            mState = STATE_WAITING_LOCK;
            Log.d(TAG, "STATE_WAITING_LOCK, Repeating Request set");
            mCaptureSession.setRepeatingRequest(mPreviewRequestBuilder.build(), mCaptureCallback,
                    mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * Switches between front and back cameras
     */
    public void switchCamera()  {
        if(mFace == CameraCharacteristics.LENS_FACING_BACK)
            mFace = CameraCharacteristics.LENS_FACING_FRONT;
        else
            mFace = CameraCharacteristics.LENS_FACING_BACK;
        closeCamera();
        openCamera(mTextureView.getWidth(), mTextureView.getHeight(), mFace);
    }

    /**
     * Toggle flash on and off
     */
    public void toggleFlash()   {
        if(mFlash)  {
            mFlashButton.setImageResource(R.drawable.no_flash);
        } else  {
            mFlashButton.setImageResource(R.drawable.flash);
        }
        closeCamera();
        mFlash = !mFlash;
        openCamera(mTextureView.getWidth(), mTextureView.getHeight(), mFace);
    }

    /**
     * Toggle sound on and off
     */
    public void toggleSound()   {
        if(mSound)  {
            mSoundButton.setImageResource(R.drawable.no_sound);
        } else  {
            mSoundButton.setImageResource(R.drawable.sound);
        }

        mSound = !mSound;

        mMediaRecorder.reset();
        try {
            setUpMediaRecorder();
        } catch(IOException e)  {
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
            mState = STATE_WAITING_PRECAPTURE;
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

        Log.d(TAG, "About to cap still pic");
        try {
            final Activity activity = mFragment.getActivity();
            if (null == activity || null == mCameraDevice) {
                return;
            }
            Log.d(TAG, "Building still pic request");
            // This is the CaptureRequest.Builder that we use to take a picture.
            final CaptureRequest.Builder captureBuilder =
                    mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureBuilder.addTarget(mImageReader.getSurface());

            // Use the same AE and AF modes as the preview.
            captureBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                    CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);

            if(mFlash) {
                captureBuilder.set(CaptureRequest.CONTROL_AE_MODE,
                        CaptureRequest.CONTROL_AE_MODE_ON_ALWAYS_FLASH);
            } else  {
                captureBuilder.set(CaptureRequest.CONTROL_AE_MODE,
                        CaptureRequest.CONTROL_AE_MODE_ON);
            }

            // Orientation
            int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, ORIENTATIONS.get(rotation));

            CameraCaptureSession.CaptureCallback CaptureCallback
                    = new CameraCaptureSession.CaptureCallback() {

                @Override
                public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request,
                                               TotalCaptureResult result) {
                    if(mFlash)// if camera flashed, do screen flash here
                        startFlashAnimation();
                    unlockFocus();
                    startEditor();
                }
            };

            mCaptureSession.stopRepeating();
            Log.d(TAG, "About to capture");
            mCaptureSession.capture(captureBuilder.build(), CaptureCallback, null);
            // flash the screen like a camera, stall for time since capture tends to take a while
            if(!mFlash) // if camera flash is used, don't screenflash now b/c it'll be too early!
                startFlashAnimation();
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * Unlock the focus. This method should be called when still image capture sequence is finished.
     */
    private void unlockFocus() {
        Log.d(TAG, "unlocking focus");
        try {
            // Reset the autofucos trigger
            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,
                    CameraMetadata.CONTROL_AF_TRIGGER_CANCEL);
            if(mFlash) {
                mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE,
                        CaptureRequest.CONTROL_AE_MODE_ON_ALWAYS_FLASH);
            } else  {
                mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE,
                        CaptureRequest.CONTROL_AE_MODE_ON);
            }
            mCaptureSession.capture(mPreviewRequestBuilder.build(), mCaptureCallback,
                    mBackgroundHandler);
            // After this, the camera will go back to the normal state of preview.
            mState = STATE_PREVIEW;
            mCaptureSession.setRepeatingRequest(mPreviewRequest, mCaptureCallback,
                    mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * Mimics a camera flash on the screen by fading in and out a white rectangular border,
     * like the existing camera app.  Runs asynchronously
     */
    public void startFlashAnimation()   {
        mFragment.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Handler flashHandler = new Handler();
                // fade in
                flashHandler.post(new Runnable()    {
                    @Override
                    public void run() {
                        fade(0, 255, 300, true);
                    }
                });
                // fade out
                flashHandler.post(new Runnable()    {
                    @Override
                    public void run() {
                        fade(255, 0, 600, false);
                    }
                });
            }
        });
    }

    private void fade(final int begin_alpha, final int end_alpha, int time,
                              final boolean fadein) {

        mScreenFlash.setImageAlpha(begin_alpha);

        if (fadein) {
            mScreenFlash.setVisibility(View.VISIBLE);
        }

        Animation a = new Animation() {
            @Override
            protected void applyTransformation(float interpolatedTime,
                                               Transformation t) {
                if (interpolatedTime == 1) {
                    mScreenFlash.setImageAlpha(end_alpha);

                    if (!fadein) {
                        mScreenFlash.setVisibility(View.GONE);
                    }
                } else {
                    int new_alpha = (int) (begin_alpha + (interpolatedTime * (end_alpha - begin_alpha)));
                    mScreenFlash.setImageAlpha(new_alpha);
                    mScreenFlash.requestLayout();
                }
            }

            @Override
            public boolean willChangeBounds() {
                return true;
            }
        };

        a.setDuration(time);
        mScreenFlash.startAnimation(a);
    }

    public static int getFace()    {
        return mFace;
    }

    public void startTouchHandler() {
        mTouchHandler = new Handler();
    }

    public void stopTouchHandler()  {
        mTouchHandler = null;
    }

    private void startEditor()   {
        //closeCamera();
        Log.d(TAG, "Starting editor function");
        mFragment.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mFragment.switchLayouts();
            }
        });
    }

    /**
     * Hides all the camera icons
     */
    public void hide()  {
        Log.d(TAG, "Hiding camera");
        mFragment.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mCaptureButton.setVisibility(View.GONE);
                mFlashButton.setVisibility(View.GONE);
                mSwitchButton.setVisibility(View.GONE);
                mSoundButton.setVisibility(View.GONE);
            }
        });
    }

    /**
     * Shows all the camera icons and starts up camera
     */
    public void show()  {
        mFragment.getActivity().runOnUiThread(new Runnable() {
              @Override
              public void run() {
                  mCaptureButton.setVisibility(View.VISIBLE);
                  mFlashButton.setVisibility(View.VISIBLE);
                  mSwitchButton.setVisibility(View.VISIBLE);
                  mSoundButton.setVisibility(View.VISIBLE);
              }
          });
    }

    /**
     * Compares two {@code Size}s based on their areas.
     */
    static class CompareSizesByArea implements Comparator<Size> {
        @Override
        public int compare(Size lhs, Size rhs) {
            // We cast here to ensure the multiplications won't overflow
            return Long.signum((long) lhs.getWidth() * lhs.getHeight() -
                    (long) rhs.getWidth() * rhs.getHeight());
        }
    }
}
