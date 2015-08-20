package com.wolfpakapp.wolfpak2.camera.editor;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.TextureView;
import android.view.View;

/**
 * An overlay for drawing above textureview
 * @author Roland Fong
 */
public class EditableOverlay extends View {

    private static final String TAG = "TAG-EditableOverlay";

    private TextureView mTextureView;

    private Bitmap mBitmap;
    private Canvas mCanvas;
    private Path mPath;
    private Paint mBitmapPaint;
    private Paint mPaint;
    private int mColor;

    private int mState;
    public static final int STATE_IDLE = 0;
    public static final int STATE_DRAW = 1;
    public static final int STATE_BLUR = 2;
    public static final int STATE_TEXT = 3;

    private float mX, mY;
    private static final float TOUCH_TOLERANCE = 4;

    private Handler mDrawHandler; // performs the actual blitting path onto textureview
    private static final int BLITTING_OVERLAY = 1;
    private boolean mTouched;

    private TextOverlay mTextOverlay;
    private ScaleGestureDetector mScaleDetector;
    private float currentFontSize;
    private final ScaleGestureDetector.OnScaleGestureListener mOnScaleListener =
            new ScaleGestureDetector.OnScaleGestureListener() {
                @Override
                public boolean onScaleBegin(ScaleGestureDetector detector) {
                    Log.d(TAG, "Scale Begin");
                    currentFontSize = mTextOverlay.getTextSize();
                    return true;
                }

                @Override
                public boolean onScale(ScaleGestureDetector detector) {
                    Log.d(TAG, "Scale Detected");
                    if(mTextOverlay.getState() == TextOverlay.TEXT_STATE_FREE ||
                            mTextOverlay.getState() == TextOverlay.TEXT_STATE_VERTICAL) {
                        Log.d(TAG, "Scaling by " + detector.getScaleFactor());
                        mTextOverlay.setTextSize(TypedValue.COMPLEX_UNIT_PX, currentFontSize * detector.getScaleFactor());
                        mTextOverlay.setmScale(mTextOverlay.getmScale() * detector.getScaleFactor());
                        mTextOverlay.invalidate();
                    }
                    return false;
                }

                @Override
                public void onScaleEnd(ScaleGestureDetector detector) {

                }
            };

    private RotationGestureDetector mRotationDetector;
    private RotationGestureDetector.OnRotationGestureListener mOnRotationListener = new RotationGestureDetector.OnRotationGestureListener() {
        @Override
        public void OnRotation(RotationGestureDetector rotationDetector) {
            Log.d(TAG, "Rotation by angle " + rotationDetector.getAngle());

            if(mTextOverlay.getState() == TextOverlay.TEXT_STATE_FREE ||
                    mTextOverlay.getState() == TextOverlay.TEXT_STATE_VERTICAL) {
                mTextOverlay.setPivotX(mTextOverlay.getWidth() / 2);
                mTextOverlay.setPivotY(mTextOverlay.getHeight() / 2);
                mTextOverlay.setRotation(-1 * rotationDetector.getAngle());
                mTextOverlay.setmRotation(mTextOverlay.getRotation());
            }
        }
    };

    public EditableOverlay(Context context)  {
        this(context, null);
    }

    public EditableOverlay(Context context, AttributeSet attrs)  {
        this(context, attrs, 0);
    }

    public EditableOverlay(Context context, AttributeSet attrs, int defStyle)    {
        super(context, attrs, defStyle);
    }

    /**
     * Initializes the paint components for the overlay
     */
    public void init(TextureView textureView, TextOverlay textOverlay) {
        Log.d(TAG, "initing overlay");
        setSaveEnabled(true);
        mState = STATE_IDLE;

        mPath = new Path();
        mBitmapPaint = new Paint(Paint.DITHER_FLAG);

        mColor = 0xFF000000;

        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setDither(true);
        mPaint.setColor(mColor);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeJoin(Paint.Join.ROUND);
        mPaint.setStrokeCap(Paint.Cap.ROUND);
        mPaint.setStrokeWidth(9);

        mTextOverlay = textOverlay;
        mTextOverlay.init();

        mTextureView = textureView;

        mDrawHandler = new Handler();
        mTouched = false;

        mScaleDetector = new ScaleGestureDetector(getContext(), mOnScaleListener);
        mRotationDetector = new RotationGestureDetector(mOnRotationListener);
    }

