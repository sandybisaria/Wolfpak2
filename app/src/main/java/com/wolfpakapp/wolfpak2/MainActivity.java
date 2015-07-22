package com.wolfpakapp.wolfpak2;

import android.location.Location;
import android.provider.Settings;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;


import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private static final int NUM_PAGES = 2;

    private WolfpakPagerAdapter mPagerAdapter;
    private ViewPager mViewPager;

    private String mDeviceUUID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        retrieveDeviceId();

        setUpUI();
    }

    /**
     * Retrieve the device's unique UUID from memory (or generate it for a first-time user).
     */
    private void retrieveDeviceId() {
        //TODO Store on the phone so you don't have to generate it every time!
        TelephonyManager manager = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
        String deviceId = manager.getDeviceId();
        String serialNumber = manager.getSimSerialNumber();
        String androidId = Settings.Secure.getString(getContentResolver(),
                Settings.Secure.ANDROID_ID);
        UUID deviceUUID = new UUID(androidId.hashCode(),
                ((long) deviceId.hashCode() << 32) | serialNumber.hashCode());
        mDeviceUUID = deviceUUID.toString();
    }

    /**
     * Set up the UI (after connecting to the Internet...)
     */
    private void setUpUI() {
        mPagerAdapter = new WolfpakPagerAdapter(getSupportFragmentManager());
        mViewPager = (ViewPager) findViewById(R.id.main_activity_view_pager);
        mViewPager.setAdapter(mPagerAdapter);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * @return The number of pages in the Wolfpak application
     */
    public static int getNumPages() {
        return NUM_PAGES;
    }

    public String getDeviceUUID() {
        return mDeviceUUID;
    }
}
