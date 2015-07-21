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
import com.wolfpakapp.wolfpak2.ServerLikeClient;

import org.apache.http.Header;

import java.util.ArrayList;

/**
 * The LeaderboardTabAdapter provides a binding from the list of LeaderboardListItems to the
 * RecyclerView. It contains the ViewHolder class that is responsible for each list item's view.
 */
public class LeaderboardTabAdapter extends RecyclerView.Adapter<LeaderboardTabAdapter.ViewHolder> {
    private ArrayList<Post> mPosts;
    private LeaderboardTabManager mParentManager;

    private Animator mCurrentAnimator;
    private boolean isItemSelected = false;
    private boolean isNewDrawingOrderSet = false;

    public LeaderboardTabAdapter(ArrayList<Post> posts,
                                 LeaderboardTabManager parentManager) {
        mPosts = posts;
        mParentManager = parentManager;
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
        holder.bindListItem(mPosts.get(position));
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
     * The ViewHolder class describes the view for each list item and configures behaviors for the
     * different elements in each item view.
     */
    public class ViewHolder extends RecyclerView.ViewHolder {
        private Post item;

        private TextView handleTextView;
        private TextView viewCountTextView;

        private ImageView thumbnailImageView;

        public ViewHolder(View itemView) {
            super(itemView);

            handleTextView = (TextView) itemView
                    .findViewById(R.id.leaderboard_item_handle_text_view);
            viewCountTextView = (TextView) itemView
                    .findViewById(R.id.leaderboard_item_view_count_text_view);
            thumbnailImageView = (ImageView) itemView
                    .findViewById(R.id.leaderboard_item_thumbnail_image_view);
        }

        public void bindListItem(Post item) {
            this.item = item;

            handleTextView.setText(item.getHandle());

            updateViewCountBackground(item.getVoteStatus());
            // The view count TextViews can not be interacted with in the den
            if (!mParentManager.getTag().equals(LeaderboardFragment.DEN_TAG)) {
                viewCountTextView.setOnTouchListener(new ViewCountOnTouchListener());
            }

            if (item.isImage()) {
                Picasso.with(mParentManager.getParentActivity()).load(item.getMediaUrl())
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
         * Update the view count background so that it corresponds to the passed VoteStatus. Note
         * that this does NOT change the LeaderboardListItem's actual VoteStatus. Also ensure that
         * the vote count matches the item's updated vote count.
         * @param voteStatus
         */
        public void updateViewCountBackground(Post.VoteStatus voteStatus) {
            GradientDrawable bg;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                bg = (GradientDrawable) viewCountTextView.getResources()
                        .getDrawable(R.drawable.background_view_count, null);
            } else {
                bg = (GradientDrawable) viewCountTextView.getResources()
                        .getDrawable(R.drawable.background_view_count);
            }

            int statusColor = voteStatus.getStatusColor(mParentManager.getParentActivity());
            if (bg != null) {
                bg.setColor(statusColor);
                viewCountTextView.setBackground(bg);
            }

            viewCountTextView.setText(Integer.toString(item.getUpdatedVoteCount()));

            viewCountTextView.invalidate();
        }

        public Post getListItem() {
            return item;
        }

        /**
         * Call requestDisallowInterceptTouchEvent() on all parents of the view.
         * @param v The child view.
         * @param disallowIntercept True to stop the parent from intercepting touch events.
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
        private final class ViewCountOnTouchListener implements View.OnTouchListener {
            private SwipeRefreshLayout mLayout = mParentManager.getTabLayout();

            private int activePointerId = MotionEvent.INVALID_POINTER_ID;

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

                        activePointerId = MotionEventCompat.getPointerId(event, 0);

                        // Ensure that the SwipeRefreshLayout is disabled... (is this still needed?)
                        mLayout.setEnabled(false);
                        setClipChildrenForParents(viewCountTextView, false);
                        requestDisallowInterceptTouchEventForParents(viewCountTextView, true);

                        lastTouchX = event.getRawX();
                        lastTouchY = event.getRawY();

                        initialViewX = viewCountTextView.getX();
                        initialViewY = viewCountTextView.getY();

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
                            // Needed so that the vote count can be drawn over sibling item views.
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
                        viewCountTextView.setX(viewCountTextView.getX() + dx);
                        viewCountTextView.setY(viewCountTextView.getY() + dy);

                        // Change the COLOR of the vote count, but do NOT save the status yet!
                        if (viewCountTextView.getY() < initialViewY) {
                            if (item.getVoteStatus() == Post.VoteStatus.UPVOTED) {
                                updateViewCountBackground(Post.VoteStatus.NOT_VOTED);
                            } else {
                                updateViewCountBackground(Post.VoteStatus.UPVOTED);
                            }
                        } else if (viewCountTextView.getY() > initialViewY) {
                            if (item.getVoteStatus() == Post.VoteStatus.DOWNVOTED) {
                                updateViewCountBackground(Post.VoteStatus.NOT_VOTED);
                            } else {
                                updateViewCountBackground(Post.VoteStatus.DOWNVOTED);
                            }
                        }

                        lastTouchX = x;
                        lastTouchY = y;

                        return true;
                    }
                    case MotionEvent.ACTION_POINTER_UP: {
                        // If the vote count is (somehow) tapped by two pointers and the original is
                        // lifted, then monitor the next pointer's movements.
                        final int pointerIndex = MotionEventCompat.getActionIndex(event);
                        final int pointerId = MotionEventCompat.findPointerIndex(event, pointerIndex);
                        if (pointerId == activePointerId) {
                            final int newPointerIndex = pointerIndex == 0 ? 1 : 0;
                            lastTouchX = event.getRawX();
                            lastTouchY = event.getRawY();
                            activePointerId = MotionEventCompat.getPointerId(event, newPointerIndex);
                        }

                        return true;
                    }

                    case MotionEvent.ACTION_CANCEL:
                    case MotionEvent.ACTION_UP: {
                        // Ensure that the SwipeRefreshLayout is not prematurely enabled.
                        mParentManager.toggleSwipeRefreshLayout();
                        requestDisallowInterceptTouchEventForParents(viewCountTextView, false);

                        // Determine the new VoteStatus of the list item.
                        final Post.VoteStatus newStatus;
                        if (viewCountTextView.getY() < initialViewY) {
                            if (item.getVoteStatus() == Post.VoteStatus.UPVOTED) {
                                newStatus = Post.VoteStatus.NOT_VOTED;
                            } else {
                                newStatus = Post.VoteStatus.UPVOTED;
                            }
                        } else if (viewCountTextView.getY() > initialViewY) {
                            if (item.getVoteStatus() == Post.VoteStatus.DOWNVOTED) {
                                newStatus = Post.VoteStatus.NOT_VOTED;
                            } else {
                                newStatus = Post.VoteStatus.DOWNVOTED;
                            }
                        } else {
                            // Don't change the vote status (this case should rarely happen).
                            newStatus = item.getVoteStatus();
                        }

                        ServerLikeClient.updateLikeStatus(LeaderboardTabAdapter.this, item.getId(),
                                newStatus, new AsyncHttpResponseHandler() {
                            @Override
                            public void onSuccess(int statusCode, Header[] headers,
                                                  byte[] responseBody) {
                                // If successful, locally update the vote status and background.
                                item.setVoteStatus(newStatus);
                                updateViewCountBackground(newStatus);
                            }

                            @Override
                            public void onFailure(int statusCode, Header[] headers,
                                                  byte[] responseBody, Throwable error) {
                                try {
                                    Log.d(Integer.toString(statusCode), new String(responseBody));
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                                // If unsuccessful, the item's vote status is unchanged.
                                updateViewCountBackground(item.getVoteStatus());
                            }
                        });

                        activePointerId = MotionEvent.INVALID_POINTER_ID;

                        // Animate the vote count returning to its position.
                        AnimatorSet animatorSet = new AnimatorSet();
                        ObjectAnimator xAnim = ObjectAnimator.ofFloat(viewCountTextView, "X",
                                viewCountTextView.getX(), initialViewX);
                        ObjectAnimator yAnim = ObjectAnimator.ofFloat(viewCountTextView, "Y",
                                viewCountTextView.getY(), initialViewY);
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

                                setClipChildrenForParents(viewCountTextView, true);
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
                    // is set to false, then the entire list item view covers the tab widget...
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
                if (item.isImage()) {
                    // Create an expanded ImageView.
                    ImageView expandedImageView = new ImageView(mParentManager.getParentActivity());

                    expandedImageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                    expandedImageView.setVisibility(View.GONE);
                    expandedImageView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN);
                    Picasso.with(mParentManager.getParentActivity()).load(item.getMediaUrl())
                            .into(expandedImageView);
                    baseFrameLayout.addView(expandedImageView);

                    animateViewExpansion(expandedImageView);
                } else {
                    // Create an expanded VideoView.
                    VideoView expandedVideoView = new VideoView(mParentManager.getParentActivity());
                    expandedVideoView.setVisibility(View.GONE);
                    expandedVideoView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN);
                    expandedVideoView.setVideoURI(Uri.parse(item.getMediaUrl()));

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

                        if (item.isImage()) {
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
             * @see com.wolfpakapp.wolfpak2.leaderboard.LeaderboardTabAdapter.ViewHolder.ViewCountOnTouchListener
             */
            private final class ExpandedViewOnTouchListener implements View.OnTouchListener {
                private int activePointerId = MotionEvent.INVALID_POINTER_ID;

                private float lastTouchX = 0;
                private float lastTouchY = 0;

                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    final int action = MotionEventCompat.getActionMasked(event);

                    switch (action) {
                        case MotionEvent.ACTION_DOWN: {
                            // Make sure that the SwipeRefreshLayout is disabled.
                            mParentManager.getTabLayout().setEnabled(false);
                            activePointerId = MotionEventCompat.getPointerId(event, 0);
                            // May not be necessary...
                            requestDisallowInterceptTouchEventForParents(v, true);

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

                            break;
                        }
                        case MotionEvent.ACTION_POINTER_UP: {
                            final int pointerIndex = MotionEventCompat.getActionIndex(event);
                            final int pointerId = MotionEventCompat.findPointerIndex(event, pointerIndex);
                            if (pointerId == activePointerId) {
                                final int newPointerIndex = pointerIndex == 0 ? 1 : 0;
                                lastTouchX = event.getRawX();
                                lastTouchY = event.getRawY();
                                activePointerId = MotionEventCompat.getPointerId(event, newPointerIndex);
                            }

                            break;
                        }
                        case MotionEvent.ACTION_UP: {
                            mParentManager.getTabLayout().setEnabled(true);
                            mParentManager.getRecyclerView().setEnabled(true);

                            activePointerId = MotionEvent.INVALID_POINTER_ID;
                            // May not be necessary...
                            requestDisallowInterceptTouchEventForParents(v, false);

                            // Recalculate the finalBounds as the expanded View may have been dragged
                            finalBounds = new Rect((int) v.getX(), (int) v.getY(),(int) v.getX() + v.getWidth(),
                                    (int) v.getY() + v.getHeight());

                            animateViewShrinking(v);
                        }
                    }

                    return true;
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
