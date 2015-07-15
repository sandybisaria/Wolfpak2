package com.wolfpakapp.wolfpak2;

import android.app.Activity;
import android.content.Context;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.animation.Interpolator;
import android.view.animation.OvershootInterpolator;

import java.util.ArrayList;

public class LeaderboardTabManager {
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private RecyclerView mRecyclerView;

    private ArrayList<LeaderboardListItem> leaderboardListItems;

    private LeaderboardFragment mParentFragment;
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
                new LeaderboardTabAdapter(leaderboardListItems, this);

        mRecyclerView.setAdapter(leaderboardTabAdapter);

        for (int x = 0; x < 15; x++) {
            leaderboardListItems.add(new LeaderboardListItem(tag, x));
        }

        leaderboardTabAdapter.notifyDataSetChanged();
    }

    public SwipeRefreshLayout getTabLayout() {
        return mSwipeRefreshLayout;
    }

    public RecyclerView getRecyclerView() {
        return mRecyclerView;
    }

    public Activity getParentActivity() {
        return mParentFragment.getActivity();
    }
}
