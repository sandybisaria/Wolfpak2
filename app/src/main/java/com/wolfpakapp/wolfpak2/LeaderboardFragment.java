package com.wolfpakapp.wolfpak2;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TabHost;

import java.util.HashMap;

public class LeaderboardFragment extends Fragment implements TabHost.TabContentFactory {
    private final String LOCAL_TAG = "local_leaderboard";
    private final String ALL_TIME_TAG = "all_time_leaderboard";
    private final String DEN_TAG = "den_board";

    private HashMap<String, LeaderboardTabManager> mTabManagerMap;

    private FrameLayout mBaseFrameLayout;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mTabManagerMap = new HashMap<>();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View baseLayout = inflater.inflate(R.layout.fragment_leaderboard, container, false);
        mBaseFrameLayout = (FrameLayout) baseLayout.findViewById(R.id.leaderboard_base_frame_layout);

        TabHost tabHost = (TabHost) baseLayout.findViewById(android.R.id.tabhost);
        tabHost.setup();

        tabHost.addTab(tabHost.newTabSpec(LOCAL_TAG).setIndicator("Local").setContent(this));
        tabHost.addTab(tabHost.newTabSpec(ALL_TIME_TAG).setIndicator("All Time").setContent(this));
        tabHost.addTab(tabHost.newTabSpec(DEN_TAG).setIndicator("Den").setContent(this));

        getActivity().getWindow().getDecorView()
                .setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                        View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        int statusBarPadding = 0;
        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
           statusBarPadding = getResources().getDimensionPixelSize(resourceId);
        }
        tabHost.setPadding(0, statusBarPadding, 0, 0);

        return baseLayout;
    }

    @Override
    public View createTabContent(String tag) {
        LeaderboardTabManager manager = new LeaderboardTabManager(tag, this);
        mTabManagerMap.put(tag, manager);

        return manager.getTabLayout();
    }

    public FrameLayout getBaseFrameLayout() {
        return mBaseFrameLayout;
    }
}
