package com.wolfpakapp.wolfpak2;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

import com.wolfpakapp.wolfpak2.leaderboard.LeaderboardFragment;

/**
 * The WolfpakPagerAdapter is an extension of the FragmentStatePagerAdapter suited for use in the
 * Wolfpak application.
 * @see WolfpakPagerAdapter#getItem(int)
 */
public class WolfpakPagerAdapter extends FragmentStatePagerAdapter {

    public WolfpakPagerAdapter(FragmentManager fm) {
        super(fm);
    }

    /**
     * Return the Fragment associated with the index. Currently, the Fragments that can be returned
     * are:
     * <ul>
     *     <li>0 - {@link LeaderboardFragment}</li>
     * </ul>
     * @param i The index of the Fragment to get.
     * @return The requested Fragment.
     */
    @Override
    public Fragment getItem(int i) {
        switch (i) {
            case 0: {
                return new LeaderboardFragment();
            }
        }

        return null;
    }

    @Override
    public int getCount() {
        return MainActivity.getNumPages();
    }
}
