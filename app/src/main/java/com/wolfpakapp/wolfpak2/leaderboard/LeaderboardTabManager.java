package com.wolfpakapp.wolfpak2.leaderboard;

import android.app.Activity;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewParent;
import android.widget.TextView;

import com.loopj.android.http.AsyncHttpResponseHandler;
import com.wolfpakapp.wolfpak2.Post;
import com.wolfpakapp.wolfpak2.R;
import com.wolfpakapp.wolfpak2.service.ServerRestClient;
import com.wolfpakapp.wolfpak2.WolfpakServiceProvider;
import com.wolfpakapp.wolfpak2.service.UserIdManager;

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

    private ServerRestClient mClient;

    private boolean isItemSelected = false;
    private boolean isNewDrawingOrderSet = false;

    public LeaderboardTabManager(final String tag, final LeaderboardFragment mParentFragment) {
        this.tag = tag;
        this.mParentFragment = mParentFragment;

        mSwipeRefreshLayout = (SwipeRefreshLayout) LayoutInflater
                .from(mParentFragment.getActivity()).inflate(R.layout.tab_leaderboard, null);
        mSwipeRefreshLayout.setColorSchemeColors(getParentActivity().getResources()
                .getColor(R.color.wolfpak_red));

        mRecyclerView = (RecyclerView) mSwipeRefreshLayout.findViewById(R.id.leaderboard_recycler_view);
        mRecyclerView.setHasFixedSize(false);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(mParentFragment.getActivity()));

        // Ensure that the RecyclerView will not scroll if the user is already touching an item.
        mRecyclerView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return isItemSelected;
            }
        });
        // Ensure that the SwipeRefreshLayout will not refresh until the user is scrolled to the top.
        mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                    safeEnableSwipeRefreshLayout();
            }
        });

        mPosts = new ArrayList<>();

        mTabAdapter = new LeaderboardTabAdapter(mPosts, this);

        mRecyclerView.setAdapter(mTabAdapter);

        mClient = (ServerRestClient) WolfpakServiceProvider
                .getServiceManager(WolfpakServiceProvider.SERVERRESTCLIENT);

        // Retrieve the set of posts from the server (and visibly indicate this).
        mSwipeRefreshLayout.post(new Runnable() {
            @Override public void run() {
                mSwipeRefreshLayout.setRefreshing(true);
            }
        });
        mClient.get(mParentFragment.getRelativeUrl(tag), mParentFragment.getRequestParams(tag),
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
                        mSwipeRefreshLayout.setRefreshing(false);
                    }

                    @Override
                    public void onFailure(int statusCode, Header[] headers, byte[] responseBody,
                                          Throwable error) {
                        Log.d("Failure", Integer.toString(statusCode));
                        mSwipeRefreshLayout.setRefreshing(false);
                    }
                });
        // When the SwipeRefreshLayout is refreshed, retrieve a fresh set of posts.
        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                mClient.get(mParentFragment.getRelativeUrl(tag), mParentFragment.getRequestParams(tag),
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
                        mSwipeRefreshLayout.setRefreshing(false);
                    }

                    @Override
                    public void onFailure(int statusCode, Header[] headers, byte[] responseBody,
                                          Throwable error) {
                        Log.d("Failure", Integer.toString(statusCode));
                        mSwipeRefreshLayout.setRefreshing(false);
                    }
                });
                if (tag.equals(LeaderboardFragment.DEN_TAG)){
                    // Refresh the karma count on refresh, too.
                    refreshKarmaCount();
                }
            }
        });

        // Set up the karma TextView if this is the den tab.
        if (tag.equals(LeaderboardFragment.DEN_TAG)) {
            karmaTextView = (TextView) mSwipeRefreshLayout.findViewById(R.id.leaderboard_den_karma_text_view);
            karmaTextView.setVisibility(View.VISIBLE);
            refreshKarmaCount();
        }
    }

    public SwipeRefreshLayout getSwipeRefreshLayout() {
        return mSwipeRefreshLayout;
    }

    /**
     * Enable the SwipeRefreshLayout if the user has scrolled to the top.
     */
    public void safeEnableSwipeRefreshLayout() {
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

    public boolean isItemSelected() {
        return isItemSelected;
    }

    public void setIsItemSelected(boolean isItemSelected) {
        this.isItemSelected = isItemSelected;
    }

    public boolean isNewDrawingOrderSet() {
        return isNewDrawingOrderSet;
    }

    public void setIsNewDrawingOrderSet(boolean isNewDrawingOrderSet) {
        this.isNewDrawingOrderSet = isNewDrawingOrderSet;
    }

    /**
     * Refresh the karma count (on the den tab).
     */
    private void refreshKarmaCount() {
        // Just being cautious...
        if (!tag.equals(LeaderboardFragment.DEN_TAG)) {
            return;
        }
        // Retrieve data on users from the server.
        mClient.get("users/", null, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                final JSONArray resArray;
                try {
                    resArray = new JSONArray(new String(responseBody));
                    for (int idx = 0; idx < resArray.length(); idx++) {
                        JSONObject userObject = resArray.getJSONObject(idx);
                        UserIdManager userIdManager = (UserIdManager) WolfpakServiceProvider
                            .getServiceManager(WolfpakServiceProvider.USERIDMANAGER);
                        String userId = userIdManager.getDeviceId();
                        if (userObject.optString("user_id").equals(userId)) {
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
                Log.d("Failure", Integer.toString(statusCode));
            }
        });
    }

    public ServerRestClient getServerRestClient() {
        return mClient;
    }

    public void requestDisallowInterceptTouchEventForParents(View v,
                                                              boolean disallowIntercept) {
        ViewParent parent = v.getParent();
        while (parent != null) {
            parent.requestDisallowInterceptTouchEvent(disallowIntercept);
            parent = parent.getParent();
        }

    }
}
