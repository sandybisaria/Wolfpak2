package com.wolfpakapp.wolfpak2.settings;

import android.content.Context;
import android.preference.PreferenceCategory;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

import com.wolfpakapp.wolfpak2.R;

public class WolfpakPreferenceCategory extends PreferenceCategory {

    public WolfpakPreferenceCategory(Context context) {
        super(context);
    }

    public WolfpakPreferenceCategory(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public WolfpakPreferenceCategory(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public WolfpakPreferenceCategory(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);

        TextView titleView = (TextView) view.findViewById(android.R.id.title);
        titleView.setTextColor(getContext().getResources().getColor(R.color.wolfpak_red));
    }
}
