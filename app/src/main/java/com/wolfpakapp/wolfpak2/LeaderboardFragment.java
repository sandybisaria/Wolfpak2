package com.wolfpakapp.wolfpak2;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TabHost;
import android.widget.TextView;

public class LeaderboardFragment extends Fragment implements TabHost.TabContentFactory {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View baseLayout = inflater.inflate(R.layout.fragment_leaderboard, container, false);

        TabHost tabHost = (TabHost) baseLayout.findViewById(android.R.id.tabhost);
        tabHost.setup();

        tabHost.addTab(tabHost.newTabSpec("tab1").setIndicator("tab1").setContent(this));
        tabHost.addTab(tabHost.newTabSpec("tab2").setIndicator("tab2").setContent(this));

        return baseLayout;
    }

    @Override
    public View createTabContent(String tag) {
        TextView textView = new TextView(getActivity());
        textView.setText("TRIAL: " + tag);
        return textView;
    }
}
