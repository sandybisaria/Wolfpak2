package com.wolfpakapp.wolfpak2;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.TabHost;

public class WolfpakTabHost extends TabHost{
    private LeaderboardFragment mParentFragment;

    public WolfpakTabHost(Context context) {
        super(context);
    }

    public WolfpakTabHost(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setParentFragment(LeaderboardFragment mParentFragment) {
        this.mParentFragment = mParentFragment;
    }

    @Override
    public void setCurrentTab(int index) {
        if (index == getCurrentTab()) {
            mParentFragment.scrollToTop(getCurrentTabTag());
        }
        super.setCurrentTab(index);
    }
}
