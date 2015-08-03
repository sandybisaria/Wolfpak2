package com.wolfpakapp.wolfpak2.settings;

import android.os.Bundle;
import android.support.v4.preference.PreferenceFragment;

import com.wolfpakapp.wolfpak2.R;

public class WolfpakPreferenceFragment extends PreferenceFragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.preferences);
    }
}
