package com.wolfpakapp.wolfpak2;

import android.content.Context;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.MotionEvent;

/**
 * The Wolfpak Pager is an extension of the ViewPager class that is nearly identical except for the
 * onInterceptTouchEvent method. Due to a bug in Android, the ViewPager class sometimes throws
 * NullPointerExceptions. The onInterceptTouchEvent method was overridden to ignore these errors.
 * They should not impact the functionality of the application.
 */
public class WolfpakPager extends ViewPager{
    public WolfpakPager(Context context) {
        super(context);
    }

    public WolfpakPager(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        try {
            return super.onInterceptTouchEvent(ev);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
}
