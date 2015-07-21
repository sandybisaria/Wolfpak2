package com.wolfpakapp.wolfpak2.mainfeed;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.wolfpakapp.wolfpak2.R;

public class MainFeedFragment extends Fragment {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View baseLayout = inflater.inflate(R.layout.fragment_main_feed, container, false);

        return baseLayout;
    }
}