    public void setBitmap(Bitmap b) {
        mBitmap = Bitmap.createBitmap(b);
        mCanvas = new Canvas(mBitmap);
        invalidate();
    }

    /**
     * @return the overlay bitmap with text
     */
    public Bitmap getBitmap()
    {
        Bitmap b = Bitmap.createBitmap(mBitmap);
        if(mTextOverlay.getState() != TextOverlay.TEXT_STATE_HIDDEN) {
            Canvas c = new Canvas(b);
            c.drawBitmap(mTextOverlay.getBitmap(), mTextOverlay.getX(), mTextOverlay.getY(), null);
        }
        return b;
    }

    /**
     * @return the overlay bitmap without text
     */
    public Bitmap getBitmapWithoutText()    {
        return mBitmap;
    }

    /**
     * Sets the state of the overlay
     * @param state
     */
    public void setState(int state)  {
        mState = state;
    }

    /**
     * @return the state of the overlay
     */
    public int getState()   {
        return mState;
    }

    /**
     * Sets the color of drawing tool
     * @param color
     */
    public void setColor(int color)   {
        mColor = color;
        mPaint.setColor(color);
    }

    /**
     * @return the drawing tool color
     */
    public int getColor() {
        return mColor;
    }

    public TextOverlay getTextOverlay() {
        return mTextOverlay;
    }


    public void clearBitmap()   {
        mBitmap.eraseColor(Color.argb(0, 0, 0, 0));
    }

    private void touch_start(float x, float y, int state) {
        switch(state)   {
            case STATE_DRAW:
                mPath.reset();
                mPath.moveTo(x, y);
                mX = x;
                mY = y;
                break;
        }
    }

    private void touch_move(float x, float y, int state) {
        switch(state)   {
            case STATE_DRAW:
                float dx = Math.abs(x - mX);
                float dy = Math.abs(y - mY);
                if (dx >= TOUCH_TOLERANCE || dy >= TOUCH_TOLERANCE) {
                    mPath.quadTo(mX, mY, (x + mX) / 2, (y + mY) / 2);
                    mX = x;
                    mY = y;
                }
                break;
        }
    }

    private void touch_up(int state) {
        switch(state)   {
            case STATE_DRAW:
                mPath.lineTo(mX, mY);
                // commit the path to our offscreen
                mCanvas.drawPath(mPath, mPaint);
                // kill this so we don't double draw
                mPath.reset();
                if (PictureEditorLayout.isImage()) {
                    blitOverlay();
                } else { // if not image, only save overlay
                    UndoManager.addScreenState(Bitmap.createBitmap(mBitmap));
                }
                break;
        }
    }

    /**
     * Blits the overlay onto the image and saves an undo state instance
     */
    private void blitOverlay()  {
        mDrawHandler.post(new Runnable()    {
            @Override
            public void run() {
                // if image, combine overlay and textureview onto textureview surface
                Canvas canvas = mTextureView.lockCanvas();
                // this should be what the screen currently looks like; getBitmap is too slow
                Bitmap txbmp = UndoManager.getLastScreenState();
                final Bitmap b = Bitmap.createBitmap(txbmp);
                final Canvas canvas2 = new Canvas(b); // for saving undo state
                canvas.drawBitmap(txbmp, 0, 0, null);
                canvas.drawBitmap(mBitmap, 0, 0, null);
                mTextureView.unlockCanvasAndPost(canvas);
                canvas2.drawBitmap(mBitmap, 0, 0, null);
                UndoManager.addScreenState(b); // save state
                clearBitmap();
            }
        });
        mDrawHandler.sendEmptyMessage(BLITTING_OVERLAY);
    }

