package com.wolfpakapp.wolfpak2;

import android.app.Activity;
import android.content.Intent;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.wolfpakapp.wolfpak2.service.LocationProvider;
import com.wolfpakapp.wolfpak2.service.ServerRestClient;
import com.wolfpakapp.wolfpak2.service.UserIdManager;

public class MainActivity extends AppCompatActivity {

    private static final int NUM_PAGES = 4;
    public static final int REQUEST_CHECK_SETTINGS = 123;

    private WolfpakPagerAdapter mPagerAdapter;
    private ViewPager mViewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Once all the service managers are initialized, continue with running the app.
        // This will be useful when we have to check for features like Facebook, Internet, and GPS.
        // If any service is not available, then the ServiceManagers can prompt the user to enable
        // them (the exact behavior depends on the manager).
        setupManagers();
    }

    /**
     * Set up the service managers.
     */
    private void setupManagers() {
        WolfpakServiceProvider.registerServiceManager(WolfpakServiceProvider.USERIDMANAGER,
                new UserIdManager(this));
        WolfpakServiceProvider.registerServiceManager(WolfpakServiceProvider.SERVERRESTCLIENT,
                new ServerRestClient(this));
        WolfpakServiceProvider.registerServiceManager(WolfpakServiceProvider.LOCATIONPROVIDER,
                new LocationProvider(this));
        WolfpakServiceProvider
                .setOnAllInitializedCallback(new WolfpakServiceProvider.OnAllInitializedCallback() {
                    @Override
                    public void onAllInitialized() {
                        setupUI();
                    }
                });
        WolfpakServiceProvider.startWaiting();
    }

    /**
     * Set up the user interface elements.
     */
    private void setupUI() {
        mPagerAdapter = new WolfpakPagerAdapter(getSupportFragmentManager());
        mViewPager = (ViewPager) findViewById(R.id.main_activity_view_pager);
        mViewPager.setAdapter(mPagerAdapter);
        // The default page is the main feed.
        mViewPager.setCurrentItem(2);
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_CHECK_SETTINGS: {
                switch (resultCode) {
                    case Activity.RESULT_OK: {
                        // All required changes were successfully made, so retrieve the location
                        ((LocationProvider) WolfpakServiceProvider
                                .getServiceManager(WolfpakServiceProvider.LOCATIONPROVIDER))
                                .retrieveLocation();
                        break;
                    }
                    case Activity.RESULT_CANCELED: {
                        // The user was asked to change settings, but chose not to
                        //TODO Define app behaviors when required services aren't available!
//                        returnHome();
                        break;
                    }
                    default: {
                        break;
                    }
                }
                break;
            }
        }
    }

    public void returnHome() {
        Intent homeIntent = new Intent(Intent.ACTION_MAIN);
        homeIntent.addCategory(Intent.CATEGORY_HOME);
        startActivity(homeIntent);
    }

    /**
     * @return The number of pages in the Wolfpak application
     */
    public static int getNumPages() {
        return NUM_PAGES;
    }
}
