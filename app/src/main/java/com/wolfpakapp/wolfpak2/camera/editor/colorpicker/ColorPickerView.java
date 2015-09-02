/*
 * Copyright (C) 2015 Daniel Nilsson
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * 
 * 

 * Change Log:
 * 
 * 1.1
 * - Fixed buggy measure and layout code. You can now make the view any size you want.
 * - Optimization of the drawing using a bitmap cache, a lot faster!
 * - Support for hardware acceleration for all but the problematic
 *	 part of the view will still be software rendered but much faster!
 *   See comment in drawSatValPanel() for more info.
 * - Support for declaring some variables in xml.
 * 
 * 1.2 - 2015-05-08
 * - More bugs in onMeasure() have been fixed, should handle all cases properly now.
 * - View automatically saves its state now.
 * - Automatic border color depending on current theme.
 * - Code cleanup, trackball support removed since they do not exist anymore.
 * 
 * 1.3 - 2015-05-10
 * - Fixed hue bar selection did not align with what was shown in the sat/val panel.
 *   Fixed by replacing the linear gardient used before. Now drawing individual lines
 *   of different colors. This was expensive so we now use a bitmap cache for the hue
 *   panel too.
 * - Replaced all RectF used in the layout process with Rect since the
 *   floating point values was causing layout issues (perfect alignment).
 */
package com.wolfpakapp.wolfpak2.camera.editor.colorpicker;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;

import com.wolfpakapp.wolfpak2.R;

/**
 * Displays a color picker to the user and allow them
 * to select a color. A slider for the alpha channel is
 * also available. Enable it by setting
 * setAlphaSliderVisible(boolean) to true.
 *
 * @author Daniel Nilsson
 */
public class ColorPickerView extends View {

    public interface OnColorChangedListener {
        void onColorChanged(int newColor);
    }

    private final static int DEFAULT_BORDER_COLOR = 0xFFFFFF;
    private final static int DEFAULT_SLIDER_COLOR = 0xFFBDBDBD;

    private final static int HUE_PANEL_WDITH_DP = 30;
    private final static int SLIDER_TRACKER_SIZE_DP = 4;
    private final static int SLIDER_TRACKER_OFFSET_DP = 2;

    /**
     * The width in pixels of the border
     * surrounding all color panels.
     */
    private final static int BORDER_WIDTH_PX = 10;

    /**
     * The width in px of the hue panel.
     */
    private int mHuePanelWidthPx;
    /**
     * The px which the tracker of the hue or alpha panel
     * will extend outside of its bounds.
     */
    private int mSliderTrackerOffsetPx;
    /**
     * Height of slider tracker on hue panel,
     * width of slider on alpha panel.
     */
    private int mSliderTrackerSizePx;

    private Paint mHueAlphaTrackerPaint;

    private Paint mBorderPaint;

    /* We cache the hue background to since its also very expensive now. */
    private BitmapCache mHueBackgroundCache;

    /* Current values */
    private float mHue = 360f;

    private int mSliderTrackerColor = DEFAULT_SLIDER_COLOR;
    private int mBorderColor = DEFAULT_BORDER_COLOR;

    /**
     * Minimum required padding. The offset from the
     * edge we must have or else the finger tracker will
     * get clipped when it's drawn outside of the view.
     */
    private int mRequiredPadding;

    /**
     * The Rect in which we are allowed to draw.
     * Trackers can extend outside slightly,
     * due to the required padding we have set.
     */
    private Rect mDrawingRect;

    private Rect mHueRect;

    private Point mStartTouchPoint = null;

    private OnColorChangedListener mListener;

    public ColorPickerView(Context context) {
        this(context, null);
    }

