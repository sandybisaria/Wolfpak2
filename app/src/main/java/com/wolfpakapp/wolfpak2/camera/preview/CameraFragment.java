package com.wolfpakapp.wolfpak2.camera.preview;

import android.graphics.SurfaceTexture;
import android.media.Image;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.MotionEvent;
import android.view.TextureView;
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

    /* CAMERA AND EDITING STATES */
    private static int mGlobalState;
    public static final int GLOBAL_STATE_CAMERA = 0;
    public static final int GLOBAL_STATE_EDITOR = 1;

    /* FILE HANDLING TYPE */
    private static int mFileType;
    public static final int FILE_TYPE_IMAGE = 0;
    public static final int FILE_TYPE_VIDEO = 1;

    /**
     * Image buffer provided directly from camera for editor
     */
    private Image mImage;
    /**
     * Temporary file path for video provided from mediarecorder for editor
     */
    private String mVideoPath;

    private static CameraLayout mCameraLayout;
    private static PictureEditorLayout mPictureEditorLayout;

    private Thread mCameraCloseThread = null;

    private AutoFitTextureView mTextureView;

    /**
     * Handles lifecycle events on {@link TextureView}
     */
    private final TextureView.SurfaceTextureListener mSurfaceTextureListener
            = new TextureView.SurfaceTextureListener()  {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            switch(mGlobalState)    {
                case GLOBAL_STATE_CAMERA:
                    mCameraLayout.onSurfaceTextureAvailable(width, height);
                    break;
            }
        }
        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
            if(mGlobalState == CameraFragment.GLOBAL_STATE_CAMERA)
                mCameraLayout.configureTransform(width, height);
        }
        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            return true;
        }
        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {}
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // clear the temporary directory of lingering files not deleted by videosaverservice
        File root = getActivity().getExternalFilesDir(null);
        File[] Files = root.listFiles();
        if(Files != null) {
            int j;
            for(j = 0; j < Files.length; j++) {
                Log.d(TAG, "DELETING: " + Files[j].getAbsolutePath());
                Files[j].delete();
            }
        }

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

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_camera, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // init global state variables
        mGlobalState = GLOBAL_STATE_CAMERA;
        mFileType = FILE_TYPE_IMAGE;

        // init texture view
        mTextureView = (AutoFitTextureView) view.findViewById(R.id.texture);
        mTextureView.setOnTouchListener(this);

        // init camera layout and picture editor layout
        mCameraLayout = new CameraLayout(this, view);
        mPictureEditorLayout = new PictureEditorLayout(this, view);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        //mImageFile = new File(getActivity().getExternalFilesDir(null), "pic.jpeg");
    }

    public static int getGlobalState() {
        return mGlobalState;
    }

    public static void setGlobalState(int mGlobalState) {
        CameraFragment.mGlobalState = mGlobalState;
    }

    public static int getFileType() {
        return mFileType;
    }

    public static void setFileType(int mFileType) {
        CameraFragment.mFileType = mFileType;
    }

    public Image getImage()   {
        return mImage;
    }

    public void setImage(Image i)   {
        mImage = i;
    }

    public String getVideoPath() {
        return mVideoPath;
    }

    public void setVideoPath(String v)   {
        mVideoPath = v;
    }

    public AutoFitTextureView getTextureView()  {
        return mTextureView;
    }

    public void switchLayouts()  {
        switch(mGlobalState)    {
            case GLOBAL_STATE_CAMERA:
                mGlobalState = GLOBAL_STATE_EDITOR;
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
            case GLOBAL_STATE_EDITOR:
                mGlobalState = GLOBAL_STATE_CAMERA;
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

    @Override
    public void onResume() {
        super.onResume();
        View decorView = getActivity().getWindow().getDecorView();
        // Hide the status bar.
        decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_FULLSCREEN);
        mCameraLayout.onResume();
        if(mGlobalState == GLOBAL_STATE_EDITOR)
            mPictureEditorLayout.onResume();

        if(!mTextureView.isAvailable())
            mTextureView.setSurfaceTextureListener(mSurfaceTextureListener);
    }

    @Override
    public void onPause() {
        mCameraLayout.onPause();
        mPictureEditorLayout.onPause();
        super.onPause();
    }

    @Override
    public void onClick(View v) {
        switch(mGlobalState)    {
            case GLOBAL_STATE_CAMERA:
                mCameraLayout.onClick(v.getId());
                break;
            case GLOBAL_STATE_EDITOR:
                mPictureEditorLayout.onClick(v.getId());
                break;
        }
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        switch(mGlobalState)    {
            case GLOBAL_STATE_CAMERA:
                return mCameraLayout.onTouch(v.getId(), event);
            case GLOBAL_STATE_EDITOR:
                return mPictureEditorLayout.onTouch(v.getId(), event);
        }
        return true;
    }


}
