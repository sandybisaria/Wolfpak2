package com.wolfpakapp.wolfpak2;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TabHost;

import java.util.HashMap;

public class LeaderboardFragment extends Fragment implements TabHost.TabContentFactory {

    final String LOCAL_TAG = "local_leaderboard";
    final String ALL_TIME_TAG = "all_time_leaderboard";
    final String DEN_TAG = "den_board";

    HashMap<String, LeaderboardTabManager> mTabManagerMap;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mTabManagerMap = new HashMap<>();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View baseLayout = inflater.inflate(R.layout.fragment_leaderboard, container, false);

        TabHost tabHost = (TabHost) baseLayout.findViewById(android.R.id.tabhost);
        tabHost.setup();

        tabHost.addTab(tabHost.newTabSpec(LOCAL_TAG).setIndicator("Local").setContent(this));
        tabHost.addTab(tabHost.newTabSpec(ALL_TIME_TAG).setIndicator("All Time").setContent(this));
        tabHost.addTab(tabHost.newTabSpec(DEN_TAG).setIndicator("Den").setContent(this));

        return baseLayout;
    }

    @Override
    public View createTabContent(String tag) {
        LeaderboardTabManager manager = new LeaderboardTabManager(tag, this);

        return manager.getTabLayout();
    }
}
