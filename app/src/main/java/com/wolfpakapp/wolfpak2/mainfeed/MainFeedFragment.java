package com.wolfpakapp.wolfpak2.mainfeed;

import android.os.Bundle;
import android.os.SystemClock;
import android.support.v4.app.Fragment;
import android.support.v4.view.MotionEventCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.squareup.picasso.Picasso;
import com.wolfpakapp.wolfpak2.Post;
import com.wolfpakapp.wolfpak2.R;
import com.wolfpakapp.wolfpak2.service.ServerRestClient;
import com.wolfpakapp.wolfpak2.WolfpakServiceProvider;
import com.wolfpakapp.wolfpak2.service.UserIdManager;

import org.apache.http.Header;
import org.json.JSONArray;

import java.util.ArrayDeque;

public class MainFeedFragment extends Fragment {

    private RequestParams mMainFeedParams;
    private ArrayDeque<Post> mPostQueue;

    private FrameLayout mBaseFrameLayout;

    private ServerRestClient mClient;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mClient = (ServerRestClient) WolfpakServiceProvider
                .getServiceManager(WolfpakServiceProvider.SERVERRESTCLIENT);

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

        UserIdManager userIdManager = (UserIdManager) WolfpakServiceProvider
                .getServiceManager(WolfpakServiceProvider.USERIDMANAGER);
        String userId = userIdManager.getDeviceId();
        mMainFeedParams.add("user_id", userId);

        //TODO GPS coordinates come from the manager
        mMainFeedParams.add("latitude", "40.518715");
        mMainFeedParams.add("longitude", "-74.412095");

        //TODO is_nsfw comes from the user settings
        mMainFeedParams.add("is_nsfw", "False");
    }

    /**
     * Retrieve a fresh set of posts from the server.
     */
    private void retrieveNewPosts() {
        mClient.get("posts/", mMainFeedParams, new AsyncHttpResponseHandler() {
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

    /**
     * Display the latest post in the queue.
     */
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

            imageView.setOnTouchListener(new PostOnTouchListener(post));

            mBaseFrameLayout.addView(imageView);
        } else {
            //TODO Support video (for now, just skip it)
            displayLatestPost();
        }
    }

    /**
     * The PostOnTouchListener handles touch events to the main feed post views.
     */
    private class PostOnTouchListener implements View.OnTouchListener {

        private long initialTouchTime = 0;
        private float initialTouchX = 0;
        private float initialTouchY = 0;
        private final int WAIT_TIME = 50;

        private Float initialViewX = null;
        private Float initialViewY = null;

        private float lastTouchX = 0;
        private float lastTouchY = 0;

        private Post mPost;

        private Boolean canSwipe = null;

        public PostOnTouchListener(Post post) {
            mPost = post;
        }

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            final int action = MotionEventCompat.getActionMasked(event);

            switch (action) {
                case MotionEvent.ACTION_DOWN: {
                    initialTouchTime = SystemClock.uptimeMillis();

                    initialTouchX = event.getRawX();
                    initialTouchY = event.getRawY();

                    lastTouchX = initialTouchX;
                    lastTouchY = initialTouchY;

                    if (initialViewX == null) {
                        //TODO If the page is swiped and the view moves, reset the view's position!
                        initialViewX = v.getX();
                        initialViewY = v.getY();
                    }

                    return true;
                }
                case MotionEvent.ACTION_MOVE: {
                    final float x = event.getRawX();
                    final float y = event.getRawY();

                    final float dx = x - lastTouchX;
                    final float dy = y - lastTouchY;

                    if (canSwipe == null && SystemClock.uptimeMillis() - initialTouchTime >= WAIT_TIME) {
                        final float deltaX = Math.abs(x - initialTouchX);
                        final float deltaY = Math.abs(y - initialTouchY);

                        if (deltaX > deltaY) {
                            Log.d("canSwipe", "true");
                            canSwipe = true;
                        } else {
                            Log.d("canSwipe", "false");
                            canSwipe = false;
                            requestDisallowInterceptTouchEventForParents(v, true);
                        }
                    }

                    v.setX(v.getX() + dx);
                    v.setY(v.getY() + dy);

                    lastTouchX = x;
                    lastTouchY = y;

                    return true;
                }
                case MotionEvent.ACTION_POINTER_UP: {
                    return true;
                }
                case MotionEvent.ACTION_UP: {
                    if (canSwipe != null && !canSwipe) {
                        requestDisallowInterceptTouchEventForParents(v, false);
                    }

                    try {
                        final float totalDeltaY = initialTouchY - lastTouchY;

                        if (Math.abs(totalDeltaY) > 500) {
                            displayLatestPost();
                            dismissPost(mPost, v);
                        } else {
                            v.setX(initialViewX);
                            v.setY(initialViewY);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    return true;
                }
            }

            return false;
        }

        /**
         * Call requestDisallowInterceptTouchEvent() on all parents of the view.
         * @param v The child view.
         * @param disallowIntercept True to stop the parent from intercepting touch events.
         * TODO Identical to LeaderboardTabAdapter method!
         */
        private void requestDisallowInterceptTouchEventForParents(View v,
                                                                  boolean disallowIntercept) {
            ViewParent parent = v.getParent();
            while (parent != null) {
                parent.requestDisallowInterceptTouchEvent(disallowIntercept);
                parent = parent.getParent();
            }

        }
    }

    /**
     * Dismiss the current post.
     * @param post
     * @param v
     */
    private void dismissPost(Post post, View v) {
        post.toString();
        mBaseFrameLayout.removeView(v);
    }
}
