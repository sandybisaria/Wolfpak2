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
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.FrameLayout;
<<<<<<< HEAD
import android.widget.ImageView;
import android.widget.VideoView;
=======
import android.widget.Toast;
>>>>>>> cd6a104d641cbb3b8fc14a6ea295f58f3378baa9

import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;
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
<<<<<<< HEAD
    private ArrayDeque<View> mVisibleViewQueue;
=======
    private ArrayDeque<PostView> mPostViewQueue;

    private Post topPost = null;
    private PostView topPostView = null;
>>>>>>> cd6a104d641cbb3b8fc14a6ea295f58f3378baa9

    private FrameLayout mBaseFrameLayout;

    private ServerRestClient mClient;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mClient = (ServerRestClient) WolfpakServiceProvider
                .getServiceManager(WolfpakServiceProvider.SERVERRESTCLIENT);

        setupRequestParams();
        mPostQueue = new ArrayDeque<>();
<<<<<<< HEAD
        mVisibleViewQueue = new ArrayDeque<>();
=======
        mPostViewQueue = new ArrayDeque<>();
>>>>>>> cd6a104d641cbb3b8fc14a6ea295f58f3378baa9
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View baseLayout = inflater.inflate(R.layout.fragment_main_feed, container, false);

        mBaseFrameLayout = (FrameLayout) baseLayout.findViewById(R.id.main_feed_base_frame_layout);

        retrieveNewPosts();

        return baseLayout;
    }

    /**
     * This method is called whenever the visibility of the fragment changes (true if visible). It
     * can be used to perform certain actions such as change the visibility of system UI elements.
     * @param isVisibleToUser
     */
    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);

        if (isVisibleToUser) {
            // The main feed fragment is always fullscreen, so whenever it is visible it dismisses
            // the status bar.
            getActivity().getWindow().getDecorView()
                    .setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN);
