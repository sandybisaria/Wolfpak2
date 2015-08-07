package com.wolfpakapp.wolfpak2.leaderboard;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.animation.OvershootInterpolator;
import android.widget.TextView;

import com.loopj.android.http.AsyncHttpResponseHandler;
import com.wolfpakapp.wolfpak2.Post;
import com.wolfpakapp.wolfpak2.R;

import org.apache.http.Header;

import java.util.ArrayList;

public class VoteCountTextView extends TextView {
    private LeaderboardTabManager mManager = null;
    private Post mPost = null;
    private boolean isTouchEnabled = false;

    private float initialViewX = 0;
    private float initialViewY = 0;

    private float lastTouchX = 0;
    private float lastTouchY = 0;

    private ArrayList<OnInteractingCallbacks> mCallbacks = new ArrayList<>();

    public VoteCountTextView(Context context) {
        super(context);
    }

    public VoteCountTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public VoteCountTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void initialize(LeaderboardTabManager manager, Post post) {
        mManager = manager;
        mPost = post;
        refresh();

        // If this is in the den, then disable the onTouchEvent() method.
        isTouchEnabled = !manager.getTag().equals(LeaderboardFragment.DEN_TAG);
    }

    /**
     * Sets the background color so that it corresponds to the given VoteStatus.
     * @param voteStatus
     */
    private void setBackgroundColor(Post.VoteStatus voteStatus) {
        GradientDrawable bg;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            bg = (GradientDrawable) getResources()
                    .getDrawable(R.drawable.background_vote_count, null);
        } else {
            bg = (GradientDrawable) getResources()
                    .getDrawable(R.drawable.background_vote_count);
        }

        int statusColor = voteStatus.getStatusColor(getContext());
        if (bg != null) {
            bg.setColor(statusColor);
            setBackground(bg);
        }

        invalidate();
    }

    /**
     * Refresh the vote count so that the correct vote count and background are displayed.
     */
    private void refresh() {
        setText(Integer.toString(mPost.getUpdatedVoteCount()));
        setBackgroundColor(mPost.getVoteStatus());
    }

    /**
     * Add a set of callbacks to be invoked when the view is being interacted with.
     * @param callback
     */
    public void addOnInteractingCallbacks(OnInteractingCallbacks callback) {
        mCallbacks.add(callback);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!isTouchEnabled) {
            return false;
        }

        final int action = MotionEventCompat.getActionMasked(event);
        switch (action) {
            case MotionEvent.ACTION_DOWN: {
                // Only one item can be interacted with at a time.
                if (mManager.isItemSelected()) {
                    return false;
                }
                mManager.setIsItemSelected(true);

                setClipChildrenForParents(false);

                for (OnInteractingCallbacks mCallback : mCallbacks) {
                    mCallback.onInteractionStart();
                }

                initialViewX = getX();
                initialViewY = getY();

                lastTouchX = event.getRawX();
                lastTouchY = event.getRawY();

                return true;
            }


            case MotionEvent.ACTION_MOVE: {
                for (OnInteractingCallbacks mCallback : mCallbacks) {
                    mCallback.onInteractionInProgress();
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
                    if (mPost.getVoteStatus() == Post.VoteStatus.UPVOTED) {
                        setBackgroundColor(Post.VoteStatus.NOT_VOTED);
                    } else {
                        setBackgroundColor(Post.VoteStatus.UPVOTED);
                    }
                } else if (getY() > initialViewY) {
                    if (mPost.getVoteStatus() == Post.VoteStatus.DOWNVOTED) {
                        setBackgroundColor(Post.VoteStatus.NOT_VOTED);
                    } else {
                        setBackgroundColor(Post.VoteStatus.DOWNVOTED);
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
                final Post.VoteStatus newStatus;
                if (getY() < initialViewY) {
                    if (mPost.getVoteStatus() == Post.VoteStatus.UPVOTED) {
                        newStatus = Post.VoteStatus.NOT_VOTED;
                    } else {
                        newStatus = Post.VoteStatus.UPVOTED;
                    }
                } else if (getY() > initialViewY) {
                    if (mPost.getVoteStatus() == Post.VoteStatus.DOWNVOTED) {
                        newStatus = Post.VoteStatus.NOT_VOTED;
                    } else {
                        newStatus = Post.VoteStatus.DOWNVOTED;
                    }
                } else {
                    // Don't change the vote status (this case should rarely happen).
                    newStatus = mPost.getVoteStatus();
                }

                mManager.getServerRestClient().updateLikeStatus(mPost.getId(), newStatus,
                        new AsyncHttpResponseHandler() {
                    @Override
                    public void onSuccess(int statusCode, Header[] headers,
                                          byte[] responseBody) {
                        // If successful, locally update the vote status and background.
                        mPost.setVoteStatus(newStatus);
                        refresh();
                    }

                    @Override
                    public void onFailure(int statusCode, Header[] headers,
                                          byte[] responseBody, Throwable error) {
                        try {
                            Log.d(Integer.toString(statusCode), new String(responseBody));
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        // If unsuccessful, the post's vote status is unchanged.
                        refresh();
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
                        for (OnInteractingCallbacks mCallback : mCallbacks) {
                            mCallback.onInteractionFinish();
                        }

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
}
