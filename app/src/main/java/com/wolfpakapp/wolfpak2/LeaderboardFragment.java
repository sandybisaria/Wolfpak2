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
import android.widget.TabHost;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class LeaderboardFragment extends Fragment implements TabHost.TabContentFactory {

    final String LOCAL_TAG = "local_leaderboard";
    final String ALL_TIME_TAG = "all_time_leaderboard";
    final String DEN_TAG = "den_board";

    HashMap<String, ArrayList<LeaderboardListItem>> mLeaderboardListMap;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mLeaderboardListMap = new HashMap<>();
        mLeaderboardListMap.put(LOCAL_TAG, new ArrayList<LeaderboardListItem>());
        mLeaderboardListMap.put(ALL_TIME_TAG, new ArrayList<LeaderboardListItem>());
        mLeaderboardListMap.put(DEN_TAG, new ArrayList<LeaderboardListItem>());
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
        final SwipeRefreshLayout swipeRefreshLayout = (SwipeRefreshLayout) LayoutInflater
                .from(getActivity()).inflate(R.layout.tab_leaderboard, null);

        RecyclerView recyclerView = (RecyclerView) swipeRefreshLayout
                .findViewById(R.id.leaderboard_recycler_view);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

        LeaderboardTabAdapter leaderboardTabAdapter =
                new LeaderboardTabAdapter(mLeaderboardListMap.get(tag));

        recyclerView.setAdapter(leaderboardTabAdapter);

        mLeaderboardListMap.get(tag).add(new LeaderboardListItem());

        leaderboardTabAdapter.notifyDataSetChanged();

        return swipeRefreshLayout;
    }
}