    private boolean performTouchEvent(MotionEvent event, int state) {
        if(state == STATE_BLUR) return false;// blur is not handled on overlay
        float x = event.getX();
        float y = event.getY();
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mTouched = true; // finger detected; this may not happen if mDrawHandler didn't finish
                touch_start(x, y, state);
                break;
            case MotionEvent.ACTION_MOVE:
                if(mTouched) // if finger has been detected, continue drawing
                    touch_move(x, y, state);
                break;
            case MotionEvent.ACTION_UP:
                if(mTouched) {// if finger started drawing, finish drawing
                    mTouched = false;
                    touch_up(state);
                }
                break;
        }
        invalidate();
        return true;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if(mState == STATE_IDLE) return false;// don't even do anything

        mScaleDetector.onTouchEvent(event);
        mRotationDetector.onTouchEvent(event);

        float x = event.getX();
        float y = event.getY();

        if(!mDrawHandler.hasMessages(BLITTING_OVERLAY)) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                case MotionEvent.ACTION_MOVE:
                case MotionEvent.ACTION_UP:
                    return performTouchEvent(event, mState);
            }
        }
        return false; // touch event not consumed - pass this on to textureview
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawBitmap(mBitmap, 0, 0, mBitmapPaint);

        canvas.drawPath(mPath, mPaint);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        System.out.println("w "+w+" h "+h+" oldw "+oldw+" oldh "+oldh);
        mBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        mCanvas = new Canvas(mBitmap);
    }

    public static class RotationGestureDetector {
        private static final int INVALID_POINTER_ID = -1;
        private float fX, fY, sX, sY;
        private int ptrID1, ptrID2;
        private float mAngle;

        private OnRotationGestureListener mListener;

        public float getAngle() {
            return mAngle;
        }

        public RotationGestureDetector(OnRotationGestureListener listener){
            mListener = listener;
            ptrID1 = INVALID_POINTER_ID;
            ptrID2 = INVALID_POINTER_ID;
        }

        public boolean onTouchEvent(MotionEvent event){
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    ptrID1 = event.getPointerId(event.getActionIndex());
                    break;
                case MotionEvent.ACTION_POINTER_DOWN:
                    ptrID2 = event.getPointerId(event.getActionIndex());
                    sX = event.getX(event.findPointerIndex(ptrID1));
                    sY = event.getY(event.findPointerIndex(ptrID1));
                    fX = event.getX(event.findPointerIndex(ptrID2));
                    fY = event.getY(event.findPointerIndex(ptrID2));
                    break;
                case MotionEvent.ACTION_MOVE:
                    if(ptrID1 != INVALID_POINTER_ID && ptrID2 != INVALID_POINTER_ID){
                        float nfX, nfY, nsX, nsY;
                        nsX = event.getX(event.findPointerIndex(ptrID1));
                        nsY = event.getY(event.findPointerIndex(ptrID1));
                        nfX = event.getX(event.findPointerIndex(ptrID2));
                        nfY = event.getY(event.findPointerIndex(ptrID2));

                        mAngle = angleBetweenLines(fX, fY, sX, sY, nfX, nfY, nsX, nsY);

                        if (mListener != null) {
                            mListener.OnRotation(this);
                        }
                    }
                    break;
                case MotionEvent.ACTION_UP:
                    ptrID1 = INVALID_POINTER_ID;
                    break;
                case MotionEvent.ACTION_POINTER_UP:
                    ptrID2 = INVALID_POINTER_ID;
                    break;
                case MotionEvent.ACTION_CANCEL:
                    ptrID1 = INVALID_POINTER_ID;
                    ptrID2 = INVALID_POINTER_ID;
                    break;
            }
            return true;
        }

        private float angleBetweenLines (float fX, float fY, float sX, float sY, float nfX, float nfY, float nsX, float nsY)
        {
            float angle1 = (float) Math.atan2( (fY - sY), (fX - sX) );
            float angle2 = (float) Math.atan2( (nfY - nsY), (nfX - nsX) );

            float angle = ((float)Math.toDegrees(angle1 - angle2)) % 360;
            if (angle < -180.f) angle += 360.0f;
            if (angle > 180.f) angle -= 360.0f;
            return angle;
        }

        public static interface OnRotationGestureListener {
            public void OnRotation(RotationGestureDetector rotationDetector);
        }
    }
}
