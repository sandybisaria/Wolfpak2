package com.wolfpakapp.wolfpak2.mainfeed;

import android.content.SharedPreferences;
import android.location.Location;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.wolfpakapp.wolfpak2.Post;
import com.wolfpakapp.wolfpak2.R;
import com.wolfpakapp.wolfpak2.VoteStatus;
import com.wolfpakapp.wolfpak2.WolfpakServiceProvider;
import com.wolfpakapp.wolfpak2.service.LocationProvider;
import com.wolfpakapp.wolfpak2.service.NoLocationException;
import com.wolfpakapp.wolfpak2.service.ServerRestClient;
import com.wolfpakapp.wolfpak2.service.UserIdManager;

import org.apache.http.Header;
import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayDeque;

import pl.droidsonroids.gif.AnimationListener;
import pl.droidsonroids.gif.GifDrawable;

/**
 * The MainFeedNetworkingManager is responsible for the main feed's connection to the server.
 */
public class MainFeedNetworkingManager {

    private ServerRestClient mClient;

    private MainFeedFragment mainFeed;
    private MainFeedLayoutManager layoutManager;

    private RelativeLayout backgroundLayout;
    private ImageView backgroundImageView;
    private GifDrawable noHowlsDrawable;

    private ArrayDeque<Post> postArrayDeque = new ArrayDeque<>();

    private RequestParams mParams = new RequestParams();

    public MainFeedNetworkingManager(MainFeedFragment mainFeed, RelativeLayout backgroundLayout) {
        // Retrieve a server REST client object to be used for calls to the server.
        mClient = (ServerRestClient) WolfpakServiceProvider
                .getServiceManager(WolfpakServiceProvider.SERVERRESTCLIENT);

        this.mainFeed = mainFeed;

        this.backgroundLayout = backgroundLayout;
        backgroundImageView = (ImageView) backgroundLayout.findViewById(R.id.main_feed_no_posts_gif_image_view);
        try {
            noHowlsDrawable = new GifDrawable(mainFeed.getResources(), R.drawable.main_feed_no_howls_background);
            noHowlsDrawable.addAnimationListener(new AnimationListener() {
                @Override
                public void onAnimationCompleted() {
                    noHowlsDrawable.start();
                }
            });
            backgroundImageView.setImageDrawable(noHowlsDrawable);
            noHowlsDrawable.start();
            final ProgressBar loadingProgressBar = (ProgressBar) backgroundLayout
                    .findViewById(R.id.main_feed_loading_posts_progress_bar);
            loadingProgressBar.setVisibility(View.VISIBLE);
            final TextView noHowlsTextView = (TextView) backgroundLayout
                    .findViewById(R.id.main_feed_no_posts_text_view);
            noHowlsTextView.setVisibility(View.GONE);
            backgroundImageView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    noHowlsTextView.setVisibility(View.GONE);
                    loadingProgressBar.setVisibility(View.VISIBLE);
                    getHowls();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Initialize the request parameters for the get request.
     * @throws NoLocationException
     */
    private void initializeRequestParams() throws NoLocationException {
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
                            resetNoHowlsBehavior();
                        } else {
                            startNoHowlsBehavior();
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onFailure(int statusCode, Header[] headers, byte[] errorResponse, Throwable e) {
                    startNoHowlsBehavior();
                }
            });
        } catch (NoLocationException e) {
            startNoHowlsBehavior();
            e.printStackTrace();
        }
    }

    /**
     * Update the like status of the topmost post.
     **/
    public void updateLikeStatus(VoteStatus voteStatus) {
        Post post = postArrayDeque.pollFirst();
        mClient.updateLikeStatus(post.getId(), voteStatus, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                if (postArrayDeque.size() <= 0) {
                    startNoHowlsBehavior();
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                onSuccess(statusCode, headers, responseBody);
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
            // Do nothing.
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
    }

    private void resetNoHowlsBehavior() {
        final ProgressBar loadingProgressBar = (ProgressBar) backgroundLayout
                .findViewById(R.id.main_feed_loading_posts_progress_bar);
        loadingProgressBar.setVisibility(View.GONE);

        noHowlsDrawable.stop();
        noHowlsDrawable.reset();
    }

    private void startNoHowlsBehavior() {
        final ProgressBar loadingProgressBar = (ProgressBar) backgroundLayout
                .findViewById(R.id.main_feed_loading_posts_progress_bar);
        loadingProgressBar.setVisibility(View.GONE);

        final TextView noHowlsTextView = (TextView) backgroundLayout
                .findViewById(R.id.main_feed_no_posts_text_view);
        noHowlsTextView.setVisibility(View.VISIBLE);
    }
}
