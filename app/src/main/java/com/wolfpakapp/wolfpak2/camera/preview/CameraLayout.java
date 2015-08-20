package com.wolfpakapp.wolfpak2.camera.preview;

import android.graphics.Bitmap;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.wolfpakapp.wolfpak2.R;

import java.io.IOException;

/**
 * Layout handler that contains camera preview.  Performs photo capture and video recording
 * Adapted from Google Sample code "android-Camera2Basic" @
 * https://github.com/googlesamples/android-Camera2Basic/blob/master/Application/src/main/java/com/example/android/camera2basic/Camera2BasicFragment.java
 * and Google Sample code "android-Video2Basic" @
 * https://github.com/googlesamples/android-Camera2Video/blob/master/Application/src/main/java/com/example/android/camera2video/Camera2VideoFragment.java
 *
 * @author Roland Fong
 */
public class CameraLayout implements CameraController.CameraActionCallback {

    private static final String TAG = "TAG-CameraLayout";

    /**
     * The Fragment container
     */
    private CameraFragment mFragment;
    private static CameraView mCameraView;
    private CameraController mCameraController;

    private static boolean mLockingForEditor; // true if about to switch to picture editor
    private static boolean mIsRecordingVideo;

    private Button mCaptureButton;
    private ImageButton mSwitchButton;
    private ImageButton mFlashButton;
    private ImageButton mSoundButton;
    private ProgressBar mProgressBar;
    private ImageView mScreenFlash;

    private CountDownTimer mCountDownTimer; // to limit video recording to 10s

    /*THREAD & IMAGE HANDLING*/
    private Handler mTouchHandler;
    private HandlerThread mVideoStarterThread;
    private Handler mVideoStarterHandler;

    private Runnable videoRunner = new Runnable()   {
        @Override
        public void run() {
            if(!mIsRecordingVideo)  {
                Log.d(TAG, "Will record video");
                startRecordingVideo();
            }
        }
    };

    /**
     * Creates the layout.
     * @param fragment the fragment container
     * @param view to locate components
     */
    public CameraLayout(CameraFragment fragment, View view) {
        mFragment = fragment;
        // TODO add code to check phone api
        mCameraController = new CameraController1(fragment.getActivity());
        //mCameraController = new CameraController2(fragment.getActivity());
        mCameraController.setCameraActionCallback(this);

        mCameraView = (CameraView) view.findViewById(R.id.camera_view);
        mCameraView.setStateCallback(mCameraController);

        // set global state defaults
        CameraStates.FLASH_STATE = CameraStates.AUTO_FLASH;
        CameraStates.IS_SOUND = true;
        CameraStates.CAMERA_FACE = CameraStates.BACK;

        // set up buttons
        mCaptureButton = (Button) view.findViewById(R.id.btn_takepicture); // take picture button
        mCaptureButton.setOnTouchListener(fragment);

        mSwitchButton = (ImageButton) view.findViewById(R.id.btn_switch); // switch camera button
        mSwitchButton.setOnClickListener(fragment);

        mFlashButton = (ImageButton) view.findViewById(R.id.btn_flash); // flash button
        mFlashButton.setOnClickListener(fragment);
        mFlashButton.setImageResource(R.drawable.auto_flash);

        mSoundButton = (ImageButton) view.findViewById(R.id.btn_sound); // sound button
        mSoundButton.setOnClickListener(fragment);
        mScreenFlash = (ImageView) view.findViewById(R.id.screen_flash);

        // progress bar and timer for video recording
        mProgressBar = (ProgressBar) view.findViewById(R.id.progress_bar); // progress bar for video
        mCountDownTimer = new CountDownTimer(10000, 10) {
            @Override
            public void onTick(long millisUntilFinished) {
                mProgressBar.setProgress((10000 - (int)millisUntilFinished) / 10);
            }
            @Override
            public void onFinish() {
                mProgressBar.setProgress(1000);
                stopRecordingVideo();
            }
        };
    }

    public void onPause()   {
        mCameraController.closeCamera();
    }

    public void onResume()  {
        View decorView = mFragment.getActivity().getWindow().getDecorView();
        // Hide the status bar.
        decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_FULLSCREEN);

        mLockingForEditor = false;

