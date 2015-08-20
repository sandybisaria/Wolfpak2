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

import java.util.ArrayDeque;
import java.util.Random;

/**
 * Created by Vishaal on 7/20/15.
 */
public class MainFeedNetworkingManager {

    private ServerRestClient mClient;

    private MainFeedFragment mainFeed;
    private MainFeedLayoutManager layoutManager;

    private ArrayDeque<Post> postArrayDeque = new ArrayDeque<>();

    RequestParams mParams = new RequestParams();

    /**
     * Random Number
     **/
    private String randomString;
    private String randomInput;

    public MainFeedNetworkingManager(MainFeedFragment mainFeed) {
        // Retrieve a server REST client object to be used for calls to the server.
        mClient = (ServerRestClient) WolfpakServiceProvider
                .getServiceManager(WolfpakServiceProvider.SERVERRESTCLIENT);

        this.mainFeed = mainFeed;

        randomInput = "";
    }

    /**
     * Initialize the request parameters for the get request.
     * @throws NoLocationException
     */
    public void initializeRequestParams() throws NoLocationException {
        // Retrieve the stored user ID.
        String userId = ((UserIdManager) WolfpakServiceProvider
                .getServiceManager(WolfpakServiceProvider.USERIDMANAGER)).getDeviceId();
        mParams.add("user_id", userId);

        // Retrieve the isNSFW setting.
        SharedPreferences sharedPreferences = PreferenceManager
                .getDefaultSharedPreferences(mainFeed.getActivity());
        boolean isNSFW = sharedPreferences.getBoolean(mainFeed.getString(R.string.nsfw_switch_key), false);
        String isNSFWString = Boolean.toString(isNSFW);
        isNSFWString = isNSFWString.substring(0, 1).toUpperCase() + isNSFWString.substring(1);
        mParams.add("is_nsfw", isNSFWString);

        // Retrieve the current location.
        Location location = ((LocationProvider) WolfpakServiceProvider
                .getServiceManager(WolfpakServiceProvider.LOCATIONPROVIDER)).getLastLocation();
        mParams.add("latitude", Double.toString(location.getLatitude()));
        mParams.add("longitude", Double.toString(location.getLongitude()));
    }

    /**
     * Get the latest howls from the server.
     **/
    public void getHowls() {
        layoutManager = mainFeed.getLayoutManager();

        try {
            // Refresh the query parameters.
            initializeRequestParams();
            mClient.get("posts/", mParams, new AsyncHttpResponseHandler() {

                @Override
                public void onSuccess(int statusCode, Header[] headers, byte[] response) {
                    final JSONArray arr;
                    try {
                        arr = new JSONArray(new String(response));

                        for (int x = 0; x < arr.length(); x++) {
                            try {
                                Post post = Post.parsePostJSONObject(null, arr.getJSONObject(x));
                                // Add each new post to the end of the deque.
                                postArrayDeque.addLast(post);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }

                        layoutManager.loadViews(postArrayDeque);
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
     * Update the like status of the topmost post.
     **/
    public void updateLikeStatus(Post.VoteStatus voteStatus) {
        Post post = postArrayDeque.pollFirst();
        mClient.updateLikeStatus(post.getId(), voteStatus, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {

            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {

            }
        });
    }

    /**
     * Report the topmost post.
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
                        MainFeedNetworkingManager.this.generateRandomString();
                        AlertDialog.Builder alertDialogBuilder1 = new AlertDialog.Builder(mainFeed.getActivity());
                        // set title
                        alertDialogBuilder1.setTitle("Type Captcha in order to report!");
                        // set dialog message

                        // Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
                        input.setInputType(InputType.TYPE_CLASS_TEXT);
                        alertDialogBuilder1.setView(input);

                        final AsyncHttpClient reportput = new AsyncHttpClient(true, 80, 443);

                        alertDialogBuilder1
                                .setMessage("CAPTCHA = " + randomString)
                                .setCancelable(false)
                                .setPositiveButton("Yes!", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog1, int id1) {
                                        randomInput = input.getText().toString();
                                        if (randomString.equals(randomInput)) {
                                            reportput.put("https://ec2-52-4-176-1.compute-1.amazonaws.com/posts/flag/" + postArrayDeque.peekFirst().getId() + "/", new AsyncHttpResponseHandler() {
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
    public void generateRandomString() {
        char[] chars1 = "ABCDEF012GHIJKL345MNOPQR678STUVWXYZ9".toCharArray();
        StringBuilder sb1 = new StringBuilder();
        Random random1 = new Random();
        for (int i = 0; i < 8; i++) {
            char c1 = chars1[random1.nextInt(chars1.length)];
            sb1.append(c1);
        }
        randomString = sb1.toString();
    }
}
