package com.wolfpakapp.wolfpak2;

import android.app.Activity;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.loopj.android.http.AsyncHttpResponseHandler;

import org.apache.http.Header;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

public class LeaderboardTabManager {
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private RecyclerView mRecyclerView;
    private LeaderboardTabAdapter mTabAdapter;

    private TextView karmaTextView;

    private ArrayList<LeaderboardListItem> leaderboardListItems;

    private LeaderboardFragment mParentFragment;
    private final String tag;

    public LeaderboardTabManager(final String tag, final LeaderboardFragment mParentFragment) {
        this.tag = tag;
        this.mParentFragment = mParentFragment;

        mSwipeRefreshLayout = (SwipeRefreshLayout) LayoutInflater
                .from(mParentFragment.getActivity()).inflate(R.layout.tab_leaderboard, null);

        if (tag.equals(LeaderboardFragment.DEN_TAG)) {
            karmaTextView = (TextView) mSwipeRefreshLayout.findViewById(R.id.leaderboard_den_karma_text_view);
            karmaTextView.setVisibility(View.VISIBLE);
            refreshKarmaCount();
        }

        mRecyclerView = (RecyclerView) mSwipeRefreshLayout.findViewById(R.id.leaderboard_recycler_view);
        mRecyclerView.setHasFixedSize(false);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(mParentFragment.getActivity()));

        mRecyclerView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return mTabAdapter.isItemSelected();
            }
        });
        mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                    toggleSwipeRefreshLayout();
            }
        });

        leaderboardListItems = new ArrayList<>();

        mTabAdapter = new LeaderboardTabAdapter(leaderboardListItems, this);

        mRecyclerView.setAdapter(mTabAdapter);


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
                        mTabAdapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onFailure(int statusCode, Header[] headers, byte[] responseBody,
                                          Throwable error) {
                        Toast.makeText(mParentFragment.getActivity(), "Failed", Toast.LENGTH_SHORT).show();
                        Log.d("Failure", Integer.toString(statusCode));
                    }
                });
        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                ServerRestClient.get(mParentFragment.getRelativeUrl(tag), mParentFragment.getRequestParams(tag),
                        new AsyncHttpResponseHandler() {
                            @Override
                            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                                final JSONArray resArray;
                                leaderboardListItems.clear();
                                try {
                                    resArray = new JSONArray(new String(responseBody));
                                    for (int idx = 0; idx < resArray.length(); idx++) {
                                        leaderboardListItems.add(mParentFragment
                                                .parseListItemJSONObject(tag, resArray.getJSONObject(idx)));
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                                mTabAdapter.notifyDataSetChanged();
                                mSwipeRefreshLayout.setRefreshing(false);
                            }

                            @Override
                            public void onFailure(int statusCode, Header[] headers, byte[] responseBody,
                                                  Throwable error) {
                                Toast.makeText(mParentFragment.getActivity(), "Failed", Toast.LENGTH_SHORT).show();
                                Log.d("Failure", Integer.toString(statusCode));
                                mSwipeRefreshLayout.setRefreshing(false);
                            }
                        });
                if (tag.equals(LeaderboardFragment.DEN_TAG)){
                    refreshKarmaCount();
                }
            }
        });
    }

    public SwipeRefreshLayout getTabLayout() {
        return mSwipeRefreshLayout;
    }

    public void toggleSwipeRefreshLayout() {
        int firstVisiblePos = ((LinearLayoutManager) mRecyclerView.getLayoutManager())
                .findFirstCompletelyVisibleItemPosition();
        mSwipeRefreshLayout.setEnabled(firstVisiblePos == 0);
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

    public String getTag() {
        return tag;
    }

    public boolean isItemSelected() {
        return mTabAdapter.isItemSelected();
    }

    private void refreshKarmaCount() {
        if (!tag.equals(LeaderboardFragment.DEN_TAG)) {
            return;
        }
        ServerRestClient.get("users/", null, new AsyncHttpResponseHandler() {
                    @Override
                    public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                        final JSONArray resArray;
                        try {
                            resArray = new JSONArray(new String(responseBody));
                            for (int idx = 0; idx < resArray.length(); idx++) {
                                JSONObject userObject = resArray.getJSONObject(idx);
                                //TODO Get user ID from device!
                                if (userObject.optString("user_id").equals("temp_test_id")) {
                                    int totalLikes = userObject.getInt("total_likes");
                                    karmaTextView.setText(Integer.toString(totalLikes));
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onFailure(int statusCode, Header[] headers, byte[] responseBody,
                                          Throwable error) {
                        Toast.makeText(mParentFragment.getActivity(), "Failed", Toast.LENGTH_SHORT).show();
                        Log.d("Failure", Integer.toString(statusCode));
                    }
                });
    }
}
