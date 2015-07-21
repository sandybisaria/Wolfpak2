package com.wolfpakapp.wolfpak2.mainfeed;

import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.squareup.picasso.Picasso;
import com.wolfpakapp.wolfpak2.MainActivity;
import com.wolfpakapp.wolfpak2.Post;
import com.wolfpakapp.wolfpak2.R;
import com.wolfpakapp.wolfpak2.ServerRestClient;

import org.apache.http.Header;
import org.json.JSONArray;

import java.util.ArrayDeque;

public class MainFeedFragment extends Fragment {

    private RequestParams mMainFeedParams;
    private ArrayDeque<Post> mPostQueue;

    private FrameLayout mBaseFrameLayout;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setUpRequestParams();
        mPostQueue = new ArrayDeque<>();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View baseLayout = inflater.inflate(R.layout.fragment_main_feed, container, false);

        mBaseFrameLayout = (FrameLayout) baseLayout.findViewById(R.id.main_feed_base_frame_layout);

        retrieveNewPosts();

        return baseLayout;
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);

        if (isVisibleToUser) {
            // The main feed fragment is always fullscreen, so whenever it is visible it dismisses
            // the status bar.
            getActivity().getWindow().getDecorView()
                    .setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN);
        }
    }

    /**
     * Set up the request parameters that the main feed will use to retrieve posts from the server.
     * TODO Set a limit on the number of incoming posts?
     */
    private void setUpRequestParams() {
        mMainFeedParams = new RequestParams();

        //TODO In the "real" app use the device UUID
        String userID = ((MainActivity) getActivity()).getDeviceUUID();
        mMainFeedParams.add("user_id", "temp_test_id");

        Location lastLocation = ((MainActivity) getActivity()).getLastLocation();
        mMainFeedParams.add("latitude", Double.toString(lastLocation.getLatitude()));
        mMainFeedParams.add("longitude", Double.toString(lastLocation.getLongitude()));

        //TODO is_nsfw comes from the user settings
        mMainFeedParams.add("is_nsfw", "False");
    }

    /**
     * Retrieve a fresh set of posts from the server.
     */
    private void retrieveNewPosts() {
        ServerRestClient.get("posts/", mMainFeedParams, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                final JSONArray resArray;
                try {
                    resArray = new JSONArray(new String(responseBody));
                    for (int idx = 0; idx < resArray.length(); idx++) {
                        // Add Posts to the end of the queue.
                        mPostQueue.add(Post.parsePostJSONObject("", resArray.getJSONObject(idx)));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

                displayLatestPost();
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                Log.d("Failure", Integer.toString(statusCode));
            }
        });
    }

    private void displayLatestPost() {
        // Get the post at the
        Post post = mPostQueue.poll();
        if (post == null) {
            //TODO Display refresh button? Or, merely keep the refresh button behind everything?
            return;
        }

        if (post.isImage()) {
            ImageView imageView = new ImageView(getActivity());

            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            imageView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
            Picasso.with(getActivity()).load(post.getMediaUrl()).into(imageView);

            mBaseFrameLayout.addView(imageView);
        }
    }
}