        if(mCameraView.isAvailable())   {
            try {
                mCameraController.openCamera();
            } catch(RuntimeException e) {
                Log.e(TAG, "Couldn't open camera");
            }
        }
    }

    public void onClick(int id) {
        switch(id)    {
            case R.id.btn_switch:
                mCameraController.toggleCamera();
                break;
            case R.id.btn_flash:
                if(CameraStates.FLASH_STATE == CameraStates.AUTO_FLASH)  {
                    mFlashButton.setImageResource(R.drawable.no_flash);
                } else if (CameraStates.FLASH_STATE == CameraStates.NO_FLASH)  {
                    mFlashButton.setImageResource(R.drawable.flash);
                } else  {
                    mFlashButton.setImageResource(R.drawable.auto_flash);
                }
                mCameraController.toggleFlash();
                break;
            case R.id.btn_sound:
                if(CameraStates.IS_SOUND)  {
                    mSoundButton.setImageResource(R.drawable.no_sound);
                } else  {
                    mSoundButton.setImageResource(R.drawable.sound);
                }
                mCameraController.toggleSound();
                break;
        }
    }

    public boolean onTouch(int id, MotionEvent event) {
        switch(id)   {
            case R.id.btn_takepicture:
                if(event.getAction() == MotionEvent.ACTION_DOWN)   {
                    startTouchHandler();
                    mTouchHandler.postDelayed(videoRunner, 500); // if hold lasts 0.5s, record video
                }
                else if(event.getAction() == MotionEvent.ACTION_UP) {
                    Log.d(TAG, "Take Picture Action Up");
                    // wait for video to finish initing if needed
                    if(mIsRecordingVideo) {
                        Log.d(TAG, "Waiting for thread join");
                        try {
                            mVideoStarterThread.join();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        Log.d(TAG, "Thread joined, stopping");
                        stopVideoStarterThread();
                    }
                    Log.d(TAG, "removing callback");
                    mTouchHandler.removeCallbacks(videoRunner);
                    Log.d(TAG, "Callback removed");
                    stopTouchHandler();
                    if(mIsRecordingVideo)   { // if indeed held for 0.6s, mIsRecordingVideo should be true
                        mCountDownTimer.cancel(); // needed if finished before 10s
                        Log.d(TAG, "Canceled timer");
                        stopRecordingVideo();
                    } else if (!mLockingForEditor)  {
                        // else mIsRecordingVideo is false, so take picture if not going to editor (from video)
                        if(CameraStates.FLASH_STATE != CameraStates.ALWAYS_FLASH)   {
                            startFlashAnimation();
                        }
                        mCameraController.takePicture();
                    }
                }
                break;
        }
        return false; // when set true, the state_pressed won't activate!
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
     * Starts video Recording
     */
    private void startRecordingVideo() {

        startVideoStarterThread();
        mVideoStarterHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    mIsRecordingVideo = true;
                    mCountDownTimer.start();

                    mCameraController.startRecording();

                    Thread.sleep(750); // make sure the video lasts at least 0.75s
                    Log.d(TAG, "1s wait");
                    mVideoStarterThread.quitSafely();
                } catch (InterruptedException e1) {
                    e1.printStackTrace();
                }
                Log.d(TAG, "Video starter finished");
            }
        });
    }

    /**
     * Stops video recording in separate thread to avoid disrupting UI in event of hang
     */
    private void stopRecordingVideo() {
        mIsRecordingVideo = false;
        mLockingForEditor = true; // prevent action_up from accidentally taking picture
        mCameraController.stopRecording();
        Log.d(TAG, "Progress bar set 0");
        mProgressBar.setProgress(0);
        Log.d(TAG, "Start editor");
        startEditor();
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

    public static CameraView getCameraView()   {
        return mCameraView;
    }

    public void startTouchHandler() {
        mTouchHandler = new Handler();
    }

    public void stopTouchHandler()  {
        mTouchHandler = null;
    }

    private void startEditor()   {
        // note camera is closing on onpause
        Log.d(TAG, "Starting editor function");
        mFragment.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mFragment.switchLayouts(CameraStates.GLOBAL_STATE_PREVIEW);
            }
        });
    }

    @Override
    public void onCameraOpened() {
    }

    @Override
    public void onCameraClosed() {
    }

    @Override
    public void onCameraError() {
    }

    @Override
    public void onCaptureCompleted() {
        if(CameraStates.FLASH_STATE == CameraStates.ALWAYS_FLASH)
            startFlashAnimation();
    }

    @Override
    public void onImageAvailable(Bitmap img) {
        mFragment.setImageBitmap(img);
        startEditor();
    }

    @Override
    public void onRecordingCompleted(String path) {
        mFragment.setVideoPath(path);
        startEditor();
    }

    /**
     * Hides all the camera icons
     */
    public void hide()  {
        //Log.d(TAG, "Hiding camera");
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
}
