package com.wolfpakapp.wolfpak2.leaderboard;

import android.app.Activity;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TabHost;

import com.loopj.android.http.RequestParams;
import com.wolfpakapp.wolfpak2.MainActivity;
import com.wolfpakapp.wolfpak2.R;
import com.wolfpakapp.wolfpak2.WolfpakTabHost;

import org.json.JSONObject;

import java.util.HashMap;

/**
 * The LeaderboardFragment represents the leaderboard screen of the Wolfpak application. It
 * implements the TabContentFactory interface so it can set up the layouts for each tab.
 */
public class LeaderboardFragment extends Fragment implements TabHost.TabContentFactory {
    public static final String LOCAL_TAG = "local_leaderboard";
    public static final String ALL_TIME_TAG = "all_time_leaderboard";
    public static final String DEN_TAG = "den_board";

    private HashMap<String, LeaderboardTabManager> mTabManagerMap;
    private HashMap<String, RequestParams> mRequestParamsMap;
    private HashMap<String, String> mRelativeUrlsMap;

    private FrameLayout mBaseFrameLayout;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize the HashMaps that will be used by the fragment.
        mTabManagerMap = new HashMap<>();
        mRequestParamsMap = new HashMap<>();
        setUpRequestParams();
        mRelativeUrlsMap = new HashMap<>();
        setUpRelativeUrls();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View baseLayout = inflater.inflate(R.layout.fragment_leaderboard, container, false);
        mBaseFrameLayout = (FrameLayout) baseLayout.findViewById(R.id.leaderboard_base_frame_layout);

        TabHost tabHost = (TabHost) baseLayout.findViewById(android.R.id.tabhost);
        // REQUIRED for a TabHost loaded with findViewById()
        tabHost.setup();

        // Necessary for the advanced tab behaviors
        ((WolfpakTabHost) tabHost).setParentFragment(this);

        // Set up the tabs
        //TODO If you click on the tab you are on, then scroll to the top of the list?
        tabHost.addTab(tabHost.newTabSpec(LOCAL_TAG).setIndicator("Local").setContent(this));
        tabHost.addTab(tabHost.newTabSpec(ALL_TIME_TAG).setIndicator("All Time").setContent(this));
        tabHost.addTab(tabHost.newTabSpec(DEN_TAG).setIndicator("Den").setContent(this));

        // The leaderboard fragment is a fullscreen fragment. This is necessary so that the expanded
        // content can take up the whole screen (dismissing the status bar in the process).
        getActivity().getWindow().getDecorView()
                .setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                        View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        // Add padding to the top of the layout so that it doesn't sink under the status bar.
        int statusBarPadding = 0;
        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
           statusBarPadding = getResources().getDimensionPixelSize(resourceId);
        }
        tabHost.setPadding(0, statusBarPadding, 0, 0);

        return baseLayout;
    }

    /**
     * Create the content for each tab.
     * @param tag The tag of the requested tab.
     * @return The content (base view) of the tab.
     */
    @Override
    public View createTabContent(String tag) {
        LeaderboardTabManager manager = new LeaderboardTabManager(tag, this);
        mTabManagerMap.put(tag, manager);

        return manager.getTabLayout();
    }

    /**
     * @return The base FrameLayout (with ID tabcontent) that contains the current tab layout.
     */
    public FrameLayout getBaseFrameLayout() {
        return mBaseFrameLayout;
    }

    /**
     * Set up the request parameters that each tab will use to retrieve posts from the server.
     */
    private void setUpRequestParams() {
        RequestParams localParams = new RequestParams();
        RequestParams allTimeParams = new RequestParams();
        RequestParams denParams = new RequestParams();

        String userID = ((MainActivity) getActivity()).getDeviceUUID();
        localParams.add("user_id", "temp_test_id");
        denParams.add("user_id", "temp_test_id");

        Location lastLocation = ((MainActivity) getActivity()).getLastLocation();

        localParams.add("latitude", Double.toString(lastLocation.getLatitude()));
        localParams.add("longitude", Double.toString(lastLocation.getLongitude()));

        localParams.add("is_nsfw", "False");
        allTimeParams.add("is_nsfw", "False");

        mRequestParamsMap.put(LOCAL_TAG, localParams);
        mRequestParamsMap.put(ALL_TIME_TAG, allTimeParams);
        mRequestParamsMap.put(DEN_TAG, denParams);
    }

    /**
     * @param tag The tag of the current tab.
     * @return The request parameters associated with the tab.
     */
    public RequestParams getRequestParams(String tag) {
        return mRequestParamsMap.get(tag);
    }

    /**
     * Set up the relative URLs that each tab will use to retrieve posts from the server.
     */
    private void setUpRelativeUrls() {
        mRelativeUrlsMap.put(LOCAL_TAG, "posts/local_leaderboard/");
        mRelativeUrlsMap.put(ALL_TIME_TAG, "/posts/all_time_leaderboard/");
        mRelativeUrlsMap.put(DEN_TAG, "/users/den/");
    }

    /**
     * @param tag The tag of the current tab.
     * @return The relative URL associated with the tab.
     */
    public String getRelativeUrl(String tag) {
        return mRelativeUrlsMap.get(tag);
    }

    /**
     * Return a LeaderboardListItem object created using the passed JSONObject. The tag determines
     * how to parse the JSONObject and instantiate the LeaderboardListItem.
     * @param tag The tag of the current leaderboard tab.
     * @param jsonObject The JSONObject of the list item.
     * @return A new LeaderboardListItem object.
     */
    public LeaderboardListItem parseListItemJSONObject(String tag, JSONObject jsonObject) {
        int id = jsonObject.optInt("id");
        String handle = jsonObject.optString("handle");
        boolean isImage = jsonObject.optBoolean("is_image");
        String mediaUrl = jsonObject.optString("media_url");
        int originalVoteCount = jsonObject.optInt("likes");
        // "Default" like status is 0
        int likeStatus = 0;
        switch (tag) {
            case LOCAL_TAG: {
                likeStatus = jsonObject.optInt("like_status");
                break;
            }
            case ALL_TIME_TAG: {
                //TODO Get like status using API
                likeStatus = 0;
                break;
            }
            case DEN_TAG: {
                // Since the user can't like his own post, the like status takes on a different
                // meaning...
                //TODO Color determined by vote count?
                likeStatus = 0;
                break;
            }
        }
        return new LeaderboardListItem(id, handle, isImage, mediaUrl, originalVoteCount,
                LeaderboardListItem.VoteStatus.getVoteStatus(likeStatus));
    }

    /**
     * @param tag The tag of the current tab.
     * @return The tab manager associated with the tab.
     */
    public LeaderboardTabManager getTabManager(String tag) {
        return mTabManagerMap.get(tag);
    }
}
