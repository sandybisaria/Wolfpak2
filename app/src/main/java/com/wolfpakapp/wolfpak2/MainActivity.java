package com.wolfpakapp.wolfpak2;

import android.location.Location;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;

public class MainActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener{

    private static final int NUM_PAGES = 1;

    private WolfpakPagerAdapter mPagerAdapter;
    private ViewPager mViewPager;

    private GoogleApiClient mGoogleApiClient;
    private Location mLastLocation = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        buildGoogleApiClient();
        mGoogleApiClient.connect();
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

    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }

    @Override
    public void onConnected(Bundle bundle) {
        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(
                mGoogleApiClient);
        setUpUI();
    }

    @Override
    public void onConnectionSuspended(int i) {
        // To be implemented...
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        // To be implemented...
    }

    public Location getLastLocation() {
        return mLastLocation;
    }
}
