package com.wolfpakapp.wolfpak2.mainfeed;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.location.Location;
import android.preference.PreferenceManager;
import android.text.InputType;
import android.util.Log;
import android.widget.EditText;
import android.widget.Toast;

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

    public MainFeedNetworkingManager(MainFeedFragment mainFeed) {
        // Retrieve a server REST client object to be used for calls to the server.
        mClient = (ServerRestClient) WolfpakServiceProvider
                .getServiceManager(WolfpakServiceProvider.SERVERRESTCLIENT);

        this.mainFeed = mainFeed;
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

                        if (postArrayDeque.size() > 0) {
                            layoutManager.loadViews(postArrayDeque);
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
     * Display the flag dialog.
     **/
    public void showFlagDialog() {
        if (postArrayDeque.size() > 0) {
            FlagDialog flagDialog = new FlagDialog();
            flagDialog.setFlagDialogListener(new FlagDialog.FlagDialogListener() {
                @Override
                public void onDialogPositiveClick() {
                    reportPost();
                }

                @Override
                public void onDialogNegativeClick() {

                }

                @Override
                public void onDialogCanceled() {

                }
            });

            flagDialog.show(mainFeed.getActivity().getFragmentManager(), "FlagDialog");
        } else {
            // Do nothing?
        }
    }

    /**
     * Report the topmost post.
     */
    private void reportPost() {
        Post post = postArrayDeque.peekFirst();
        mClient.put("posts/flag/" + Integer.toString(post.getId()) + "/", null, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                Toast.makeText(mainFeed.getActivity(), "Successfully reported", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                Toast.makeText(mainFeed.getActivity(), "Failed to report...", Toast.LENGTH_SHORT).show();
            }
        });
//        AlertDialog.Builder questionBuilder = new AlertDialog.Builder(mainFeed.getActivity());
//        final EditText random = new EditText(mainFeed.getActivity());
//        random.setInputType(InputType.TYPE_CLASS_TEXT);
//
//        // Set the title.
//        questionBuilder.setTitle("FLAG!");
//        final EditText input = new EditText(mainFeed.getActivity());
//
//        // Set the dialog message.
//        questionBuilder
//                .setMessage("Do you really want to report this howl?")
//                .setCancelable(false)
//                .setPositiveButton("Yes!", new DialogInterface.OnClickListener() {
//                    @Override
//                    public void onClick(DialogInterface dialog, int id) {
//                        MainFeedNetworkingManager.this.generateRandomString();
//                        AlertDialog.Builder captchaBuilder = new AlertDialog.Builder(mainFeed.getActivity());
//                        // set title
//                        captchaBuilder.setTitle("Type Captcha in order to report!");
//                        // set dialog message
//
//                        // Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
//                        input.setInputType(InputType.TYPE_CLASS_TEXT);
//                        captchaBuilder.setView(input);
//
//                        final AsyncHttpClient reportput = new AsyncHttpClient(true, 80, 443);
//                        final String randomString = generateRandomString();
//
//                        captchaBuilder
//                                .setMessage("CAPTCHA = " + randomString)
//                                .setCancelable(false)
//                                .setPositiveButton("Yes!", new DialogInterface.OnClickListener() {
//                                    @Override
//                                    public void onClick(DialogInterface dialog1, int id1) {
//                                        String randomInput = input.getText().toString();
//                                        if (randomString.equals(randomInput)) {
//                                            reportput.put("https://ec2-52-4-176-1.compute-1.amazonaws.com/posts/flag/" + postArrayDeque.peekFirst().getId() + "/", new AsyncHttpResponseHandler() {
//                                                @Override
//                                                public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
//                                                }
//
//                                                @Override
//                                                public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
//                                                }
//                                            });
//                                        } else {
//                                            dialog1.cancel();
//                                        }
//
//                                    }
//                                })
//                                .setNegativeButton("No", new DialogInterface.OnClickListener() {
//                                    @Override
//                                    public void onClick(DialogInterface dialog1, int id1) {
//                                        // if this button is clicked, just close
//                                        // the dialog box and do nothing
//                                        dialog1.cancel();
//                                    }
//                                });
//
//                        // create alert dialog
//                        AlertDialog alertDialog = captchaBuilder.create();
//
//                        // show it
//                        alertDialog.show();
//                    }
//                })
//                .setNegativeButton("No", new DialogInterface.OnClickListener() {
//                    @Override
//                    public void onClick(DialogInterface dialog, int id) {
//                        // if this button is clicked, just close
//                        // the dialog box and do nothing
//                        dialog.cancel();
//                    }
//                });
//
//        // create alert dialog
//        AlertDialog alertDialog = questionBuilder.create();
//
//        // show it
//        alertDialog.show();
    }
}
