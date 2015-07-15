package com.wolfpakapp.wolfpak2;

import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TabHost;

import java.util.ArrayList;

public class LeaderboardFragment extends Fragment implements TabHost.TabContentFactory {

    final String LOCAL_TAG = "local_leaderboard";
    final String ALL_TIME_TAG = "all_time_leaderboard";
    final String DEN_TAG = "den_board";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
        final SwipeRefreshLayout swipeRefreshLayout = (SwipeRefreshLayout) LayoutInflater.from(getActivity())
                .inflate(R.layout.tab_leaderboard, null);

        RecyclerView recyclerView = (RecyclerView) swipeRefreshLayout
                .findViewById(R.id.leaderboard_recycler_view);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

        return swipeRefreshLayout;
    }
}
