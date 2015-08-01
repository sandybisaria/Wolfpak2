package com.wolfpakapp.wolfpak2.camera.editor;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraCharacteristics;
import android.media.Image;
import android.media.MediaPlayer;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;
import android.util.Log;
import android.view.MotionEvent;
import android.view.TextureView;
import android.view.View;
import android.widget.ImageButton;
import android.widget.VideoView;

import com.wolfpakapp.wolfpak2.R;
import com.wolfpakapp.wolfpak2.camera.editor.colorpicker.ColorPickerView;
import com.wolfpakapp.wolfpak2.camera.preview.CameraFragment;
import com.wolfpakapp.wolfpak2.camera.preview.CameraLayout;

import java.nio.ByteBuffer;

/**
 * A fragment that displays a captured image or loops video for the user to edit
 * @author Roland Fong
 */
public class PictureEditorLayout {

    private static final String TAG = "TAG-PictureEditorLayout";

    /**
     * The Fragment container
     */
    private CameraFragment mFragment;
    private static boolean isImage;

    private boolean mIsTextureReady = false;

    private static String mVideoPath;

    private MediaSaver mMediaSaver;

    // for blurring
    private static final int BLUR_RADIUS = 20;
    private static final int BLUR_SIDE = 100;
    private RenderScript mBlurScript = null;
    private ScriptIntrinsicBlur mIntrinsicScript = null;
    private Bitmap mTextureBitmap = null;
    private Bitmap mBlurredBitmap = null;
    private Canvas blurCanvas = null;

    // buttons
    private ImageButton mBackButton;
    private ImageButton mDownloadButton;
    private ImageButton mUploadButton;
    private ImageButton mUndoButton;
    private ImageButton mTextButton;
    private ImageButton mBlurButton;
    private ImageButton mDrawButton;

    private VideoView mVideoView;

    private EditableOverlay mOverlay;
    private static ColorPickerView mColorPicker;

    private TextureView mTextureView;

    /**
     * Handles lifecycle events on {@link TextureView}
     */
    private final TextureView.SurfaceTextureListener mSurfaceTextureListener
            = new TextureView.SurfaceTextureListener()  {
                @Override
                public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
                    Log.d(TAG, "Edit Texture Avail");
                    mIsTextureReady = true;
                    mTextureView.setTransform(new Matrix());
                    displayMedia();
                }

                @Override
                public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {}

