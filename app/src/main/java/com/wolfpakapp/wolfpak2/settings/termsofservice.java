package com.wolfpakapp.wolfpak2.settings;

import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;

import com.wolfpakapp.wolfpak2.R;


//ALL TEXT ON THE TERMS OF SERVICE IS ON THE XML PAGE, AND IT IS UUUUUUGGGGLLLYYY, also eventually need to format it

public class termsofservice extends ActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_termsofservice);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

}
