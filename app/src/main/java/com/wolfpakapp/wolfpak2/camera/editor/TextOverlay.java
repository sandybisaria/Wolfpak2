package com.wolfpakapp.wolfpak2.camera.editor;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.wolfpakapp.wolfpak2.R;

/**
 * Overlay for drawing text, also manages text states
 *
 * @author Roland Fong
 */
public class TextOverlay extends EditText {

    private final static String TAG = "TAG-TextOverlay";

    private int mState;
    /**
     * Text is hidden from user and cannot be edited
     */
    public static final int TEXT_STATE_HIDDEN = 0;
    /**
     * Text displayed on bar and editable by user.  Allows for
     * vertical movement
     */
    public static final int TEXT_STATE_DEFAULT = 1;
    /**
     * Text is centered on the screen with only vertical movement,
     * but without a bar.  Can enlarge or rotate text.  Tapping on
     * text brings up color picker to change color
     */
    public static final int TEXT_STATE_VERTICAL = 2;
    /**
     * Same as TEXT_STATE_VERTICAL except that text can move anywhere
     * on the screen
     */
    public static final int TEXT_STATE_FREE = 3;

    /**
     * Controls whether textedit can be moved, scaled, and rotated
     */
    private boolean canEdit;

    /**
     * The amount it has rotated.  When in DEFAULT view is not rotated
     */
    private float mRotation;
    /**
     * The amount it has been scaled.  When in DEFAULT view is not scaled
     */
    private float mScale;
    private int mTextColor;
    private float mX;
    private float mY;

    private boolean mIsRotating;
    private boolean mIsScaling;

    /**
     * Original Center Position
     */
    private float centerX;
    private float centerY;

    private Context context = null;

    public TextOverlay(Context context) {
        this(context, null);
    }

    public TextOverlay(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TextOverlay(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.context = context;
    }

    /**
     * Initialize view
     */
    public void init() {
        setState(TEXT_STATE_HIDDEN);
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT);
        // center the view
        params.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
        setLayoutParams(params);
        // bug... these always return 0,0
        int[] coors = new int[2];
        getLocationInWindow(coors);
        centerX = coors[0];
        centerY = coors[1];

        Log.d(TAG, "Text position: " + centerX + ", " + centerY);
        canEdit = false;
        mRotation = 0f;
        mScale = 1f;
        mTextColor = Color.WHITE;

        mIsRotating = false;
        mIsScaling = false;

        this.setOnEditorActionListener(new OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    v.clearFocus(); // clear focus when keyboard is down
                    // force close keyboard
                    InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                }
                return false;
            }
        });

        setDrawingCacheEnabled(true);
    }

    /**
     * @return a bitmap of the text's current appearance
     */
    public Bitmap getBitmap() {
        clearFocus(); // in the event the cursor for some reason is still on there
        Bitmap b = getDrawingCache();
        Matrix rotator = new Matrix();
        rotator.postRotate(getRotation());
        return Bitmap.createBitmap(b, 0, 0, b.getWidth(), b.getHeight(), rotator, true);
    }

    /**
     * Set whether user can edit text overlay
     * If true, user can change, transform, or recolor text
     *
     * @param edit whether or not the text can be edited
     */
    public void setEditable(boolean edit) {
        canEdit = edit;
        if (!edit)   // also hide colorpicker
            PictureEditorLayout.getColorPicker().setVisibility(View.GONE);
    }

    /**
     * @return canEdit
     */
    public boolean isEditable() {
        return canEdit;
    }


    public boolean isRotating() {
        return mIsRotating;
    }

    public void setRotating(boolean mIsRotating) {
        this.mIsRotating = mIsRotating;
    }

    public boolean isScaling() {
        return mIsScaling;
    }

    public void setScaling(boolean mIsScaling) {
        this.mIsScaling = mIsScaling;
    }

    /**
     * Set the text overlay state
     *
     * @param state the state of the text overlay (hidden, default, vertical, free)
     */
    public void setState(int state) {
        switch (state) {
            case TEXT_STATE_HIDDEN:
                PictureEditorLayout.getColorPicker().setVisibility(View.GONE); // hide color picker
                setVisibility(View.INVISIBLE);
                break;
            case TEXT_STATE_DEFAULT:
                setVisibility(View.VISIBLE);
                setCursorVisible(true);
                setFocusableInTouchMode(true); // can edit in default mode
                setRotation(0f); // set horizontal
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 30); // reset text size
                setTextColor(Color.WHITE); // use plain white for default
                requestFocus();
                // resume center position
                setX(0);
                setBackgroundResource(R.drawable.camera_text_bar);
                break;
            case TEXT_STATE_VERTICAL:
                setCursorVisible(false);
                setFocusableInTouchMode(false); // cannot edit when transforming, moving, etc.
                PictureEditorLayout.getColorPicker().setVisibility(View.VISIBLE); // also show color picker
                setRotation(mRotation); // change back to whatever rotation it had
                setTextColor(mTextColor); // restore color
                setBackground(null); // get rid of the bar
                break;
            case TEXT_STATE_FREE:
                break;
        }
        mState = state;
    }

    public void setmRotation(float mRotation) {
        this.mRotation = mRotation;
    }

    public int getmTextColor() {
        return mTextColor;
    }

    public void setmTextColor(int mTextColor) {
        this.mTextColor = mTextColor;
    }

    public float getmScale() {
        return mScale;
    }

    public void setmScale(float mScale) {
        this.mScale = mScale;
    }

    /**
     * @return state
     */
    public int getState() {
        return mState;
    }

    /**
     * Advances the state
     *
     * @return the new state
     */
    public int nextState() {
        switch (mState) {
            case TEXT_STATE_HIDDEN:
                setState(TEXT_STATE_DEFAULT);
                return TEXT_STATE_DEFAULT;
            case TEXT_STATE_DEFAULT:
                setState(TEXT_STATE_VERTICAL);
                return TEXT_STATE_VERTICAL;
            case TEXT_STATE_VERTICAL:
                setState(TEXT_STATE_FREE);
                return TEXT_STATE_FREE;
            case TEXT_STATE_FREE:
                setState(TEXT_STATE_HIDDEN);
                return TEXT_STATE_HIDDEN;
            default:
                return TEXT_STATE_HIDDEN;
        }
    }

    @Override
    protected void onFocusChanged(boolean focused, int direction, Rect previouslyFocusedRect) {
        super.onFocusChanged(focused, direction, previouslyFocusedRect);
        // status bar always seems to come back up, so hide it again
        View decorView = ((Activity) context).getWindow().getDecorView();
        decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_FULLSCREEN);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (canEdit) {
            float mx = event.getRawX();
            float my = event.getRawY();
            clearFocus();
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    break;
                case MotionEvent.ACTION_MOVE:
                    if (mIsRotating || mIsScaling) return false; // don't move text
                    if (mState == TEXT_STATE_DEFAULT || mState == TEXT_STATE_VERTICAL) {
                        mY = my - getHeight() / 2;
                        setY(mY); // only move in Y
                    } else if (mState == TEXT_STATE_FREE) {// move in any direction
                        mX = mx - getWidth() / 2;
                        mY = my - getHeight() / 2;
                        setX(mX);
                        setY(mY);
                    }
                    break;
                case MotionEvent.ACTION_UP:
                    Log.d(TAG, "New Pos: " + mX + ", " + mY);
                    break;
            }
        }
        return super.onTouchEvent(event);
    }
}
