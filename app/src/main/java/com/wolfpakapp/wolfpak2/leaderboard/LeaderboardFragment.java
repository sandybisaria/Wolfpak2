package com.wolfpakapp.wolfpak2.leaderboard;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.TabHost;
import android.widget.TabWidget;

import com.loopj.android.http.RequestParams;
import com.wolfpakapp.wolfpak2.R;
import com.wolfpakapp.wolfpak2.WolfpakServiceProvider;
import com.wolfpakapp.wolfpak2.WolfpakTabHost;
import com.wolfpakapp.wolfpak2.service.LocationProvider;
import com.wolfpakapp.wolfpak2.service.NoLocationException;
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
        try {
            refreshRequestParams(null);
        } catch (NoLocationException e) {
            // Do nothing; let the tab manager handle the exception.
        }
        mRelativeUrlsMap = new HashMap<>();
        setupRelativeUrls();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View baseLayout = inflater.inflate(R.layout.fragment_leaderboard, container, false);
        mBaseFrameLayout = (FrameLayout) baseLayout.findViewById(R.id.leaderboard_base_frame_layout);

        final TabHost tabHost = (TabHost) baseLayout.findViewById(android.R.id.tabhost);
        // REQUIRED for a TabHost loaded with findViewById()
        tabHost.setup();

        // Necessary for the advanced tab behaviors
        ((WolfpakTabHost) tabHost).setParentFragment(this);

        // Set up the tabs
        tabHost.addTab(tabHost.newTabSpec(LOCAL_TAG).setIndicator("Local").setContent(this));
        tabHost.addTab(tabHost.newTabSpec(ALL_TIME_TAG).setIndicator("All Time").setContent(this));
        tabHost.addTab(tabHost.newTabSpec(DEN_TAG).setIndicator("Den").setContent(this));

        // Add padding to the top of the layout so that it isn't obscured by the status bar.
        int statusBarPadding = 0;
        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
           statusBarPadding = getResources().getDimensionPixelSize(resourceId);
        }
        tabHost.setPadding(0, statusBarPadding, 0, 0);

        tabHost.setOnTabChangedListener(new TabHost.OnTabChangeListener() {
            @Override
            public void onTabChanged(String tabId) {
                updateTabBackgrounds(tabHost);
            }
        });
        updateTabBackgrounds(tabHost);


        return baseLayout;
    }

    private void updateTabBackgrounds(TabHost tabHost) {
        TabWidget tabWidget = tabHost.getTabWidget();
        for (int i = 0; i < tabWidget.getChildCount(); i++) {
            if (i == tabHost.getCurrentTab()) {
                tabWidget.getChildAt(i).setBackgroundResource(R.drawable.drawable_selected_tab);
            } else {
                tabWidget.getChildAt(i).setBackgroundResource(R.drawable.drawable_unselected_tab);
            }
        }
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
            getActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
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

        return manager.getSwipeRefreshLayout();
    }

    /**
     * @return The base FrameLayout (with ID tabcontent) that contains the current tab layout.
     */
    public FrameLayout getBaseFrameLayout() {
        return mBaseFrameLayout;
    }

    /**
     * @param tag The tag of the current tab.
     * @return The request parameters associated with the tab.
     */
    public RequestParams getRequestParams(String tag) throws NoLocationException {
        refreshRequestParams(tag);
        return mRequestParamsMap.get(tag);
    }

    /**
     * Refresh the request params (or initialize them if not set).
     * @param tag The tag of the specific request parameter (or null if all)
     */
    public void refreshRequestParams(String tag) throws NoLocationException {
        RequestParams localParams = mRequestParamsMap.get(LOCAL_TAG);
        RequestParams allTimeParams = mRequestParamsMap.get(ALL_TIME_TAG);

        // If localParams is null, set up the
        if (localParams == null) {
            localParams = new RequestParams();
            allTimeParams = new RequestParams();
            RequestParams denParams = new RequestParams();

            String userId = ((UserIdManager) WolfpakServiceProvider
                    .getServiceManager(WolfpakServiceProvider.USERIDMANAGER)).getDeviceId();
            localParams.add("user_id", userId);
            denParams.add("user_id", userId);

            mRequestParamsMap.put(LOCAL_TAG, localParams);
            mRequestParamsMap.put(ALL_TIME_TAG, allTimeParams);
            mRequestParamsMap.put(DEN_TAG, denParams);
        }

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        boolean isNSFW = sharedPreferences.getBoolean(getString(R.string.nsfw_switch_key), false);
        String isNSFWString = Boolean.toString(isNSFW);
        isNSFWString = isNSFWString.substring(0, 1).toUpperCase() + isNSFWString.substring(1);
        localParams.add("is_nsfw", isNSFWString);
        allTimeParams.add("is_nsfw", isNSFWString);

        if (LOCAL_TAG.equals(tag)) {
            Location location = ((LocationProvider) WolfpakServiceProvider
                    .getServiceManager(WolfpakServiceProvider.LOCATIONPROVIDER)).getLastLocation();
            localParams.add("latitude", Double.toString(location.getLatitude()));
            localParams.add("longitude", Double.toString(location.getLongitude()));
        }
    }

    /**
     * Set up the relative URLs that each tab will use to retrieve posts from the server.
     */
    private void setupRelativeUrls() {
        mRelativeUrlsMap.put(LOCAL_TAG, "posts/local_leaderboard/");
        mRelativeUrlsMap.put(ALL_TIME_TAG, "posts/all_time_leaderboard/");
        mRelativeUrlsMap.put(DEN_TAG, "users/den/");
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
