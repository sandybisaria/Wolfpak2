package com.wolfpakapp.wolfpak2;

import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.wolfpakapp.wolfpak2.service.ServiceManager;
import com.wolfpakapp.wolfpak2.service.UserIdManager;

public class MainActivity extends AppCompatActivity {

    private static final int NUM_PAGES = 2;

    private WolfpakPagerAdapter mPagerAdapter;
    private ViewPager mViewPager;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setupManagers();
        setupUI();
    }

    /**
     * Set up the service managers.
     */
    private void setupManagers() {
        WolfpakServiceProvider.registerServiceManager(WolfpakServiceProvider.USERIDMANAGER,
                new UserIdManager(this));
    }

    /**
     * Set up the UI.
     */
    private void setupUI() {
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
}
