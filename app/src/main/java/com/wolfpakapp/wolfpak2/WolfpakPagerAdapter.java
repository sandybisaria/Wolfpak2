package com.wolfpakapp.wolfpak2;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

public class WolfpakPagerAdapter extends FragmentStatePagerAdapter {

    public WolfpakPagerAdapter(FragmentManager fm) {
        super(fm);
    }

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
