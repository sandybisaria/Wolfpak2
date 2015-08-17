package com.wolfpakapp.wolfpak2.camera.preview;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.MotionEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.github.hiteshsondhi88.libffmpeg.FFmpeg;
import com.github.hiteshsondhi88.libffmpeg.LoadBinaryResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegNotSupportedException;
import com.wolfpakapp.wolfpak2.WolfpakPager;
import com.wolfpakapp.wolfpak2.camera.editor.MediaSaver;
import com.wolfpakapp.wolfpak2.camera.editor.PictureEditorLayout;
import com.wolfpakapp.wolfpak2.R;

import java.io.File;

/**
 * A fragment container housing both camera and editor user interfaces
 * @author Roland Fong
 */
public class CameraFragment extends Fragment
        implements View.OnClickListener, View.OnTouchListener {

    private static final String TAG = "TAG-CameraFragment";

    private static CameraLayout mCameraLayout;
    private static PictureEditorLayout mPictureEditorLayout;

    private String mVideoPath;
    private Bitmap mImageBitmap;

    private Thread mCameraCloseThread = null;

    /**
     * {@inheritDoc}
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        deleteTemporaryFiles();

        MediaSaver.setActivity(getActivity());
        Log.d(TAG, "initing FFMPEG");
        // init FFmpeg
        MediaSaver.setFfmpeg(FFmpeg.getInstance(getActivity()));
        try {
            MediaSaver.getFfmpeg().loadBinary(new LoadBinaryResponseHandler() {
                @Override
                public void onStart() {}
                @Override
                public void onFailure() {}
                @Override
                public void onSuccess() {}
                @Override
                public void onFinish() {}
            });
        } catch (FFmpegNotSupportedException e) {
            // Handle if FFmpeg is not supported by device
        }
    }

    /**
     * Clear the temporary files just in case {@link com.wolfpakapp.wolfpak2.camera.editor.VideoSavingService}
     * failed to delete them (e.g. service was force stopped)
     */
    private void deleteTemporaryFiles() {
        File root = getActivity().getExternalFilesDir(null);
        File[] Files = root.listFiles();
        if (Files != null) {
            int j;
            for (j = 0; j < Files.length; j++) {
                Log.d(TAG, "DELETING: " + Files[j].getAbsolutePath());
                Files[j].delete();
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_camera, container, false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // init global state variables
        CameraStates.CAMERA_GLOBAL_STATE = CameraStates.GLOBAL_STATE_PREVIEW;
        CameraStates.FILE_TYPE = CameraStates.FILE_IMAGE;

        // init camera layout and picture editor layout
        mCameraLayout = new CameraLayout(this, view);
        mPictureEditorLayout = new PictureEditorLayout(this, view);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    /**
     * Switches the layouts
     * @param state the current state
     */
    public void switchLayouts(int state)  {
        switch(state)    {
            case CameraStates.GLOBAL_STATE_PREVIEW:
                CameraStates.CAMERA_GLOBAL_STATE = CameraStates.GLOBAL_STATE_EDITOR;
                WolfpakPager.setActive(false);
                Log.d(TAG, "Hiding camera, showing editor");
                mCameraLayout.hide();
                mCameraCloseThread = new Thread(new Runnable()  {
                    @Override
                    public void run() {
                        Log.d(TAG, "Pausing the camera");
                        mCameraLayout.onPause(); // takes time... run off UI thread
                        Log.d(TAG, "Finished pausing the camera");
                    }
                });
                mCameraCloseThread.start();
                mPictureEditorLayout.show();
                break;
            case CameraStates.GLOBAL_STATE_EDITOR:
                CameraStates.CAMERA_GLOBAL_STATE = CameraStates.GLOBAL_STATE_PREVIEW;
                WolfpakPager.setActive(true);
                Log.d(TAG, "Hiding Editor, showing camera");
                mPictureEditorLayout.hide();
                if(mCameraCloseThread != null) {
                    try {
                        Log.d(TAG, "Waiting for camera to finish pausing");
                        mCameraCloseThread.join(2000); // wait (up to 2s) until camera finishes closing
                        Log.d(TAG, "Will show layout");
                    } catch(InterruptedException e) {
                        e.printStackTrace();
                    } finally {
                        mCameraCloseThread = null;
                    }
                }
                mCameraLayout.show();
                mCameraLayout.onResume();
                break;
        }
    }

    /**
     * @return the path where video is saved
     */
    public String getVideoPath() {
        return mVideoPath;
    }

    /**
     * @param path the path where video is saved
     */
    public void setVideoPath(String path) {
        mVideoPath = path;
    }

    /**
     * @return the decoded image bitmap
     */
    public Bitmap getImageBitmap() {
        return mImageBitmap;
    }

    /**
     * @param bmp the image bitmap
     */
    public void setImageBitmap(Bitmap bmp) {
        mImageBitmap = bmp;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onResume() {
        super.onResume();
        View decorView = getActivity().getWindow().getDecorView();
        // Hide the status bar.
        decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_FULLSCREEN);
        mCameraLayout.onResume();
        if(CameraStates.CAMERA_GLOBAL_STATE == CameraStates.GLOBAL_STATE_EDITOR)
            mPictureEditorLayout.onResume();

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onPause() {
        mCameraLayout.onPause();
        mPictureEditorLayout.onPause();
        super.onPause();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onClick(View v) {
        switch(CameraStates.CAMERA_GLOBAL_STATE)    {
            case CameraStates.GLOBAL_STATE_PREVIEW:
                mCameraLayout.onClick(v.getId());
                break;
            case CameraStates.GLOBAL_STATE_EDITOR:
                mPictureEditorLayout.onClick(v.getId());
                break;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        switch(CameraStates.CAMERA_GLOBAL_STATE)    {
            case CameraStates.GLOBAL_STATE_PREVIEW:
                return mCameraLayout.onTouch(v.getId(), event);
            case CameraStates.GLOBAL_STATE_EDITOR:
                return mPictureEditorLayout.onTouch(v.getId(), event);
        }
        return true;
    }

}
