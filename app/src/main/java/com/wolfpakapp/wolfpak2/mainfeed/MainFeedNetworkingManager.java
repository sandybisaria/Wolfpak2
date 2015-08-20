package com.wolfpakapp.wolfpak2.mainfeed;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.location.Location;
import android.preference.PreferenceManager;
import android.text.InputType;
import android.widget.EditText;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.wolfpakapp.wolfpak2.Post;
import com.wolfpakapp.wolfpak2.R;
import com.wolfpakapp.wolfpak2.WolfpakServiceProvider;
import com.wolfpakapp.wolfpak2.service.LocationProvider;
import com.wolfpakapp.wolfpak2.service.NoLocationException;
import com.wolfpakapp.wolfpak2.service.ServerRestClient;
import com.wolfpakapp.wolfpak2.service.UserIdManager;

import org.apache.http.Header;
import org.json.JSONArray;
import org.json.JSONException;

import java.util.Objects;
import java.util.Random;

/**
 * Created by Vishaal on 7/20/15.
 */
public class MainFeedNetworkingManager {

    private ServerRestClient mClient;

    public int length;
    private MainFeedFragment mainFeed;
    private MainFeedLayoutManager layoutManager;

    public Post[] mPosts;

    RequestParams mParams = new RequestParams();

    /**
     * Random Number
     **/
    private String random_string;
    private String random_input;

    public int count;

    public MainFeedNetworkingManager(MainFeedFragment mainFeed) {
        mClient = (ServerRestClient) WolfpakServiceProvider
                .getServiceManager(WolfpakServiceProvider.SERVERRESTCLIENT);

        this.mainFeed = mainFeed;
        this.layoutManager = new MainFeedLayoutManager(mainFeed, this);

        length = 10;
        count = 0;

        mPosts = new Post[length];

        random_input = "";
    }

    public void initializeRequestParams() throws NoLocationException {
        String userId = ((UserIdManager) WolfpakServiceProvider
                .getServiceManager(WolfpakServiceProvider.USERIDMANAGER)).getDeviceId();
        mParams.add("user_id", userId);

        SharedPreferences sharedPreferences = PreferenceManager
                .getDefaultSharedPreferences(mainFeed.getActivity());
        boolean isNSFW = sharedPreferences.getBoolean(mainFeed.getString(R.string.nsfw_switch_key), false);
        String isNSFWString = Boolean.toString(isNSFW);
        isNSFWString = isNSFWString.substring(0, 1).toUpperCase() + isNSFWString.substring(1);
        mParams.add("is_nsfw", isNSFWString);

        Location location = ((LocationProvider) WolfpakServiceProvider
                .getServiceManager(WolfpakServiceProvider.LOCATIONPROVIDER)).getLastLocation();
        mParams.add("latitude", Double.toString(location.getLatitude()));
        mParams.add("longitude", Double.toString(location.getLongitude()));
    }

    /**
     * Get the latest howls from the server.
     **/
    public void getHowls() {
        try {
            // Refresh the query string with the latest location.
            initializeRequestParams();
            mClient.get("posts/", mParams, new AsyncHttpResponseHandler() {

                @Override
                public void onSuccess(int statusCode, Header[] headers, byte[] response) {
                    final JSONArray arr;
                    try {
                        arr = new JSONArray(new String(response));

                        for (int x = 0; x < 5 || x < arr.length(); x++) {
                            try {
                                mPosts[x] = Post.parsePostJSONObject(null, arr.getJSONObject(x));
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }

                        layoutManager.num = arr.length() - 1;
                        int size = arr.length() - 1;
                        count = arr.length();

                        for (int x = size; x > -1; x--) {
                            layoutManager.loadView(mPosts[x]);
                        }

                        if (!mPosts[0].isImage()) {
                            layoutManager.views[0].mediaVideoView.start();
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onFailure(int statusCode, Header[] headers, byte[] errorResponse, Throwable e) {
                    // Display nothing.
                }
            });
        } catch (NoLocationException e) {
            // Display nothing.
            e.printStackTrace();
        }
    }

    /**
     * Update the like status of the post.
     **/
    public void updateLikeStatus(Post.VoteStatus voteStatus) {
        mClient.updateLikeStatus(mPosts[mainFeed.number].getId(), voteStatus, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {

            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {

            }
        });
    }

    /**
     * Asynchronous HTTP Client - Reports Image/Video in Server
     **/
    public void reportHowl() {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(mainFeed.getActivity());
        final EditText random = new EditText(mainFeed.getActivity());
        random.setInputType(InputType.TYPE_CLASS_TEXT);

        // set title
        alertDialogBuilder.setTitle("FLAG!!");
        final EditText input = new EditText(mainFeed.getActivity());

        // set dialog message
        alertDialogBuilder
                .setMessage("Do you really want to report this howl?")
                .setCancelable(false)
                .setPositiveButton("Yes!", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        MainFeedNetworkingManager.this.randomstring();
                        AlertDialog.Builder alertDialogBuilder1 = new AlertDialog.Builder(mainFeed.getActivity());
                        // set title
                        alertDialogBuilder1.setTitle("Type Captcha in order to report!");
                        // set dialog message

                        // Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
                        input.setInputType(InputType.TYPE_CLASS_TEXT);
                        alertDialogBuilder1.setView(input);

                        final AsyncHttpClient reportput = new AsyncHttpClient(true, 80, 443);

                        alertDialogBuilder1
                                .setMessage("CAPTCHA = " + random_string)
                                .setCancelable(false)
                                .setPositiveButton("Yes!", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog1, int id1) {
                                        random_input = input.getText().toString();
                                        if (random_string.equals(random_input)) {
                                            reportput.put("https://ec2-52-4-176-1.compute-1.amazonaws.com/posts/flag/" + mPosts[mainFeed.number].getId() + "/", new AsyncHttpResponseHandler() {
                                                @Override
                                                public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                                                }

                                                @Override
                                                public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                                                }
                                            });
                                        } else {
                                            dialog1.cancel();
                                        }

                                    }
                                })
                                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog1, int id1) {
                                        // if this button is clicked, just close
                                        // the dialog box and do nothing
                                        dialog1.cancel();
                                    }
                                });

                        // create alert dialog
                        AlertDialog alertDialog = alertDialogBuilder1.create();

                        // show it
                        alertDialog.show();
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        // if this button is clicked, just close
                        // the dialog box and do nothing
                        dialog.cancel();
                    }
                });

        // create alert dialog
        AlertDialog alertDialog = alertDialogBuilder.create();

        // show it
        alertDialog.show();
    }

    /**
     * Random Number Generator for reportHowl()
     **/
    public void randomstring() {
        char[] chars1 = "ABCDEF012GHIJKL345MNOPQR678STUVWXYZ9".toCharArray();
        StringBuilder sb1 = new StringBuilder();
        Random random1 = new Random();
        for (int i = 0; i < 8; i++) {
            char c1 = chars1[random1.nextInt(chars1.length)];
            sb1.append(c1);
        }
        random_string = sb1.toString();
    }
}
