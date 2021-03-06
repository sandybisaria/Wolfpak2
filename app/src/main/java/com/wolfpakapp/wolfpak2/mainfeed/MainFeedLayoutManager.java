package com.wolfpakapp.wolfpak2.mainfeed;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.support.v4.view.MotionEventCompat;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.RelativeLayout;

import com.wolfpakapp.wolfpak2.Post;
import com.wolfpakapp.wolfpak2.VoteStatus;

import java.util.ArrayDeque;
import java.util.ArrayList;

/**
 * The MainFeedLayoutManager manages the layout of the main feed. It is responsible for adding and
 * removing the MediaViews.
 */
public class MainFeedLayoutManager {
    private RelativeLayout baseLayout;
    private MainFeedFragment mainFeed;

    private MainFeedNetworkingManager networkingManager;

    private ArrayDeque<MediaView> mediaViewArrayDeque = new ArrayDeque<>();

    public MainFeedLayoutManager(MainFeedFragment mainFeed) {
        this.mainFeed = mainFeed;
    }

    /**
     * Load the views using the posts from the server.
     *
     * @param postArrayDeque The ArrayDeque of posts (from the NetworkingManager).
     */
    public void loadViews(ArrayDeque<Post> postArrayDeque) {
        networkingManager = mainFeed.getNetworkingManager();
        baseLayout = mainFeed.getBaseLayout();

        // Instantiate a MediaView object for each post.
        ArrayList<MediaView> mediaViewList = new ArrayList<>();
        for (Post post : postArrayDeque) {
            MediaView mediaView = new MediaView(mainFeed.getActivity());
            mediaView.setContent(post);
            mediaViewList.add(mediaView);
            mediaViewArrayDeque.addLast(mediaView);
        }

        // Add posts to the layout in reverse order (so that the topmost post is added first).
        for (int idx = mediaViewList.size() - 1; idx >= 0; idx--) {
            baseLayout.addView(mediaViewList.get(idx));
        }

        mainFeed.bringButtonsToFront();

        mediaViewArrayDeque.peekFirst().start();

        mainFeed.addCallbacks(new MainFeedFragment.OnVisibilityChangeCallbacks() {
            @Override
            public void onBecomesVisible() {

            }

            @Override
            public void onBecomesInvisible() {
                // When the fragment is no longer visible, return the top post back to the starting
                // position.
                returnToPosition();
            }
        });

        // Give the MediaViews enough time to load the content before adding the OnTouchListener.
        for (MediaView mediaView : mediaViewList) {
            mediaView.setOnTouchListener(new HowlOnTouchListener());
        }
    }

    /**
     * Animate the topmost view so that it slides up and disappears, and then remove it from layout.
     */
    private void slideToTop() {
        View view = mediaViewArrayDeque.peekFirst();
        if (view != null) {
            Animation slide;
            slide = new TranslateAnimation(Animation.RELATIVE_TO_PARENT, 0.0f,
                    Animation.RELATIVE_TO_PARENT, 0.0f, Animation.RELATIVE_TO_PARENT,
                    0.0f, Animation.RELATIVE_TO_PARENT, -5.0f);
            slide.setDuration(750);

            view.startAnimation(slide);
            view.animate().rotation(-30).start();

            baseLayout.removeView(view);
        }
    }

    /**
     * Animate the topmost view so that it slides down and disappears, and then remove it from layout.
     */
    private void slideToBottom() {
        View view = mediaViewArrayDeque.peekFirst();
        if (view != null) {
            Animation slide;
            slide = new TranslateAnimation(Animation.RELATIVE_TO_PARENT, 0.0f,
                    Animation.RELATIVE_TO_PARENT, 0.0f, Animation.RELATIVE_TO_PARENT,
                    0.0f, Animation.RELATIVE_TO_PARENT, 5.0f);
            slide.setDuration(750);

            view.startAnimation(slide);
            view.animate().rotation(30).start();

            baseLayout.removeView(view);
        }
    }

    /**
     * Return the topmost view to its original position.
     */
    private void returnToPosition() {
        View view = mediaViewArrayDeque.peekFirst();
        if (view != null) {
            AnimatorSet set = new AnimatorSet();
            set.playTogether(ObjectAnimator
                            .ofFloat(view, View.X, view.getX(), 0f),
                    ObjectAnimator
                            .ofFloat(view, View.Y, view.getY(), 0f));
            set.setDuration(500);
            set.setInterpolator(new AccelerateDecelerateInterpolator());
            set.start();
        }
    }

    /**
     * The HowlOnTouchListener is the OnTouchListener for every post (howl).
     */
    public final class HowlOnTouchListener implements View.OnTouchListener {

        private float initialTouchY = 0;

        private float lastTouchX = 0;
        private float lastTouchY = 0;

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            final int action = MotionEventCompat.getActionMasked(event);

            switch (action) {
                case MotionEvent.ACTION_DOWN: {
                    initialTouchY = event.getRawY();

                    lastTouchX = event.getRawX();
                    lastTouchY = event.getRawY();

                    break;
                }
                case MotionEvent.ACTION_MOVE: {
                    final float x = event.getRawX();
                    final float y = event.getRawY();

                    final float dx = x - lastTouchX;
                    final float dy = y - lastTouchY;

                    v.setX(v.getX() + dx);
                    v.setY(v.getY() + dy);

                    lastTouchX = x;
                    lastTouchY = y;

                    MediaView mediaView = (MediaView) v;
                    if (isUpvoting()) {
                        mediaView.setTint(VoteStatus.UPVOTED);
                    } else if (isDownvoting()) {
                        mediaView.setTint(VoteStatus.DOWNVOTED);
                    } else {
                        mediaView.setTint(VoteStatus.NOT_VOTED);
                    }
                    break;
                }
                case MotionEvent.ACTION_POINTER_UP:
                case MotionEvent.ACTION_CANCEL:
                    break;
                case MotionEvent.ACTION_UP: {
                    MediaView mediaView = (MediaView) v;
                    if (isUpvoting()) {
                        networkingManager.updateLikeStatus(VoteStatus.UPVOTED);
                        mediaView.setTint(VoteStatus.UPVOTED);
                        slideToTop();
                        mediaViewArrayDeque.pollFirst();

                        if (mediaViewArrayDeque.size() > 0) {
                            mediaViewArrayDeque.peekFirst().start();
                        }
                    } else if (isDownvoting()) {
                        networkingManager.updateLikeStatus(VoteStatus.DOWNVOTED);
                        mediaView.setTint(VoteStatus.DOWNVOTED);
                        slideToBottom();
                        mediaViewArrayDeque.pollFirst();

                        if (mediaViewArrayDeque.size() > 0) {
                            mediaViewArrayDeque.peekFirst().start();
                        }
                    } else {
                        returnToPosition();
                        mediaView.setTint(VoteStatus.NOT_VOTED);
                    }

                    if (mediaViewArrayDeque.size() == 0) {
                        //TODO Do we want auto-refresh?
//                        networkingManager.getHowls();
                    }

                    break;
                }
            }
            return true;
        }

        private final float THRESHOLD = 300;

        private boolean isUpvoting() {
            return lastTouchY - initialTouchY < -THRESHOLD;
        }

        private boolean isDownvoting() {
            return lastTouchY - initialTouchY > THRESHOLD;
        }
    }
}