<<<<<<< HEAD
        } else {
            if (mVisibleViewQueue != null) {
                for (View view : mVisibleViewQueue) {
=======
            // See displayLatestPost()
            if (mPostQueue != null && mPostQueue.size() == 0) {
                Toast.makeText(getActivity(), "No posts", Toast.LENGTH_SHORT).show();
            }
        } else {
            if (mPostViewQueue != null) {
                for (View view : mPostViewQueue) {
>>>>>>> cd6a104d641cbb3b8fc14a6ea295f58f3378baa9
                    view.setX(0);
                    view.setY(0);
                }
            }
        }
    }

    /**
     * Set up the request parameters that the main feed will use to retrieve posts from the server.
     * TODO Set a limit on the number of incoming posts?
     */
    private void setupRequestParams() {
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
                        Post newPost = Post.parsePostJSONObject("", resArray.getJSONObject(idx));
                        mPostQueue.add(newPost);

                        PostView postView = new PostView(getActivity(), newPost);
                        postView.setVisibility(View.INVISIBLE);
                        mBaseFrameLayout.addView(postView);
                        mPostViewQueue.add(postView);
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
        topPostView = mPostViewQueue.poll();
        topPost = mPostQueue.poll();

        if (topPostView == null) {
           //TODO What happens when there are no posts?
            Toast.makeText(getActivity(), "No posts", Toast.LENGTH_SHORT).show();
        }

<<<<<<< HEAD
            mBaseFrameLayout.addView(imageView);
            mVisibleViewQueue.add(imageView);
        } else {
            VideoView videoView = new VideoView(getActivity());

            videoView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
            videoView.setVideoPath(post.getMediaUrl());
            videoView.start();

            videoView.setOnTouchListener(new PostOnTouchListener(post));

            mBaseFrameLayout.addView(videoView);
            mVisibleViewQueue.add(videoView);
=======
        PostView secondPostView = mPostViewQueue.peek();
        if (secondPostView != null) {
            secondPostView.setVisibility(View.VISIBLE);
>>>>>>> cd6a104d641cbb3b8fc14a6ea295f58f3378baa9
        }

        topPostView.setVisibility(View.VISIBLE);
        topPostView.bringToFront();
        topPostView.setOnTouchListener(new PostOnTouchListener());
        topPostView.start();
    }

    /**
     * The PostOnTouchListener handles touch events to the main feed post views.
     */
    private class PostOnTouchListener implements View.OnTouchListener {

        private long initialTouchTime = 0;
        private float initialTouchX = 0;
        private float initialTouchY = 0;
        private final int WAIT_TIME = 50;

        private float lastTouchX = 0;
        private float lastTouchY = 0;

        private Boolean canSwipe = null;

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

                    return true;
                }
                case MotionEvent.ACTION_MOVE: {
                    final float x = event.getRawX();
                    final float y = event.getRawY();

                    final float dx = x - lastTouchX;
                    final float dy = y - lastTouchY;

                    // This is a method I was considering to determine between a swipe (through pages)
                    // or a "drag" of the post. Basically, during a small fraction of time, if the
                    // user moved horizontally more than vertically, we can assume that he will
                    // likely want to swipe and thus permit him to do so.
                    if (canSwipe == null && SystemClock.uptimeMillis() - initialTouchTime >= WAIT_TIME) {
                        final float deltaX = Math.abs(x - initialTouchX);
                        final float deltaY = Math.abs(y - initialTouchY);

                        if (deltaX > deltaY) {
                            Log.d("canSwipe", "true");
                            canSwipe = true;
                        } else {
                            Log.d("canSwipe", "false");
                            canSwipe = false;
                            // This function disables the swipe between pages.
                            requestDisallowInterceptTouchEventForParents(v, true);
                        }
                    }
<<<<<<< HEAD
                    if (canSwipe != null && !canSwipe) {
                        v.setX(v.getX() + dx);
                        v.setY(v.getY() + dy);
=======
                    // Now, if canSwipe was set to false, we can allow the PostView to be manipulated
                    if (canSwipe != null && !canSwipe) {
                        v.setX(v.getX() + dx);
                        v.setY(v.getY() + dy);

                        if (isUpvoting()) {
                            ((PostView) v).setTint(Post.VoteStatus.UPVOTED);
                        } else if (isDownvoting()) {
                            ((PostView) v).setTint(Post.VoteStatus.DOWNVOTED);
                        } else {
                            ((PostView) v).setTint(Post.VoteStatus.NOT_VOTED);
                        }
>>>>>>> cd6a104d641cbb3b8fc14a6ea295f58f3378baa9
                    }

                    lastTouchX = x;
                    lastTouchY = y;

                    return true;
                }
                case MotionEvent.ACTION_POINTER_UP: {
                    return true;
                }
                case MotionEvent.ACTION_UP: {
                    // Reset the disallowInterceptTouchEvent for the parents (if it was previously set.
                    if (canSwipe != null && !canSwipe) {
                        requestDisallowInterceptTouchEventForParents(v, false);
                    }

                    try {
                        if (isUpvoting()) {
                            dismissPost(Post.VoteStatus.UPVOTED);
                            displayLatestPost();
                        } else if (isDownvoting()) {
                            dismissPost(Post.VoteStatus.DOWNVOTED);
                            displayLatestPost();
                        } else {
                            v.setX(0);
                            v.setY(0);
                            canSwipe = null;
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    return true;
                }
            }

            return false;
        }

        private boolean isUpvoting() {
            final float totalDeltaY = initialTouchY - lastTouchY;
            if (totalDeltaY > 300) {
                return true;
            } else {
                return false;
            }
        }

        private boolean isDownvoting() {
            final float totalDeltaY = initialTouchY - lastTouchY;
            if (totalDeltaY < -300) {
                return true;
            } else {
                return false;
            }
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

    private void dismissPost(Post.VoteStatus voteStatus) {
        //TODO Determine whether to upvote or downvote (requires additional parameters...)
<<<<<<< HEAD
        mVisibleViewQueue.remove(v);
        mBaseFrameLayout.removeView(v);
=======
        if (voteStatus == Post.VoteStatus.UPVOTED) {
            Animation slide = new TranslateAnimation(Animation.RELATIVE_TO_PARENT, 0.0f,
                    Animation.RELATIVE_TO_PARENT, 0.0f, Animation.RELATIVE_TO_PARENT,
                    0.0f, Animation.RELATIVE_TO_PARENT, -5.0f);
            slide.setDuration(750);

            topPostView.startAnimation(slide);
            topPostView.animate().rotation(-30).start();
        } else if (voteStatus == Post.VoteStatus.DOWNVOTED) {
            Animation slide = new TranslateAnimation(Animation.RELATIVE_TO_PARENT, 0.0f,
                    Animation.RELATIVE_TO_PARENT, 0.0f, Animation.RELATIVE_TO_PARENT,
                    0.0f, Animation.RELATIVE_TO_PARENT, 5.2f);
            slide.setDuration(751);

            topPostView.startAnimation(slide);
            topPostView.animate().rotation(30).start();
        }

        mBaseFrameLayout.removeView(topPostView);

        mClient.updateLikeStatus(topPost.getId(), voteStatus, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers,
                                  byte[] responseBody) {
                Log.d("UPDATE", "Successful");
            }

            @Override
            public void onFailure(int statusCode, Header[] headers,
                                  byte[] responseBody, Throwable error) {
                try {
                    Log.d(Integer.toString(statusCode), new String(responseBody));
                } catch (Exception e) {
                    e.printStackTrace();
                }
                Log.d("UPDATE", "Failed");
            }
        });

>>>>>>> cd6a104d641cbb3b8fc14a6ea295f58f3378baa9
    }
}