    public ColorPickerView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ColorPickerView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context, attrs);
    }


    @Override
    public Parcelable onSaveInstanceState() {

        Bundle state = new Bundle();
        state.putParcelable("instanceState", super.onSaveInstanceState());
        state.putFloat("hue", mHue);

        return state;
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {

        if (state instanceof Bundle) {
            Bundle bundle = (Bundle) state;

            mHue = bundle.getFloat("hue");


            state = bundle.getParcelable("instanceState");
        }
        super.onRestoreInstanceState(state);
    }


    private void init(Context context, AttributeSet attrs) {
        //Load those if set in xml resource file.
        TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.ColorPickerView);
        mSliderTrackerColor = a.getColor(R.styleable.ColorPickerView_sliderColor, 0xFFEDEDED);
        mBorderColor = a.getColor(R.styleable.ColorPickerView_borderColor, 0xFFEEEEEE);
        a.recycle();

        applyThemeColors(context);


        mHuePanelWidthPx = DrawingUtils.dpToPx(getContext(), HUE_PANEL_WDITH_DP);
        mSliderTrackerSizePx = DrawingUtils.dpToPx(getContext(), SLIDER_TRACKER_SIZE_DP);
        mSliderTrackerOffsetPx = DrawingUtils.dpToPx(getContext(), SLIDER_TRACKER_OFFSET_DP);

        mRequiredPadding = getResources().getDimensionPixelSize(R.dimen.color_picker_view_required_padding);

        initPaintTools();

        //Needed for receiving trackball motion events.
        setFocusable(true);
        setFocusableInTouchMode(true);
    }

    private void applyThemeColors(Context c) {
        // If no specific border/slider color has been
        // set we take the default secondary text color
        // as border/slider color. Thus it will adopt
        // to theme changes automatically.

        final TypedValue value = new TypedValue();
        TypedArray a = c.obtainStyledAttributes(value.data, new int[]{android.R.attr.textColorSecondary});

        if (mBorderColor == DEFAULT_BORDER_COLOR) {
            mBorderColor = a.getColor(0, DEFAULT_BORDER_COLOR);
        }

        if (mSliderTrackerColor == DEFAULT_SLIDER_COLOR) {
            mSliderTrackerColor = a.getColor(0, DEFAULT_SLIDER_COLOR);
        }

        a.recycle();
    }

    private void initPaintTools() {

        mHueAlphaTrackerPaint = new Paint();
        mBorderPaint = new Paint();

        mHueAlphaTrackerPaint.setColor(mSliderTrackerColor);
        mHueAlphaTrackerPaint.setStyle(Style.STROKE);
        mHueAlphaTrackerPaint.setStrokeWidth(DrawingUtils.dpToPx(getContext(), 2));
        mHueAlphaTrackerPaint.setAntiAlias(true);

    }


    @Override
    protected void onDraw(Canvas canvas) {
        if (mDrawingRect.width() <= 0 || mDrawingRect.height() <= 0) {
            return;
        }
        drawHuePanel(canvas);
    }

    private void drawHuePanel(Canvas canvas) {
        final Rect rect = mHueRect;

        if (BORDER_WIDTH_PX > 0) {
            mBorderPaint.setColor(mBorderColor);

            canvas.drawRoundRect(new RectF(rect.left - BORDER_WIDTH_PX,
                            rect.top - BORDER_WIDTH_PX,
                            rect.right + BORDER_WIDTH_PX,
                            rect.bottom + BORDER_WIDTH_PX),
                    DrawingUtils.dpToPx(getContext(), HUE_PANEL_WDITH_DP / 2),
                    DrawingUtils.dpToPx(getContext(), HUE_PANEL_WDITH_DP / 2),
                    mBorderPaint);
//			canvas.drawRect(rect.left - BORDER_WIDTH_PX,
//					rect.top - BORDER_WIDTH_PX,
//					rect.right + BORDER_WIDTH_PX,
//					rect.bottom + BORDER_WIDTH_PX,
//					mBorderPaint);
        }


        if (mHueBackgroundCache == null) {
            mHueBackgroundCache = new BitmapCache();
            mHueBackgroundCache.bitmap =
                    Bitmap.createBitmap(rect.width(), rect.height(), Config.ARGB_8888);
            mHueBackgroundCache.canvas = new Canvas(mHueBackgroundCache.bitmap);


            int[] hueColors = new int[(int) (rect.height() + 0.5f)];

            // Generate array of all colors, will be drawn as individual lines.
            float h = 360f;
            for (int i = 0; i < hueColors.length; i++) {
                hueColors[i] = Color.HSVToColor(new float[]{h, 1f, 1f});
                h -= 360f / hueColors.length;
            }

            // Time to draw the hue color gradient,
            // its drawn as individual lines which
            // will be quite many when the resolution is high
            // and/or the panel is large.
            Paint linePaint = new Paint();
            linePaint.setStrokeWidth(0);
            for (int i = 0; i < hueColors.length; i++) {
                linePaint.setColor(hueColors[i]);
                mHueBackgroundCache.canvas.drawLine(0, i, mHueBackgroundCache.bitmap.getWidth(), i, linePaint);
            }
        }


        canvas.drawBitmap(getRoundedCornerBitmap(mHueBackgroundCache.bitmap,
                DrawingUtils.dpToPx(getContext(), HUE_PANEL_WDITH_DP / 2)), null, rect, null);

        // we don't need the slider tracker if we are to copy the iOS version of the app
//		Point p = hueToPoint(mHue);
//
//		RectF r = new RectF();
//		r.left = rect.left - mSliderTrackerOffsetPx;
//		r.right = rect.right + mSliderTrackerOffsetPx;
//		r.top = p.y - (mSliderTrackerSizePx / 2);
//		r.bottom = p.y + (mSliderTrackerSizePx / 2);
//
//		canvas.drawRoundRect(r, 2, 2, mHueAlphaTrackerPaint);
    }

    //TODO this is a repeat from in PictureEditorLayout, maybe put in DrawingUtils?
    public static Bitmap getRoundedCornerBitmap(Bitmap bitmap, int pixels) {
        Bitmap output = Bitmap.createBitmap(bitmap.getWidth(), bitmap
                .getHeight(), Config.ARGB_8888);
        Canvas canvas = new Canvas(output);

        final int color = 0xff424242;
        final Paint paint = new Paint();
        final Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
        final RectF rectF = new RectF(rect);
        final float roundPx = pixels;

        paint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);
        paint.setColor(color);
        canvas.drawRoundRect(rectF, roundPx, roundPx, paint);

        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(bitmap, rect, rect, paint);

        return output;
    }

    private Point hueToPoint(float hue) {

        final Rect rect = mHueRect;
        final float height = rect.height();

        Point p = new Point();

        p.y = (int) (height - (hue * height / 360f) + rect.top);
        p.x = rect.left;

        return p;
    }

    private float pointToHue(float y) {

        final Rect rect = mHueRect;

        float height = rect.height();

        if (y < rect.top) {
            y = 0f;
        } else if (y > rect.bottom) {
            y = height;
        } else {
            y = y - rect.top;
        }


        float hue = 360f - (y * 360f / height);
        Log.d("color-picker-view", "Hue: " + hue);

        return hue;
    }


    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        boolean update = false;

        switch (event.getAction()) {

            case MotionEvent.ACTION_DOWN:
                mStartTouchPoint = new Point((int) event.getX(), (int) event.getY());
                update = moveTrackersIfNeeded(event);
                break;
            case MotionEvent.ACTION_MOVE:
                update = moveTrackersIfNeeded(event);
                break;
            case MotionEvent.ACTION_UP:
                mStartTouchPoint = null;
                update = moveTrackersIfNeeded(event);
                break;
        }

        if (update) {
            if (mListener != null) {
                mListener.onColorChanged(Color.HSVToColor(new float[]{mHue, 1f, 1f}));
            }
            invalidate();
            return true;
        }

        return super.onTouchEvent(event);
    }

    private boolean moveTrackersIfNeeded(MotionEvent event) {
        if (mStartTouchPoint == null) {
            return false;
        }

        boolean update = false;

        int startX = mStartTouchPoint.x;
        int startY = mStartTouchPoint.y;

        if (mHueRect.contains(startX, startY)) {
            mHue = pointToHue(event.getY());

            update = true;
        }

        return update;
    }


    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int finalWidth = 0;
        int finalHeight = 0;

        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);

        int widthAllowed = MeasureSpec.getSize(widthMeasureSpec) - getPaddingLeft() - getPaddingRight();
        int heightAllowed = (MeasureSpec.getSize(heightMeasureSpec) - getPaddingBottom() - getPaddingTop()) / 4;
        // quarter of allowed height

        //Log.d("color-picker-view", "widthMode: " + modeToString(widthMode) + " heightMode: " + modeToString(heightMode) + " widthAllowed: " + widthAllowed + " heightAllowed: " + heightAllowed);

        if (widthMode == MeasureSpec.EXACTLY || heightMode == MeasureSpec.EXACTLY) {
            //A exact value has been set in either direction, we need to stay within this size.

            if (widthMode == MeasureSpec.EXACTLY && heightMode != MeasureSpec.EXACTLY) {
                //The with has been specified exactly, we need to adopt the height to fit.
                int h = widthAllowed - mHuePanelWidthPx;

                if (h > heightAllowed) {
                    //We can't fit the view in this container, set the size to whatever was allowed.
                    finalHeight = heightAllowed;
                } else {
                    finalHeight = h;
                }

                finalWidth = widthAllowed;

            } else if (heightMode == MeasureSpec.EXACTLY && widthMode != MeasureSpec.EXACTLY) {
                //The height has been specified exactly, we need to stay within this height and adopt the width.

                int w = heightAllowed + mHuePanelWidthPx;

                if (w > widthAllowed) {
                    //we can't fit within this container, set the size to whatever was allowed.
                    finalWidth = widthAllowed;
                } else {
                    finalWidth = w;
                }

                finalHeight = heightAllowed;

            } else {
                //If we get here the dev has set the width and height to exact sizes. For example match_parent or 300dp.
                //This will mean that the sat/val panel will not be square but it doesn't matter. It will work anyway.
                //In all other senarios our goal is to make that panel square.

                //We set the sizes to exactly what we were told.
                finalWidth = widthAllowed;
                finalHeight = heightAllowed;
            }

        } else {
            //If no exact size has been set we try to make our view as big as possible
            //within the allowed space.

            //Calculate the needed width to layout using max allowed height.
            int widthNeeded = heightAllowed + mHuePanelWidthPx;

            //Calculate the needed height to layout using max allowed width.
            int heightNeeded = widthAllowed - mHuePanelWidthPx;

            boolean widthOk = false;
            boolean heightOk = false;

            if (widthNeeded <= widthAllowed) {
                widthOk = true;
            }

            if (heightNeeded <= heightAllowed) {
                heightOk = true;
            }


            //Log.d("color-picker-view", "Size - Allowed w: " + widthAllowed + " h: " + heightAllowed + " Needed w:" + widthNeeded + " h: " + heightNeeded);


            if (widthOk && heightOk) {
                finalWidth = widthAllowed;
                finalHeight = heightNeeded;
            } else if (!heightOk && widthOk) {
                finalHeight = heightAllowed;
                finalWidth = widthNeeded;
            } else if (!widthOk && heightOk) {
                finalHeight = heightNeeded;
                finalWidth = widthAllowed;
            } else {
                finalHeight = heightAllowed;
                finalWidth = widthAllowed;
            }

        }

        //Log.d("color-picker-view", "Final Size: " + finalWidth + "x" + finalHeight);

        setMeasuredDimension(finalWidth + getPaddingLeft() + getPaddingRight(),
                finalHeight + getPaddingTop() + getPaddingBottom());
    }

    private int getPreferredWidth() {
        //Our preferred width and height is 200dp for the square sat / val rectangle.
        int width = DrawingUtils.dpToPx(getContext(), 200);

        return width + mHuePanelWidthPx;
    }

    private int getPreferredHeight() {
        int height = DrawingUtils.dpToPx(getContext(), 200);

        return height;
    }

    @Override
    public int getPaddingTop() {
        return Math.max(super.getPaddingTop(), mRequiredPadding);
    }

    @Override
    public int getPaddingBottom() {
        return Math.max(super.getPaddingBottom(), mRequiredPadding);
    }

    @Override
    public int getPaddingLeft() {
        return Math.max(super.getPaddingLeft(), mRequiredPadding);
    }

    @Override
    public int getPaddingRight() {
        return Math.max(super.getPaddingRight(), mRequiredPadding);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        mDrawingRect = new Rect();
        mDrawingRect.left = getPaddingLeft();
        mDrawingRect.right = w - getPaddingRight();
        mDrawingRect.top = getPaddingTop();
        mDrawingRect.bottom = h - getPaddingBottom();

        // Clear those bitmap caches since the size may have changed.
        mHueBackgroundCache = null;

        //Log.d("color-picker-view", "Size: " + w + "x" + h);

        setUpHueRect();
    }

    private void setUpHueRect() {
        //Calculate the size for the hue slider on the left.
        final Rect dRect = mDrawingRect;

        int left = dRect.right - mHuePanelWidthPx + BORDER_WIDTH_PX;
        int top = dRect.top + BORDER_WIDTH_PX;
        int bottom = dRect.bottom - BORDER_WIDTH_PX;
        int right = dRect.right - BORDER_WIDTH_PX;

        mHueRect = new Rect(left, top, right, bottom);
    }

    /**
     * Set a OnColorChangedListener to get notified when the color
     * selected by the user has changed.
     *
     * @param listener
     */
    public void setOnColorChangedListener(OnColorChangedListener listener) {
        mListener = listener;
    }

    /**
     * Get the current color this view is showing.
     *
     * @return the current color.
     */
    public int getColor() {
        return Color.HSVToColor(new float[]{mHue, 1f, 1f});
    }

    /**
     * Set the color the view should show.
     *
     * @param color The color that should be selected. #argb
     */
    public void setColor(int color) {
        setColor(color, false);
    }

    /**
     * Set the color this view should show.
     *
     * @param color    The color that should be selected. #argb
     * @param callback If you want to get a callback to
     *                 your OnColorChangedListener.
     */
    public void setColor(int color, boolean callback) {

        int red = Color.red(color);
        int blue = Color.blue(color);
        int green = Color.green(color);

        float[] hsv = new float[3];

        Color.RGBToHSV(red, green, blue, hsv);

        mHue = hsv[0];

        if (callback && mListener != null) {
            mListener.onColorChanged(Color.HSVToColor(new float[]{mHue, 1f, 1f}));
        }

        invalidate();
    }

    /**
     * Set the color of the tracker slider on the hue and alpha panel.
     *
     * @param color
     */
    public void setSliderTrackerColor(int color) {
        mSliderTrackerColor = color;
        mHueAlphaTrackerPaint.setColor(mSliderTrackerColor);
        invalidate();
    }

    /**
     * Get color of the tracker slider on the hue and alpha panel.
     *
     * @return
     */
    public int getSliderTrackerColor() {
        return mSliderTrackerColor;
    }

    /**
     * Set the color of the border surrounding all panels.
     *
     * @param color
     */
    public void setBorderColor(int color) {
        mBorderColor = color;
        invalidate();
    }

    /**
     * Get the color of the border surrounding all panels.
     */
    public int getBorderColor() {
        return mBorderColor;
    }

    private class BitmapCache {
        public Canvas canvas;
        public Bitmap bitmap;
        public float value;
    }

}
