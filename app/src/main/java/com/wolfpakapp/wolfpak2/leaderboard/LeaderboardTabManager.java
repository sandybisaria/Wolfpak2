package com.wolfpakapp.wolfpak2.leaderboard;

import android.app.Activity;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;

import com.loopj.android.http.AsyncHttpResponseHandler;
import com.wolfpakapp.wolfpak2.MainActivity;
import com.wolfpakapp.wolfpak2.Post;
import com.wolfpakapp.wolfpak2.R;
import com.wolfpakapp.wolfpak2.ServerRestClient;

import org.apache.http.Header;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * The LeaderboardTabManager manages the layout for each tab. It is responsible for inflating the
 * layout, setting up the SwipeRefreshLayout, RecyclerView, and adapter, and providing utility
 * methods for the LeaderboardTabAdapter.
 */
public class LeaderboardTabManager {
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private RecyclerView mRecyclerView;
    private LeaderboardTabAdapter mTabAdapter;

    private TextView karmaTextView;

    private ArrayList<Post> mPosts;

    private LeaderboardFragment mParentFragment;
    private final String tag;

    public LeaderboardTabManager(final String tag, final LeaderboardFragment mParentFragment) {
        this.tag = tag;
        this.mParentFragment = mParentFragment;

        mSwipeRefreshLayout = (SwipeRefreshLayout) LayoutInflater
                .from(mParentFragment.getActivity()).inflate(R.layout.tab_leaderboard, null);

        // Set up the karma TextView if this is the den tab.
        if (tag.equals(LeaderboardFragment.DEN_TAG)) {
            karmaTextView = (TextView) mSwipeRefreshLayout.findViewById(R.id.leaderboard_den_karma_text_view);
            karmaTextView.setVisibility(View.VISIBLE);
            refreshKarmaCount();
        }

        mRecyclerView = (RecyclerView) mSwipeRefreshLayout.findViewById(R.id.leaderboard_recycler_view);
        mRecyclerView.setHasFixedSize(false);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(mParentFragment.getActivity()));

        // Ensure that the RecyclerView will not scroll if the user is already touching an item.
        mRecyclerView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return mTabAdapter.isItemSelected();
            }
        });
        // Ensure that the SwipeRefreshLayout will not refresh until the user is scrolled to the top.
        mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                    toggleSwipeRefreshLayout();
            }
        });

        mPosts = new ArrayList<>();

        mTabAdapter = new LeaderboardTabAdapter(mPosts, this);

        mRecyclerView.setAdapter(mTabAdapter);

        // Retrieve the set of posts from the server.
        ServerRestClient.get(mParentFragment.getRelativeUrl(tag), mParentFragment.getRequestParams(tag),
                new AsyncHttpResponseHandler() {
                    @Override
                    public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                        final JSONArray resArray;
                        try {
                            resArray = new JSONArray(new String(responseBody));
                            for (int idx = 0; idx < resArray.length(); idx++) {
                                mPosts.add(Post.parsePostJSONObject(tag, resArray.getJSONObject(idx)));
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        mTabAdapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onFailure(int statusCode, Header[] headers, byte[] responseBody,
                                          Throwable error) {
//                        Toast.makeText(mParentFragment.getActivity(), "Failed", Toast.LENGTH_SHORT).show();
                        Log.d("Failure", Integer.toString(statusCode));
                    }
                });
        // When the SwipeRefreshLayout is refreshed, retrieve a fresh set of posts.
        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                ServerRestClient.get(mParentFragment.getRelativeUrl(tag), mParentFragment.getRequestParams(tag),
                        new AsyncHttpResponseHandler() {
                    @Override
                    public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                        final JSONArray resArray;
                        // The naive approach is to clear the leaderboard of posts and rebuild
                        mPosts.clear();
                        try {
                            resArray = new JSONArray(new String(responseBody));
                            for (int idx = 0; idx < resArray.length(); idx++) {
                                mPosts.add(Post.parsePostJSONObject(tag, resArray.getJSONObject(idx)));
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        mTabAdapter.notifyDataSetChanged();
                        // End the refresh.
                        mSwipeRefreshLayout.setRefreshing(false);
                    }

                    @Override
                    public void onFailure(int statusCode, Header[] headers, byte[] responseBody,
                                          Throwable error) {
//                        Toast.makeText(mParentFragment.getActivity(), "Failed", Toast.LENGTH_SHORT).show();
                        Log.d("Failure", Integer.toString(statusCode));
                        // End the refresh.
                        mSwipeRefreshLayout.setRefreshing(false);
                    }
                });
                if (tag.equals(LeaderboardFragment.DEN_TAG)){
                    // Refresh the karma count on refresh, too.
                    refreshKarmaCount();
                }
            }
        });
    }

    public SwipeRefreshLayout getTabLayout() {
        return mSwipeRefreshLayout;
    }

    /**
     * Toggle the SwipeRefreshLayout based on whether the user has scrolled to the top.
     */
    public void toggleSwipeRefreshLayout() {
        // Get the position of the first completely visible post.
        int firstVisiblePos = ((LinearLayoutManager) mRecyclerView.getLayoutManager())
                .findFirstCompletelyVisibleItemPosition();
        // If the first visible post is not the first actual item (#0), then keep the
        // SwipeRefreshLayout disabled.
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

    /**
     * @return True if an item in the tab was selected.
     */
    public boolean isItemSelected() {
        return mTabAdapter.isItemSelected();
    }

    /**
     * Refresh the karma count. This method does nothing if this is not the den tab.
     */
    private void refreshKarmaCount() {
        if (!tag.equals(LeaderboardFragment.DEN_TAG)) {
            return;
        }
        // Retrieve data on users from the server.
        ServerRestClient.get("users/", null, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                final JSONArray resArray;
                try {
                    resArray = new JSONArray(new String(responseBody));
                    for (int idx = 0; idx < resArray.length(); idx++) {
                        JSONObject userObject = resArray.getJSONObject(idx);
                        //TODO In the "real" app use the device UUID
                        String userID = ((MainActivity) getParentActivity()).getDeviceId();
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
//                Toast.makeText(mParentFragment.getActivity(), "Failed", Toast.LENGTH_SHORT).show();
                Log.d("Failure", Integer.toString(statusCode));
            }
        });
    }
}
