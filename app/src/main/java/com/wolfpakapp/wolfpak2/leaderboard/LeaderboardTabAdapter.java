package com.wolfpakapp.wolfpak2.leaderboard;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Interpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.VideoView;

import com.loopj.android.http.AsyncHttpResponseHandler;
import com.squareup.picasso.Picasso;
import com.wolfpakapp.wolfpak2.Post;
import com.wolfpakapp.wolfpak2.R;
import com.wolfpakapp.wolfpak2.service.ServerRestClient;
import com.wolfpakapp.wolfpak2.WolfpakServiceProvider;

import org.apache.http.Header;

import java.util.ArrayList;

/**
 * The LeaderboardTabAdapter provides a binding from the list of Posts to the RecyclerView. It
 * contains the ViewHolder class that is responsible for each post's view.
 */
public class LeaderboardTabAdapter extends RecyclerView.Adapter<LeaderboardTabAdapter.ViewHolder> {
    private ArrayList<Post> mPosts;
    private LeaderboardTabManager mParentManager;

    private Animator mCurrentAnimator;
    private boolean isItemSelected = false;
    private boolean isNewDrawingOrderSet = false;

    private ServerRestClient mClient;

    public LeaderboardTabAdapter(ArrayList<Post> posts,
                                 LeaderboardTabManager parentManager) {
        mPosts = posts;
        mParentManager = parentManager;

        mClient = (ServerRestClient) WolfpakServiceProvider
                .getServiceManager(WolfpakServiceProvider.SERVERRESTCLIENT);
    }

