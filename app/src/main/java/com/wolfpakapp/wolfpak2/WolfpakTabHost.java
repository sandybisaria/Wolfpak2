package com.wolfpakapp.wolfpak2;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.TabHost;

import com.wolfpakapp.wolfpak2.leaderboard.LeaderboardFragment;
import com.wolfpakapp.wolfpak2.leaderboard.LeaderboardTabManager;

/**
 * The WolfpakTabHost is an extension of the TabHost with added behaviors.
 * @see WolfpakTabHost#setCurrentTab(int)
 */
public class WolfpakTabHost extends TabHost{
    private LeaderboardFragment mParentFragment = null;

    public WolfpakTabHost(Context context) {
        super(context);
    }

    public WolfpakTabHost(Context context, AttributeSet attrs) {
        super(context, attrs);
    }


    /**
     * Store a reference to the parent LeaderboardFragment so that added behaviors can be
     * implemented.
     * @param parentFragment The parent LeaderboardFragment responsible for the WolfpakTabHost.
     */
    public void setParentFragment(LeaderboardFragment parentFragment) {
        mParentFragment = parentFragment;
    }

    /**
     * Set the current tab. If the tab of the current page is clicked, then the tab's RecyclerView
     * will scroll to the top. If the tab is animating an element or if an item is selected, do NOT
     * switch tabs.
     * @param index The index of the tab to set.
     * @see android.support.v7.widget.RecyclerView#smoothScrollToPosition(int)
     */
    @Override
    public void setCurrentTab(int index) {
       if (mParentFragment != null) {
           LeaderboardTabManager currentTabManager = mParentFragment
                   .getTabManager(getCurrentTabTag());
           if (currentTabManager == null) {
               // Do nothing because the tab manager was not instantiated yet.
           } else if (currentTabManager.isItemSelected()) {
               // If an item is selected, do not change the tab!
               return;
           } else if (index == getCurrentTab()) {
               // Scroll to the top
               currentTabManager.getRecyclerView().smoothScrollToPosition(0);
           }
        }

        super.setCurrentTab(index);
    }
}
