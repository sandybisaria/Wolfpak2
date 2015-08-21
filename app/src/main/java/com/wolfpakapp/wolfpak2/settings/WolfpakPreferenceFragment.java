package com.wolfpakapp.wolfpak2.settings;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.support.v4.preference.PreferenceFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ListAdapter;

import com.wolfpakapp.wolfpak2.R;

public class WolfpakPreferenceFragment extends PreferenceFragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.preferences);

        Preference socialMediaPreference = findPreference("followus");
        socialMediaPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                createListPreferenceDialog();
                return true;

            }
        });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);

        View root = view.findViewById(android.R.id.list);
        // Add padding to the top of the layout so that it doesn't sink under the status bar.
        int statusBarPadding = 0;
        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            statusBarPadding = getResources().getDimensionPixelSize(resourceId);
        }
        root.setPadding(0, statusBarPadding, 0, 0);

        return view;
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);

        if (isVisibleToUser) {
            getActivity().getWindow().getDecorView()
                    .setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
            getActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
    }

    //THIS CREATES THE DIALOG FOR THE SOCIAL MEDIA BUTTONS
    private void createListPreferenceDialog() {

        Dialog dialog;

        //GET THE NAME OF THE SOCIAL MEDIA PLATFORMS
        final String[] str = getResources().getStringArray(R.array.iconName);

        //SET THE ICONS (NOTE TO SELF: FIND SMALLER GODDAMN ICONS LMAO)
        final Integer[] icons = new Integer[] {R.drawable.tinyfb, R.drawable.twit, R.drawable.insta};

        //COMBINE WITH MAGIC FUNCTION FOUND ON STACKOVERFLOW HERE: https://stackoverflow.com/questions/8533394/icons-in-a-list-dialog
        ListAdapter adapter = new ArrayAdapterWithIcon(getActivity(), str, icons);

        //MAKE AND SHOW THE DIALOG

        dialog = new AlertDialog.Builder(getActivity()).setTitle("Follow Us on Social Media").setAdapter(adapter, new DialogInterface.OnClickListener() {

            public void onClick(DialogInterface dialog, int item) {

                Intent open;

                //WHAT HAPPENS WHEN YOU CLICK A THING
                switch (item) {
                    case (0):
                        open = getOpenFacebookIntent(getActivity());
                        startActivity(open);
                        break;
                    case (1):
                        open = getOpenTwitterIntent(getActivity());
                        startActivity(open);
                        break;
                    case (2):

                        //INSTA IS WEIRD SO FUCK IT I'll JUST DO IT THIS WAY

                        Uri uri = Uri.parse("http://instagram.com/_u/wolfpakapp");
                        Intent likeIng = new Intent(Intent.ACTION_VIEW, uri);

                        likeIng.setPackage("com.instagram.android");

                        try {
                            startActivity(likeIng);
                        } catch (Exception e) {
                            startActivity(new Intent(Intent.ACTION_VIEW,
                                    Uri.parse("http://instagram.com/wolfpakapp")));
                        }
                        break;
                    default:
                        //IS THERE EVEN A DEFAULT FOR THIS? WHO KNOWS? PUT IT ANYWAYS
                        break;
                }

            }
        }).show();

        dialog.setCanceledOnTouchOutside(true);

       /*
        //THIS ALLOWS YOU TO MOVE THE DIALOG AROUND THE SCREEN WITH OFFSETS

        WindowManager.LayoutParams layoutParams = dialog.getWindow().getAttributes();
        layoutParams.x = 0; // left margin
        layoutParams.y = 0; // bottom margin
        dialog.getWindow().setAttributes(layoutParams);*/


    }


    //INTENTS TO OPEN THE SOCIAL MEDIA ACCOUNTS

    //FACEBOOK
    public static Intent getOpenFacebookIntent(Context context) {

        try {
            context.getPackageManager().getPackageInfo("com.facebook.katana", 0);
            return new Intent(Intent.ACTION_VIEW, Uri.parse("fb://page/1444711199174503"));
        } catch (Exception e) {
            return new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.facebook.com/wolfpak-app"));
        }
    }

    //TWITTER
    public Intent getOpenTwitterIntent(Context context) {

        try {
            return new Intent(Intent.ACTION_VIEW,
                    Uri.parse("twitter://user?screen_name="
                            .concat("wolfpakapp")));

        } catch (Exception e) {
            return new Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://twitter.com/#!/".concat("wolfpakapp")));
        }

    }
}
