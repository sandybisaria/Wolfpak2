package com.wolfpakapp.wolfpak2.leaderboard;

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
import com.wolfpakapp.wolfpak2.WolfpakServiceProvider;
import com.wolfpakapp.wolfpak2.WolfpakTabHost;
import com.wolfpakapp.wolfpak2.service.UserIdManager;

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
        tabHost.addTab(tabHost.newTabSpec(LOCAL_TAG).setIndicator("Local").setContent(this));
        tabHost.addTab(tabHost.newTabSpec(ALL_TIME_TAG).setIndicator("All Time").setContent(this));
        tabHost.addTab(tabHost.newTabSpec(DEN_TAG).setIndicator("Den").setContent(this));

        // Add padding to the top of the layout so that it doesn't sink under the status bar.
        int statusBarPadding = 0;
        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
           statusBarPadding = getResources().getDimensionPixelSize(resourceId);
        }
        tabHost.setPadding(0, statusBarPadding, 0, 0);

        return baseLayout;
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);

        if (isVisibleToUser) {
            // The leaderboard fragment is not "fullscreen", but when media is expanded, the status
            // bar needs to be dismissed without causing drastic UI stutters. Thus, the leaderboard
            // is laid out as if it were fullscreen, and when an image or video expands, the bar can
            // be dismissed smoothly
            getActivity().getWindow().getDecorView()
                    .setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        }
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

        UserIdManager userIdManager = (UserIdManager) WolfpakServiceProvider
                .getServiceManager(WolfpakServiceProvider.USERIDMANAGER);
        String userId = userIdManager.getDeviceId();
        localParams.add("user_id", userId);
        denParams.add("user_id", userId);

        localParams.add("latitude", "40.518715");
        localParams.add("longitude", "-74.412095");

        //TODO is_nsfw comes from the user settings
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
     * @param tag The tag of the current tab.
     * @return The tab manager associated with the tab.
     */
    public LeaderboardTabManager getTabManager(String tag) {
        return mTabManagerMap.get(tag);
    }
}
