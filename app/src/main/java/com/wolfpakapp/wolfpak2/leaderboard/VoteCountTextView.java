package com.wolfpakapp.wolfpak2.leaderboard;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.os.Handler;
import android.os.Vibrator;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.animation.OvershootInterpolator;
import android.widget.TextView;

import com.loopj.android.http.AsyncHttpResponseHandler;
import com.wolfpakapp.wolfpak2.Post;
import com.wolfpakapp.wolfpak2.R;
import com.wolfpakapp.wolfpak2.VoteStatus;

import org.apache.http.Header;

public class VoteCountTextView extends TextView {
    private static final String TAG = "VoteCountTextView";

    private LeaderboardTabManager mManager = null;
    private Post mPost = null;
    private View parentItemView = null;
    private boolean isTouchEnabled = false;

    private float initialViewX = 0;
    private float initialViewY = 0;

    private float lastTouchX = 0;
    private float lastTouchY = 0;

    public VoteCountTextView(Context context) {
        super(context);
    }

    public VoteCountTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public VoteCountTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void initialize(LeaderboardTabManager manager, Post post, View parentItemView) {
        mManager = manager;
        mPost = post;
        this.parentItemView = parentItemView;
        refresh();

        if (manager.getTag().equals(LeaderboardFragment.LOCAL_TAG)) {
            // If this is in the local leaderboard, then enable the onTouchEvent() method.
            isTouchEnabled = true;
        } else {
            setBackgroundResource(R.drawable.vote_count_cant_vote);
        }
    }

    /**
     * Sets the background color so that it corresponds to the given VoteStatus.
     *
     * @param voteStatus The vote status that determines the background
     */
    private void setBackground(VoteStatus voteStatus) {
        switch (voteStatus) {
            case UPVOTED: {
                setBackgroundResource(R.drawable.vote_count_up_voted);
                break;
            }
            case DOWNVOTED: {
                setBackgroundResource(R.drawable.vote_count_down_voted);
                break;
            }
            case NOT_VOTED:
            default: {
                setBackgroundResource(R.drawable.vote_count_not_voted);
                break;
            }
        }
    }

    /**
     * Refresh the vote count so that the correct vote count and background are displayed.
     */
    private void refresh() {
        setText(Integer.toString(mPost.getUpdatedVoteCount()));
        setBackground(mPost.getVoteStatus());
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!isTouchEnabled) {
//            vibrateOnError();

            return true;
        }

