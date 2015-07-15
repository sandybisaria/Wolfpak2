package com.wolfpakapp.wolfpak2;

import android.app.Activity;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;

import java.util.ArrayList;

public class LeaderboardTabManager {
    SwipeRefreshLayout mSwipeRefreshLayout;
    RecyclerView mRecyclerView;

    ArrayList<LeaderboardListItem> leaderboardListItems;

    LeaderboardFragment mParentFragment;
    final String tag;

    public LeaderboardTabManager(String tag, LeaderboardFragment mParentFragment) {
        this.tag = tag;
        this.mParentFragment = mParentFragment;

        mSwipeRefreshLayout = (SwipeRefreshLayout) LayoutInflater
                .from(mParentFragment.getActivity()).inflate(R.layout.tab_leaderboard, null);

        mRecyclerView = (RecyclerView) mSwipeRefreshLayout.findViewById(R.id.leaderboard_recycler_view);
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(mParentFragment.getActivity()));

        leaderboardListItems = new ArrayList<>();

        LeaderboardTabAdapter leaderboardTabAdapter =
                new LeaderboardTabAdapter(leaderboardListItems);

        mRecyclerView.setAdapter(leaderboardTabAdapter);

        leaderboardListItems.add(new LeaderboardListItem(tag));

        leaderboardTabAdapter.notifyDataSetChanged();
    }

    public SwipeRefreshLayout getTabLayout() {
        return mSwipeRefreshLayout;
    }
}