                @Override
                public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                    return true;
                }

                @Override
                public void onSurfaceTextureUpdated(SurfaceTexture surface) {}
            };

    public PictureEditorLayout(CameraFragment fragment, View view) {
        // set up texture view
        mFragment = fragment;
        mTextureView = (TextureView) view.findViewById(R.id.edit_texture);
        mTextureView.setSurfaceTextureListener(mSurfaceTextureListener);
        // set up color picker
        mColorPicker = (ColorPickerView)
                view.findViewById(R.id.color_picker_view);
        mColorPicker.setOnColorChangedListener(new ColorPickerView.OnColorChangedListener() {

            @Override
            public void onColorChanged(int newColor) {
                if (mOverlay.getState() == EditableOverlay.STATE_DRAW) {
                    mDrawButton.setBackgroundColor(newColor);
                    mOverlay.setColor(newColor);
                } else if (mOverlay.getState() == EditableOverlay.STATE_TEXT) {
                    mOverlay.getTextOverlay().setTextColor(newColor);
                    mOverlay.getTextOverlay().setmTextColor(newColor);
                }
            }
        });
        mColorPicker.setVisibility(View.GONE);
        // set up bitmap overlay (drawing and text)
        mOverlay = (EditableOverlay) view.findViewById(R.id.overlay);
        mOverlay.init(mTextureView, (TextOverlay) view.findViewById(R.id.text_overlay));
        // init buttons
        mBackButton = (ImageButton) view.findViewById(R.id.btn_back);
        mDownloadButton = (ImageButton) view.findViewById(R.id.btn_download);
        mUploadButton = (ImageButton) view.findViewById(R.id.btn_upload);
        mUndoButton = (ImageButton) view.findViewById(R.id.btn_undo);
        mTextButton = (ImageButton) view.findViewById(R.id.btn_text);
        mBlurButton = (ImageButton) view.findViewById(R.id.btn_blur);
        mDrawButton = (ImageButton) view.findViewById(R.id.btn_draw);
        // add button click listeners
        mBackButton.setOnClickListener(fragment);
        mDownloadButton.setOnClickListener(fragment);
        mUploadButton.setOnClickListener(fragment);
        mUndoButton.setOnClickListener(fragment);
        mTextButton.setOnClickListener(fragment);
        mBlurButton.setOnClickListener(fragment);
        mDrawButton.setOnClickListener(fragment);

        // set up blurring scripts
        mBlurScript = RenderScript.create(fragment.getActivity());
        mIntrinsicScript = ScriptIntrinsicBlur.create(mBlurScript, Element.U8_4(mBlurScript));

        mMediaSaver = new MediaSaver(fragment.getActivity(), mOverlay, mTextureView);
        mMediaSaver.setMediaSaverListener(new MediaSaver.MediaSaverListener() {
            @Override
            public void onDownloadCompleted() {
                if(!isImage) {
                    mVideoView.resume();
                    Log.d(TAG, "Resuming Video");
                }
            }

            @Override
            public void onUploadCompleted() {
                // restart preview process
                UndoManager.clearStates();
                startCamera();
            }
        });

        mVideoView = (VideoView) view.findViewById(R.id.video_player);
    }

    /**
     * @return if image is being handled
     */
    public static boolean isImage()    {
        return isImage;
    }

    /**
     * @return mVideoPath the path of the video
     */
    public static String getVideoPath() {
        return mVideoPath;
    }

    /**
     * Displays media onto textureview
     */
    private void displayMedia() {
        Log.d(TAG, "FILE TYPE: " + mFragment.getFileType());
        if(mFragment.getFileType() == CameraFragment.FILE_TYPE_IMAGE) {
            isImage = true;
        } else if(mFragment.getFileType() == CameraFragment.FILE_TYPE_VIDEO)    {
            isImage = false;
        }

        if(isImage) {
            if(mFragment.getImage() != null) {
                Canvas canvas = mTextureView.lockCanvas();

                Image img = mFragment.getImage();
                int width = img.getWidth();
                int height = img.getHeight();

                // put image info from camera into buffer
                ByteBuffer buffer = img.getPlanes()[0].getBuffer();
                byte[] bytes = new byte[buffer.remaining()];
                buffer.get(bytes);
                Bitmap src = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                mFragment.getImage().close(); // don't forget to close the image buffer!
                mFragment.setImage(null); // so we know to skip initing image upon resume
                // resize horizontally oriented images
                if (width > height) {
                    // transformation matrix that scales and rotates
                    Matrix matrix = new Matrix();
                    if(CameraLayout.getFace() == CameraCharacteristics.LENS_FACING_FRONT)  {
                        matrix.setScale(-1, 1);
                    }
                    matrix.postRotate(90);
                    /*matrix.postScale(((float) canvas.getWidth()) / src.getHeight(),
                            ((float) canvas.getHeight()) / src.getWidth());*/
                    Bitmap resizedBitmap = Bitmap.createBitmap(
                            src, 0, 0, width, height, matrix, true);
                    canvas.drawBitmap(resizedBitmap, 0, 0, null);
                    UndoManager.addScreenState(resizedBitmap); // initial state
                }

                mTextureView.unlockCanvasAndPost(canvas);
                Log.d(TAG, "Pic posted should be visible");
            } else { // device likely resumed,  so restore previous session
                Canvas c = mTextureView.lockCanvas();
                c.drawBitmap(UndoManager.getLastScreenState(), 0, 0, null);
                mTextureView.unlockCanvasAndPost(c);
            }
        } else  {
            if(mFragment.getVideoPath() != null) {
                mVideoPath = mFragment.getVideoPath();
                try {
                    UndoManager.addScreenState(Bitmap.createBitmap(mOverlay.getBitmap()));// initial state
                } catch(NullPointerException e) {
                    e.printStackTrace(); // screw that... just let the user wonder why he can't undo to first move
                }
                mFragment.setVideoPath(null); // so we know to skip initing upon resume
            } else  { // device likely resumed, so restore previous session
                mOverlay.setBitmap(UndoManager.getLastScreenState());
            }
            if(CameraLayout.getFace() == CameraCharacteristics.LENS_FACING_FRONT)   {
                Matrix matrix = new Matrix();
                matrix.setScale(1, -1, 0, mTextureView.getHeight() / 2);
                mTextureView.setTransform(matrix);
            }
            // play the video
            try {
                Log.d(TAG, "Playing Video");
                mVideoView.setVideoPath(mVideoPath);
                mVideoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                    @Override
                    public void onPrepared(MediaPlayer mp) {
                        mp.setLooping(true);
                    }
                });
                mVideoView.start();

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * @return the ColorPickerView
     */
    public static ColorPickerView getColorPicker()  {
        return mColorPicker;
    }

    /**
     * Sets the bitmap on TextureView
     * @param bitmap
     */
    public void setBitmap(Bitmap bitmap)    {
    }

    /**
     * Takes a square bitmap and turns it into a circle
     * @param bitmap
     * @return
     */
    public static Bitmap getRoundedCornerBitmap(Bitmap bitmap) {
        Bitmap output = Bitmap.createBitmap(bitmap.getWidth(),
                bitmap.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);

        final int color = 0xff424242;
        final Paint paint = new Paint();
        final Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
        final RectF rectF = new RectF(rect);
        final float roundPx = BLUR_SIDE / 2;

        paint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);
        paint.setColor(color);
        canvas.drawRoundRect(rectF, roundPx, roundPx, paint);

        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(bitmap, rect, rect, paint);

        return output;
    }

    /**
     * Blurs image at specified coordinates by producing a circular bitmap and blitting it
     * to the textureview
     * @param action
     * @param x
     * @param y
     */
    private void blurImage(int action, float x, float y)    {
        switch(action)  {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_MOVE:
                blurCanvas = mTextureView.lockCanvas();
                mTextureBitmap = mTextureView.getBitmap();
                // Blur bitmap
                mBlurredBitmap = Bitmap.createBitmap(BLUR_SIDE, BLUR_SIDE, mTextureBitmap.getConfig());

                // prevent errors when blur rectangle exceeds bounds
                if((int) x - BLUR_SIDE / 2 <= 0)
                    x = BLUR_SIDE / 2;
                if((int) y - BLUR_SIDE / 2 <= 0)
                    y = BLUR_SIDE / 2;

                if((int) x + BLUR_SIDE > mTextureBitmap.getWidth())
                    x = mTextureBitmap.getWidth() - BLUR_SIDE / 2;
                if((int) y + BLUR_SIDE > mTextureBitmap.getHeight())
                    y = mTextureBitmap.getHeight() - BLUR_SIDE / 2;

                final Bitmap blurSource = Bitmap.createBitmap(mTextureBitmap,
                        (int) x - BLUR_SIDE / 2, (int) y - BLUR_SIDE / 2, BLUR_SIDE, BLUR_SIDE);

                final Allocation inAlloc = Allocation.createFromBitmap(mBlurScript,
                        blurSource, Allocation.MipmapControl.MIPMAP_NONE, Allocation.USAGE_GRAPHICS_TEXTURE);
                final Allocation outAlloc = Allocation.createFromBitmap(mBlurScript, mBlurredBitmap);

                mIntrinsicScript.setRadius(BLUR_RADIUS);
                mIntrinsicScript.setInput(inAlloc);
                mIntrinsicScript.forEach(outAlloc);
                outAlloc.copyTo(mBlurredBitmap);

                // first draw what's on textureview, then draw blur
                blurCanvas.drawBitmap(mTextureBitmap, 0, 0, null);
                blurCanvas.drawBitmap(getRoundedCornerBitmap(mBlurredBitmap), (int) x - BLUR_SIDE / 2, (int)y - BLUR_SIDE / 2, null);
                mTextureView.unlockCanvasAndPost(blurCanvas);
                break;
            case MotionEvent.ACTION_UP:
                Bitmap screen = Bitmap.createBitmap(mTextureView.getBitmap());
                Canvas c = new Canvas(screen);
                c.drawBitmap(mOverlay.getBitmapWithoutText(), 0, 0, null);
                UndoManager.addScreenState(Bitmap.createBitmap(screen));
                break;
            case MotionEvent.ACTION_CANCEL:
            default: break;
        }

    }

    public void onSurfaceTextureAvailable() {
        mTextureView.setTransform(new Matrix());
        displayMedia();
    }

    public void onPause() {
        if(!isImage) {
            mVideoView.pause();
        }
    }

    public void onResume()  {
        if(!isImage)    {
            mVideoView.resume();
        }
    }

    public void onClick(int id) {
        switch(id) {
            case R.id.btn_back:
                UndoManager.clearStates();
                startCamera();
                break;
            case R.id.btn_download:
                if(!isImage)
                    mVideoView.pause();
                mMediaSaver.downloadMedia();
                break;
            case R.id.btn_upload:
                if(!isImage)
                    mVideoView.pause();
                mMediaSaver.uploadMedia();
                break;
            case R.id.btn_undo:
                if(UndoManager.getNumberOfStates() > 1) {
                    if (isImage) {
                        Canvas c = mTextureView.lockCanvas();
                        c.drawBitmap(UndoManager.undoScreenState(), 0, 0, null);
                        mTextureView.unlockCanvasAndPost(c);
                        mOverlay.clearBitmap();
                    } else {
                        mOverlay.setBitmap(UndoManager.undoScreenState());
                    }
                }
                break;
            case R.id.btn_draw:
                if(mOverlay.getState() == EditableOverlay.STATE_TEXT)    {
                    mOverlay.getTextOverlay().setEditable(false);
                    mOverlay.getTextOverlay().setEnabled(false);
                    mOverlay.getTextOverlay().clearFocus();
                }
                if(mOverlay.getState() != EditableOverlay.STATE_DRAW) {
                    mOverlay.setState(EditableOverlay.STATE_DRAW);
                    mOverlay.setColor(mColorPicker.getColor());
                    mColorPicker.setVisibility(View.VISIBLE);
                    mDrawButton.setBackgroundColor(mOverlay.getColor());
                } else {
                    mOverlay.setState(EditableOverlay.STATE_IDLE);
                    mColorPicker.setVisibility(View.GONE);
                    mDrawButton.setBackgroundColor(0x00000000);
                }
                break;
            case R.id.btn_blur:
                if(isImage) {// don't enable blurring for video
                    if(mOverlay.getState() == EditableOverlay.STATE_BLUR) {
                        mOverlay.setState(EditableOverlay.STATE_IDLE);
                        break;
                    } else if(mOverlay.getState() == EditableOverlay.STATE_DRAW)   {
                        mColorPicker.setVisibility(View.GONE);
                        mDrawButton.setBackgroundColor(0x00000000);
                    } else if(mOverlay.getState() == EditableOverlay.STATE_TEXT)    {
                        mOverlay.getTextOverlay().setEditable(false);
                        mOverlay.getTextOverlay().setEnabled(false);
                        mOverlay.getTextOverlay().clearFocus();
                    }
                    mOverlay.setState(EditableOverlay.STATE_BLUR);
                }
                break;
            case R.id.btn_text:
                Log.d(TAG, "Clicked on text");
                if(mOverlay.getState() == EditableOverlay.STATE_DRAW)   {
                    mColorPicker.setVisibility(View.GONE);
                    mDrawButton.setBackgroundColor(0x00000000);
                }
                if(mOverlay.getState() != EditableOverlay.STATE_TEXT) {
                    mOverlay.setState(EditableOverlay.STATE_TEXT);
                    mOverlay.getTextOverlay().setEditable(true);
                    mOverlay.getTextOverlay().setEnabled(true);
                    mOverlay.getTextOverlay().requestFocus();
                    mOverlay.getTextOverlay().nextState();// go to default state
                } else  {// if text is selected
                    // goes to next state and ends text editing session if text hidden
                    if(mOverlay.getTextOverlay().nextState() == TextOverlay.TEXT_STATE_HIDDEN)  {
                        mOverlay.setState(EditableOverlay.STATE_IDLE);
                    }
                }
                break;
        }
    }

    public boolean onTouch(int id, MotionEvent event) {
        switch(id)   {
            case R.id.texture:
                Log.d(TAG, "Action Down");
                if(mOverlay.getState() == EditableOverlay.STATE_BLUR) {
                    blurImage(event.getAction(), event.getX(), event.getY());
                }
                break;
        }
        return true;
    }

    public void startCamera()   {
        mFragment.switchLayouts();
    }

    /**
     * Hides all the editor icons
     */
    public void hide()  {
        mFragment.getActivity().runOnUiThread(new Runnable() {
              @Override
              public void run() {
                  mVideoView.stopPlayback();
                  mVideoView.suspend();
                  mOverlay.setVisibility(View.GONE);
                  mOverlay.getTextOverlay().setVisibility(View.GONE);
                  mColorPicker.setVisibility(View.GONE);
                  mVideoView.setVisibility(View.GONE);
                  mTextureView.setVisibility(View.INVISIBLE);
                  mUndoButton.setVisibility(View.GONE);
                  mTextButton.setVisibility(View.GONE);
                  mBlurButton.setVisibility(View.GONE);
                  mDrawButton.setBackgroundColor(Color.argb(0, 0, 0, 0));
                  mDrawButton.setVisibility(View.GONE);
                  mBackButton.setVisibility(View.GONE);
                  mDownloadButton.setVisibility(View.GONE);
                  mUploadButton.setVisibility(View.GONE);
              }
          });
    }

    /**
     * Shows all the editor icons
     */
    public void show()  {
        mFragment.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "Showing Editor");
                mOverlay.setVisibility(View.VISIBLE);
                mUndoButton.setVisibility(View.VISIBLE);
                mTextButton.setVisibility(View.VISIBLE);
                mBlurButton.setVisibility(View.VISIBLE);
                mDrawButton.setVisibility(View.VISIBLE);
                mBackButton.setVisibility(View.VISIBLE);
                mDownloadButton.setVisibility(View.VISIBLE);
                mUploadButton.setVisibility(View.VISIBLE);

                if(mFragment.getFileType() == CameraFragment.FILE_TYPE_IMAGE) {
                    Log.d(TAG, "Showing TextureView");
                    mTextureView.setVisibility(View.VISIBLE);
                    if (mIsTextureReady) {
                        onSurfaceTextureAvailable();
                    }
                }
                else {
                    Log.d(TAG, "Showing VidView");
                    mVideoView.setVisibility(View.VISIBLE);
                    displayMedia();
                }
            }
        });
    }
}