        final int action = MotionEventCompat.getActionMasked(event);
        switch (action) {
            case MotionEvent.ACTION_DOWN: {
                // Only one item can be interacted with at a time.
                if (mManager.isItemSelected()) {
                    return false;
                }
                mManager.setIsItemSelected(true);

                // Ensure that the vote count receives all touch events.
                mManager.getSwipeRefreshLayout().setEnabled(false);
                mManager.requestDisallowInterceptTouchEventForParents(this, true);

                setClipChildrenForParents(false);

                initialViewX = getX();
                initialViewY = getY();

                lastTouchX = event.getRawX();
                lastTouchY = event.getRawY();

                return true;
            }


            case MotionEvent.ACTION_MOVE: {
                // Was originally under onInteractionStart, but a simultaneous tap on two views
                // causes the application to crash due to the RecyclerView (for some reason)
                // being unable to draw the elements. Now, only when the view has been moved
                // is the drawing oder of the RecyclerView modified.
                if (!mManager.isNewDrawingOrderSet()) {
                    mManager.setIsNewDrawingOrderSet(true);
                    RecyclerView recyclerView = mManager.getRecyclerView();
                    final int indexOfFrontChild = recyclerView.indexOfChild(parentItemView);
                    // Needed so that the vote count can be drawn over sibling post views.
                    recyclerView.setChildDrawingOrderCallback(new RecyclerView
                            .ChildDrawingOrderCallback() {
                        private int nextChildIndexToRender;

                        @Override
                        public int onGetChildDrawingOrder(int childCount, int iteration) {
                            if (iteration == childCount - 1) {
                                nextChildIndexToRender = 0;
                                return indexOfFrontChild;
                            } else {
                                if (nextChildIndexToRender == indexOfFrontChild) {
                                    nextChildIndexToRender++;
                                }
                                return nextChildIndexToRender++;
                            }
                        }
                    });
                    recyclerView.invalidate();
                }

                // Calculate how much the pointer has moved, and move the vote count as much.
                final float x = event.getRawX();
                final float y = event.getRawY();
                final float dx = x - lastTouchX;
                final float dy = y - lastTouchY;
                setX(getX() + dx);
                setY(getY() + dy);

                // Change the COLOR of the vote count, but do NOT save the status yet!
                if (getY() < initialViewY) {
                    if (mPost.getVoteStatus() == VoteStatus.UPVOTED) {
                        setBackground(VoteStatus.NOT_VOTED);
                    } else {
                        setBackground(VoteStatus.UPVOTED);
                    }
                } else if (getY() > initialViewY) {
                    if (mPost.getVoteStatus() == VoteStatus.DOWNVOTED) {
                        setBackground(VoteStatus.NOT_VOTED);
                    } else {
                        setBackground(VoteStatus.DOWNVOTED);
                    }
                }

                lastTouchX = x;
                lastTouchY = y;

                return true;
            }
            case MotionEvent.ACTION_POINTER_UP: {
                return true;
            }
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP: {
                // Determine what the new VoteStatus of the post should be.
                final VoteStatus newStatus;
                if (getY() < initialViewY) {
                    if (mPost.getVoteStatus() == VoteStatus.UPVOTED) {
                        newStatus = VoteStatus.NOT_VOTED;
                    } else {
                        newStatus = VoteStatus.UPVOTED;
                    }
                } else if (getY() > initialViewY) {
                    if (mPost.getVoteStatus() == VoteStatus.DOWNVOTED) {
                        newStatus = VoteStatus.NOT_VOTED;
                    } else {
                        newStatus = VoteStatus.DOWNVOTED;
                    }
                } else {
                    // Don't change the vote status (this case should rarely happen).
                    newStatus = mPost.getVoteStatus();
                }
                final VoteStatus oldStatus = mPost.getVoteStatus();
                mPost.setVoteStatus(newStatus);
                refresh();

                mManager.getServerRestClient().updateLikeStatus(mPost.getId(), newStatus,
                        new AsyncHttpResponseHandler() {
                            @Override
                            public void onSuccess(int statusCode, Header[] headers,
                                                  byte[] responseBody) {
                                // Do nothing. The appearance was already changed.
                            }

                            @Override
                            public void onFailure(int statusCode, Header[] headers,
                                                  byte[] responseBody, Throwable error) {
                                // If unsuccessful, revert the post to the old look.
                                mPost.setVoteStatus(oldStatus);
                                refresh();

                                try {
                                    Log.d(TAG, "Failure: " + Integer.toString(statusCode));
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        });

                // Animate the vote count returning to its position.
                AnimatorSet animatorSet = new AnimatorSet();
                ObjectAnimator xAnim = ObjectAnimator.ofFloat(this, "X", this.getX(), initialViewX);
                ObjectAnimator yAnim = ObjectAnimator.ofFloat(this, "Y", this.getY(), initialViewY);
                animatorSet.play(xAnim).with(yAnim);
                animatorSet.setDuration(350);
                animatorSet.setInterpolator(new OvershootInterpolator(1.4f));
                animatorSet.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        // Enable the SwipeRefreshLayout when appropriate.
                        mManager.safeEnableSwipeRefreshLayout();
                        mManager.requestDisallowInterceptTouchEventForParents(VoteCountTextView.this,
                                false);

                        // Reset the drawing order of the RecyclerView.
                        mManager.setIsNewDrawingOrderSet(false);
                        RecyclerView recyclerView = mManager.getRecyclerView();
                        recyclerView.setChildDrawingOrderCallback(null);
                        recyclerView.invalidate();

                        mManager.setIsItemSelected(false);
                        setClipChildrenForParents(true);
                    }

                    @Override
                    public void onAnimationCancel(Animator animation) {
                        onAnimationEnd(animation);
                    }
                });

                animatorSet.start();

                return true;
            }
        }

        return false;
    }

    /**
     * Call setClipChildren on (almost) all parents of this view. However, the
     * SwipeRefreshLayout is never modified (for rendering purposes).
     *
     * @param clipChildren True to clip children to the bounds.
     */
    private void setClipChildrenForParents(boolean clipChildren) {
        ViewParent parent = getParent();
        while (parent instanceof ViewGroup) {
            // This is a bandage solution to fix the issue where if the SwipeRefreshLayout
            // is set to false, then the entire post view covers the tab widget...
            if (!(parent instanceof SwipeRefreshLayout)) {
                ((ViewGroup) parent).setClipChildren(clipChildren);
            }
            parent = parent.getParent();
        }
    }

//    private static boolean isVibrating = false;
//    private static final long VIBRATE_DURATION = 200;
//
//    private void vibrateOnError() {
//        Vibrator vibrator = (Vibrator) getContext().getSystemService(Context.VIBRATOR_SERVICE);
//        if (vibrator.hasVibrator() && !isVibrating) {
//            isVibrating = true;
//            vibrator.vibrate(VIBRATE_DURATION);
//            new Handler().postDelayed(new Runnable() {
//                @Override
//                public void run() {
//                    isVibrating = false;
//                }
//            }, VIBRATE_DURATION);
//        }
//    }
}
