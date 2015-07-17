package com.wolfpakapp.wolfpak2;

import android.app.Activity;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.widget.Toast;

import com.loopj.android.http.AsyncHttpResponseHandler;

import org.apache.http.Header;
import org.json.JSONArray;

import java.util.ArrayList;

public class LeaderboardTabManager {
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private RecyclerView mRecyclerView;

    private ArrayList<LeaderboardListItem> leaderboardListItems;

    private LeaderboardFragment mParentFragment;
    final String tag;

    public LeaderboardTabManager(final String tag, final LeaderboardFragment mParentFragment) {
        this.tag = tag;
        this.mParentFragment = mParentFragment;

        mSwipeRefreshLayout = (SwipeRefreshLayout) LayoutInflater
                .from(mParentFragment.getActivity()).inflate(R.layout.tab_leaderboard, null);

        mRecyclerView = (RecyclerView) mSwipeRefreshLayout.findViewById(R.id.leaderboard_recycler_view);
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(mParentFragment.getActivity()));

        leaderboardListItems = new ArrayList<>();

        final LeaderboardTabAdapter leaderboardTabAdapter =
                new LeaderboardTabAdapter(leaderboardListItems, this);

        mRecyclerView.setAdapter(leaderboardTabAdapter);

        ServerRestClient.get(mParentFragment.getRelativeUrl(tag), mParentFragment.getRequestParams(tag),
                new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                final JSONArray resArray;
                try {
                    resArray = new JSONArray(new String(responseBody));
                    for (int idx = 0; idx < resArray.length(); idx++) {
                        leaderboardListItems.add(mParentFragment
                                .parseListItemJSONObject(tag, resArray.getJSONObject(idx)));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                leaderboardTabAdapter.notifyDataSetChanged();
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody,
                                  Throwable error) {
                Toast.makeText(mParentFragment.getActivity(), "Failed", Toast.LENGTH_SHORT).show();
                Log.d("Failure", Integer.toString(statusCode));
            }
        });
    }

    public SwipeRefreshLayout getTabLayout() {
        return mSwipeRefreshLayout;
    }

    public RecyclerView getRecyclerView() {
        return mRecyclerView;
    }

    public LeaderboardFragment getParentFragment() {
        return mParentFragment;
    }

    public Activity getParentActivity() {
        return mParentFragment.getActivity();
    }
}