    @Override
    public int getItemCount() {
        return mPosts.size();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_leaderboard, parent, false);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        holder.bindPost(mPosts.get(position));
    }

    public LeaderboardTabManager getParentManager() {
        return mParentManager;
    }

    /**
     * @return True if an item in the tab was selected.
     */
    public boolean isItemSelected() {
        return isItemSelected;
    }

    /**
     * The ViewHolder class describes the view for each post and configures behaviors for the
     * different elements in each post view.
     */
    public class ViewHolder extends RecyclerView.ViewHolder {
        private Post mPost;

        private TextView handleTextView;
        private TextView voteCountTextView;

        private ImageView thumbnailImageView;

        public ViewHolder(View itemView) {
            super(itemView);

            handleTextView = (TextView) itemView
                    .findViewById(R.id.leaderboard_item_handle_text_view);
            voteCountTextView = (TextView) itemView
                    .findViewById(R.id.leaderboard_item_view_count_text_view);
            thumbnailImageView = (ImageView) itemView
                    .findViewById(R.id.leaderboard_item_thumbnail_image_view);
        }

        public void bindPost(Post post) {
            mPost = post;

            handleTextView.setText(post.getHandle());

            updateVoteCountBackground(post.getVoteStatus());
            // The view count TextViews can not be interacted with in the den
            if (!mParentManager.getTag().equals(LeaderboardFragment.DEN_TAG)) {
                voteCountTextView.setOnTouchListener(new VoteCountOnTouchListener());
            }

            if (post.isImage()) {
                Picasso.with(mParentManager.getParentActivity()).load(post.getMediaUrl())
                        .into(thumbnailImageView);
            } else {
                // Overlay a play icon on top of the video thumbnail
                Drawable thumbnailDrawable;
                Drawable overlayDrawable;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    //TODO Retrieve thumbnails from the server
                    thumbnailDrawable = mParentManager.getParentActivity()
                            .getDrawable(R.drawable.empty_video_icon);
                    overlayDrawable = mParentManager.getParentActivity()
                            .getDrawable(R.drawable.play_icon);
                } else {
                    thumbnailDrawable = mParentManager.getParentActivity().getResources()
                            .getDrawable(R.drawable.empty_video_icon);
                    overlayDrawable = mParentManager.getParentActivity().getResources()
                            .getDrawable(R.drawable.play_icon);

                }
                Drawable[] layers = {thumbnailDrawable, overlayDrawable};
                thumbnailImageView.setImageDrawable(new LayerDrawable(layers));
                thumbnailImageView.setBackgroundColor(Color.BLACK);
            }

            thumbnailImageView.setOnClickListener(new ThumbnailOnClickListener());
        }

        /**
         * Update the vote count background so that it corresponds to the passed VoteStatus. Note
         * that this does NOT change the Post's actual VoteStatus. Also ensure that
         * the vote count matches the Post's updated vote count.
         * @param voteStatus
         */
        public void updateVoteCountBackground(Post.VoteStatus voteStatus) {
            GradientDrawable bg;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                bg = (GradientDrawable) voteCountTextView.getResources()
                        .getDrawable(R.drawable.background_view_count, null);
            } else {
                bg = (GradientDrawable) voteCountTextView.getResources()
                        .getDrawable(R.drawable.background_view_count);
            }

            int statusColor = voteStatus.getStatusColor(mParentManager.getParentActivity());
            if (bg != null) {
                bg.setColor(statusColor);
                voteCountTextView.setBackground(bg);
            }

            voteCountTextView.setText(Integer.toString(mPost.getUpdatedVoteCount()));

            voteCountTextView.invalidate();
        }

        /**
         * Call requestDisallowInterceptTouchEvent() on all parents of the view.
         * @param v The child view.
         * @param disallowIntercept True to stop the parent from intercepting touch events.
         * TODO Identical to MainFeedFragment method!
         */
        private void requestDisallowInterceptTouchEventForParents(View v,
                                                                  boolean disallowIntercept) {
            ViewParent parent = v.getParent();
            while (parent != null) {
                parent.requestDisallowInterceptTouchEvent(disallowIntercept);
                parent = parent.getParent();
            }

        }

        /**
         * The ViewCountOnTouchListener handles touch events on the vote count.
         */
        private final class VoteCountOnTouchListener implements View.OnTouchListener {
            private SwipeRefreshLayout mLayout = mParentManager.getTabLayout();

            private float initialViewX;
            private float initialViewY;

            private float lastTouchX = 0;
            private float lastTouchY = 0;

            private final int ANIM_DURATION = 350;
            private Interpolator INTERPOLATOR = new OvershootInterpolator(1.4f);

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                final int action = MotionEventCompat.getActionMasked(event);

                switch (action) {
                    case MotionEvent.ACTION_DOWN: {
                        // Only one item can be selected at a time
                        if (isItemSelected) {
                            return false;
                        }
                        isItemSelected = true;

                        // Ensure that the SwipeRefreshLayout is disabled... (is this still needed?)
                        mLayout.setEnabled(false);
                        setClipChildrenForParents(voteCountTextView, false);
                        requestDisallowInterceptTouchEventForParents(voteCountTextView, true);

                        lastTouchX = event.getRawX();
                        lastTouchY = event.getRawY();

                        initialViewX = voteCountTextView.getX();
                        initialViewY = voteCountTextView.getY();

                        return true;
                    }
                    case MotionEvent.ACTION_MOVE: {
                        // Was originally under ACTION_DOWN, but a simultaneous tap on two elements
                        // causes the application to crash due to the RecyclerView (for some reason)
                        // being unable to draw the elements. Now, only when the view has been moved
                        // will the drawing oder of the RecyclerView be modified
                        if (!isNewDrawingOrderSet) {
                            RecyclerView recyclerView = mParentManager.getRecyclerView();
                            final int indexOfFrontChild = recyclerView.indexOfChild(itemView);
                            Log.d("Index",Integer.toString(indexOfFrontChild));
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
                            isNewDrawingOrderSet = true;
                            // NEEDED SO THE APP DOESN'T CRASH
                            recyclerView.invalidate();
                        }

                        final float x = event.getRawX();
                        final float y = event.getRawY();

                        final float dx = x - lastTouchX;
                        final float dy = y - lastTouchY;

                        // Calculate how much the pointer has moved, and move the vote count as much.
                        voteCountTextView.setX(voteCountTextView.getX() + dx);
                        voteCountTextView.setY(voteCountTextView.getY() + dy);

                        // Change the COLOR of the vote count, but do NOT save the status yet!
                        if (voteCountTextView.getY() < initialViewY) {
                            if (mPost.getVoteStatus() == Post.VoteStatus.UPVOTED) {
                                updateVoteCountBackground(Post.VoteStatus.NOT_VOTED);
                            } else {
                                updateVoteCountBackground(Post.VoteStatus.UPVOTED);
                            }
                        } else if (voteCountTextView.getY() > initialViewY) {
                            if (mPost.getVoteStatus() == Post.VoteStatus.DOWNVOTED) {
                                updateVoteCountBackground(Post.VoteStatus.NOT_VOTED);
                            } else {
                                updateVoteCountBackground(Post.VoteStatus.DOWNVOTED);
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
                        // Ensure that the SwipeRefreshLayout is not prematurely enabled.
                        mParentManager.toggleSwipeRefreshLayout();
                        requestDisallowInterceptTouchEventForParents(voteCountTextView, false);

                        // Determine the new VoteStatus of the post.
                        final Post.VoteStatus newStatus;
                        if (voteCountTextView.getY() < initialViewY) {
                            if (mPost.getVoteStatus() == Post.VoteStatus.UPVOTED) {
                                newStatus = Post.VoteStatus.NOT_VOTED;
                            } else {
                                newStatus = Post.VoteStatus.UPVOTED;
                            }
                        } else if (voteCountTextView.getY() > initialViewY) {
                            if (mPost.getVoteStatus() == Post.VoteStatus.DOWNVOTED) {
                                newStatus = Post.VoteStatus.NOT_VOTED;
                            } else {
                                newStatus = Post.VoteStatus.DOWNVOTED;
                            }
                        } else {
                            // Don't change the vote status (this case should rarely happen).
                            newStatus = mPost.getVoteStatus();
                        }

                        mClient.updateLikeStatus(mPost.getId(), newStatus,
                                new AsyncHttpResponseHandler() {
                            @Override
                            public void onSuccess(int statusCode, Header[] headers,
                                                  byte[] responseBody) {
                                // If successful, locally update the vote status and background.
                                mPost.setVoteStatus(newStatus);
                                updateVoteCountBackground(newStatus);
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
                                updateVoteCountBackground(mPost.getVoteStatus());
                            }
                        });

                        // Animate the vote count returning to its position.
                        AnimatorSet animatorSet = new AnimatorSet();
                        ObjectAnimator xAnim = ObjectAnimator.ofFloat(voteCountTextView, "X",
                                voteCountTextView.getX(), initialViewX);
                        ObjectAnimator yAnim = ObjectAnimator.ofFloat(voteCountTextView, "Y",
                                voteCountTextView.getY(), initialViewY);
                        animatorSet.play(xAnim).with(yAnim);
                        animatorSet.setDuration(ANIM_DURATION);
                        animatorSet.setInterpolator(INTERPOLATOR);
                        animatorSet.addListener(new AnimatorListenerAdapter() {
                            @Override
                            public void onAnimationEnd(Animator animation) {
                                // Reset the drawing order of the RecyclerView.
                                RecyclerView recyclerView = mParentManager.getRecyclerView();
                                recyclerView.setChildDrawingOrderCallback(null);
                                recyclerView.invalidate();
                                isNewDrawingOrderSet = false;

                                setClipChildrenForParents(voteCountTextView, true);
                                // No item is being selected anymore.
                                isItemSelected = false;
                            }
                        });

                        animatorSet.start();
                        return true;
                    }
                }

                return false;
            }

            /**
             * Call setClipChildren on (almost) all parents of the view. However, the
             * SwipeRefreshLayout is never modified.
             * @param v The child view.
             * @param clipChildren True to clip children to the bounds.
             */
            private void setClipChildrenForParents(View v, boolean clipChildren) {
                ViewParent parent = v.getParent();
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

        /**
         * The ThumbnailOnClickListener handles clicks on the thumbnail.
         * TODO Fix bug - can swipe left/right while view is expanding
         */
        private final class ThumbnailOnClickListener implements View.OnClickListener {
            private FrameLayout baseFrameLayout = mParentManager.getParentFragment()
                    .getBaseFrameLayout();

            private ImageView animatingView;

            private Rect startBounds;
            private Rect finalBounds;

            private int thumbnailSize;

            private final int ANIM_DURATION = 500;
            private final Interpolator INTERPOLATOR = new AccelerateDecelerateInterpolator();

            @Override
            public void onClick(View v) {
                // Do not respond if another element is selected.
                if (isItemSelected) {
                    return;
                }
                isItemSelected = true;
                if (mPost.isImage()) {
                    // Create an expanded ImageView.
                    ImageView expandedImageView = new ImageView(mParentManager.getParentActivity());

                    expandedImageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                    expandedImageView.setVisibility(View.GONE);
                    expandedImageView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN);
                    Picasso.with(mParentManager.getParentActivity()).load(mPost.getMediaUrl())
                            .into(expandedImageView);
                    baseFrameLayout.addView(expandedImageView);

                    animateViewExpansion(expandedImageView);
                } else {
                    // Create an expanded VideoView.
                    VideoView expandedVideoView = new VideoView(mParentManager.getParentActivity());
                    expandedVideoView.setVisibility(View.GONE);
                    expandedVideoView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN);
                    expandedVideoView.setVideoPath(mPost.getMediaUrl());

                    baseFrameLayout.addView(expandedVideoView);

                    animateViewExpansion(expandedVideoView);
                }
            }

            /**
             * Animate the expansion of the thumbnail.
             * @param expandedView The expanded View.
             */
            private void animateViewExpansion(final View expandedView) {
                // The animating view is used purely for the animation.
                animatingView = new ImageView(mParentManager.getParentActivity());
                animatingView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                animatingView.setVisibility(View.GONE);
                animatingView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
                animatingView.setImageDrawable(thumbnailImageView.getDrawable());
                baseFrameLayout.addView(animatingView);

                startBounds = new Rect();
                finalBounds = new Rect();
                thumbnailSize = (int) mParentManager.getParentActivity().getResources()
                        .getDimension(R.dimen.leaderboard_item_thumbnail_size);

                // Determine the start and final bounds for the animating view.
                thumbnailImageView.getGlobalVisibleRect(startBounds);
                mParentManager.getParentActivity().getWindow().getDecorView()
                        .getGlobalVisibleRect(finalBounds);

                if (mCurrentAnimator != null) {
                    mCurrentAnimator.cancel();
                }

                animatingView.setVisibility(ImageView.VISIBLE);
                animatingView.setPivotX(0f);
                animatingView.setPivotY(0f);

                ValueAnimator widthAnimator = ValueAnimator.ofInt(thumbnailSize, finalBounds.width());
                widthAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                    @Override
                    public void onAnimationUpdate(ValueAnimator animation) {
                        animatingView.getLayoutParams().width = (int) animation.getAnimatedValue();
                        animatingView.requestLayout();
                    }
                });
                ValueAnimator heightAnimator = ValueAnimator.ofInt(thumbnailSize, finalBounds.height());
                heightAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                    @Override
                    public void onAnimationUpdate(ValueAnimator animation) {
                        animatingView.getLayoutParams().height = (int) animation.getAnimatedValue();
                        animatingView.requestLayout();
                    }
                });

                AnimatorSet set = new AnimatorSet();
                set.playTogether(ObjectAnimator
                                .ofFloat(animatingView, View.X, startBounds.left, finalBounds.left),
                        ObjectAnimator
                                .ofFloat(animatingView, View.Y, startBounds.top, finalBounds.top),
                        widthAnimator, heightAnimator);
                set.setDuration(ANIM_DURATION);
                set.setInterpolator(INTERPOLATOR);
                set.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        thumbnailImageView.setVisibility(View.INVISIBLE);
                        expandedView.setVisibility(View.VISIBLE);
                        animatingView.setVisibility(View.GONE);

                        if (mPost.isImage()) {
                            expandedView
                                    .setOnTouchListener(new ExpandedViewOnTouchListener());
                        } else {
                            // Do not set the OnTouchListener until the video is completed.
                            ((VideoView) expandedView).setOnCompletionListener(new MediaPlayer
                                    .OnCompletionListener() {
                                @Override
                                public void onCompletion(MediaPlayer mp) {
                                    expandedView.setOnTouchListener(new ExpandedViewOnTouchListener());
                                }
                            });
                            ((VideoView) expandedView).start();
                        }
                    }
                });

                set.start();
                mCurrentAnimator = set;
            }

            /**
             * The ExpandedViewOnTouchListener handles touch events on the expanded view. The drag
             * implementation is similar to that of the vote count.
             * @see VoteCountOnTouchListener
             */
            private final class ExpandedViewOnTouchListener implements View.OnTouchListener {
                private float lastTouchX = 0;
                private float lastTouchY = 0;

                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    final int action = MotionEventCompat.getActionMasked(event);

                    switch (action) {
                        case MotionEvent.ACTION_DOWN: {
                            // Make sure that the SwipeRefreshLayout is disabled.
                            mParentManager.getTabLayout().setEnabled(false);
                            // May not be necessary...
                            requestDisallowInterceptTouchEventForParents(v, true);

                            lastTouchX = event.getRawX();
                            lastTouchY = event.getRawY();

                            return true;
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

                            return true;
                        }
                        case MotionEvent.ACTION_POINTER_UP: {
                            return true;
                        }
                        case MotionEvent.ACTION_CANCEL:
                        case MotionEvent.ACTION_UP: {
                            mParentManager.getTabLayout().setEnabled(true);
                            mParentManager.getRecyclerView().setEnabled(true);

                            // May not be necessary...
                            requestDisallowInterceptTouchEventForParents(v, false);

                            // Recalculate the finalBounds as the expanded View may have been dragged
                            finalBounds = new Rect((int) v.getX(), (int) v.getY(),(int) v.getX() + v.getWidth(),
                                    (int) v.getY() + v.getHeight());

                            animateViewShrinking(v);

                            return true;
                        }
                    }

                    return false;
                }
            }

            /**
             * Animate the shrinking of the expanded View.
             * @param expandedView The expanded View.
             */
            private void animateViewShrinking(View expandedView) {
                mParentManager.toggleSwipeRefreshLayout();

                if (mCurrentAnimator != null) {
                    mCurrentAnimator.cancel();
                }

                animatingView.setVisibility(View.VISIBLE);
                expandedView.setVisibility(View.GONE);

                ValueAnimator widthAnimator = ValueAnimator.ofInt(finalBounds.width(), thumbnailSize);
                widthAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                    @Override
                    public void onAnimationUpdate(ValueAnimator animation) {
                        animatingView.getLayoutParams().width = (int) animation.getAnimatedValue();
                        animatingView.requestLayout();
                    }
                });
                ValueAnimator heightAnimator = ValueAnimator.ofInt(finalBounds.height(), thumbnailSize);
                heightAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                    @Override
                    public void onAnimationUpdate(ValueAnimator animation) {
                        animatingView.getLayoutParams().height = (int) animation.getAnimatedValue();
                        animatingView.requestLayout();
                    }
                });

                AnimatorSet set = new AnimatorSet();
                set.playTogether(ObjectAnimator
                                .ofFloat(animatingView, View.X, finalBounds.left, startBounds.left),
                        ObjectAnimator
                                .ofFloat(animatingView, View.Y, finalBounds.top, startBounds.top),
                        widthAnimator, heightAnimator);
                set.setDuration(ANIM_DURATION);
                set.setInterpolator(INTERPOLATOR);
                set.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        thumbnailImageView.setVisibility(View.VISIBLE);
                        mCurrentAnimator = null;
                        // The item is no longer selected.
                        isItemSelected = false;
                        animatingView.setVisibility(View.GONE);
                    }
                });

                set.start();
                mCurrentAnimator = set;
            }
        }
    }
}
