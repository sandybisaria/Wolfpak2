package com.wolfpakapp.wolfpak2;

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

import com.squareup.picasso.Picasso;

import java.util.ArrayList;

public class LeaderboardTabAdapter extends RecyclerView.Adapter<LeaderboardTabAdapter.ViewHolder> {
    private ArrayList<LeaderboardListItem> mLeaderboardListItems;
    private LeaderboardTabManager mParentManager;

    private Animator mCurrentAnimator;
    private boolean isItemSelected = false;
    private boolean isNewDrawingOrderSet = false;

    public LeaderboardTabAdapter(ArrayList<LeaderboardListItem> mLeaderboardListItems,
                                 LeaderboardTabManager mParentManager) {
        this.mLeaderboardListItems = mLeaderboardListItems;
        this.mParentManager = mParentManager;
    }

    @Override
    public int getItemCount() {
        return mLeaderboardListItems.size();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_leaderboard, parent, false);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        holder.bindListItem(mLeaderboardListItems.get(position));
    }

    public LeaderboardTabManager getParentManager() {
        return mParentManager;
    }

    public boolean isItemSelected() {
        return isItemSelected;
    }

    //TODO Handle touching multiple items at once (i.e. view count + expanding view)
    public class ViewHolder extends RecyclerView.ViewHolder {
        private LeaderboardListItem item;

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

        public void bindListItem(LeaderboardListItem item) {
            this.item = item;

            handleTextView.setText(item.getHandle());

            updateViewCountBackground(item.getVoteStatus());
            if (!mParentManager.getTag().equals(LeaderboardFragment.DEN_TAG)) {
                viewCountTextView.setOnTouchListener(new ViewCountOnTouchListener());
            }

            if (item.isImage()) {
                Picasso.with(mParentManager.getParentActivity()).load(item.getMediaUrl())
                        .into(thumbnailImageView);
            } else {
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

        public void updateViewCountBackground(LeaderboardListItem.VoteStatus voteStatus) {
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

        public LeaderboardListItem getListItem() {
            return item;
        }

        private void requestDisallowInterceptTouchEventForParents(View v,
                                                                  boolean disallowIntercept) {
            ViewParent parent = v.getParent();
            while (parent != null) {
                parent.requestDisallowInterceptTouchEvent(disallowIntercept);
                parent = parent.getParent();
            }

        }

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
                        if (!isNewDrawingOrderSet) {
                            RecyclerView recyclerView = mParentManager.getRecyclerView();
                            final int indexOfFrontChild = recyclerView.indexOfChild(itemView);
                            Log.d("Index",Integer.toString(indexOfFrontChild));
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
                            recyclerView.invalidate();
                        }

                        final float x = event.getRawX();
                        final float y = event.getRawY();

                        final float dx = x - lastTouchX;
                        final float dy = y - lastTouchY;

                        viewCountTextView.setX(viewCountTextView.getX() + dx);
                        viewCountTextView.setY(viewCountTextView.getY() + dy);

                        if (viewCountTextView.getY() < initialViewY) {
                            if (item.getVoteStatus() == LeaderboardListItem.VoteStatus.UPVOTED) {
                                updateViewCountBackground(LeaderboardListItem.VoteStatus.NOT_VOTED);
                            } else {
                                updateViewCountBackground(LeaderboardListItem.VoteStatus.UPVOTED);
                            }
                        } else if (viewCountTextView.getY() > initialViewY) {
                            if (item.getVoteStatus() == LeaderboardListItem.VoteStatus.DOWNVOTED) {
                                updateViewCountBackground(LeaderboardListItem.VoteStatus.NOT_VOTED);
                            } else {
                                updateViewCountBackground(LeaderboardListItem.VoteStatus.DOWNVOTED);
                            }
                        }

                        lastTouchX = x;
                        lastTouchY = y;

                        return true;
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

                        return true;
                    }

                    case MotionEvent.ACTION_CANCEL:
                    case MotionEvent.ACTION_UP: {
                        mParentManager.toggleSwipeRefreshLayout();
                        requestDisallowInterceptTouchEventForParents(viewCountTextView, false);

                        LeaderboardListItem.VoteStatus newStatus = item.getVoteStatus();
                        if (viewCountTextView.getY() < initialViewY) {
                            if (item.getVoteStatus() == LeaderboardListItem.VoteStatus.UPVOTED) {
                                newStatus = LeaderboardListItem.VoteStatus.NOT_VOTED;
                            } else {
                                newStatus = LeaderboardListItem.VoteStatus.UPVOTED;
                            }
                        } else if (viewCountTextView.getY() > initialViewY) {
                            if (item.getVoteStatus() == LeaderboardListItem.VoteStatus.DOWNVOTED) {
                                newStatus = LeaderboardListItem.VoteStatus.NOT_VOTED;
                            } else {
                                newStatus = LeaderboardListItem.VoteStatus.DOWNVOTED;
                            }
                        }

                        ServerLikeClient.updateLikeStatus(LeaderboardTabAdapter.this, ViewHolder.this,
                                item.getId(), newStatus);

                        activePointerId = MotionEvent.INVALID_POINTER_ID;

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
                                RecyclerView recyclerView = mParentManager.getRecyclerView();
                                recyclerView.setChildDrawingOrderCallback(null);
                                recyclerView.invalidate();
                                isNewDrawingOrderSet = false;
                                setClipChildrenForParents(viewCountTextView, true);
                                isItemSelected = false;
                            }
                        });

                        animatorSet.start();
                        return true;
                    }
                }

                return false;
            }

            private void setClipChildrenForParents(View v, boolean clipChildren) {
                ViewParent parent = v.getParent();
                while (parent instanceof ViewGroup) {
                    if (!(parent instanceof SwipeRefreshLayout)) {
                        ((ViewGroup) parent).setClipChildren(clipChildren);
                    }
                    parent = parent.getParent();
                }
            }
        }

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
                if (isItemSelected) {
                    return;
                }
                isItemSelected = true;
                if (item.isImage()) {
                    ImageView expandedImageView = new ImageView(mParentManager.getParentActivity());

                    expandedImageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                    expandedImageView.setVisibility(View.GONE);
                    expandedImageView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN);
                    Picasso.with(mParentManager.getParentActivity()).load(item.getMediaUrl())
                            .into(expandedImageView);
                    baseFrameLayout.addView(expandedImageView);

                    animateViewExpansion(expandedImageView);
                } else {
                    VideoView expandedVideoView = new VideoView(mParentManager.getParentActivity());
                    expandedVideoView.setVisibility(View.GONE);
                    expandedVideoView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN);
                    expandedVideoView.setVideoURI(Uri.parse(item.getMediaUrl()));

                    baseFrameLayout.addView(expandedVideoView);

                    animateViewExpansion(expandedVideoView);
                }
            }

            private void animateViewExpansion(final View expandedView) {
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
                set.play(ObjectAnimator.ofFloat(animatingView, View.X, startBounds.left, finalBounds.left))
                        .with(ObjectAnimator.ofFloat(animatingView, View.Y, startBounds.top, finalBounds.top))
                        .with(widthAnimator).with(heightAnimator);
                set.setDuration(ANIM_DURATION);
                set.setInterpolator(INTERPOLATOR);
                set.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        expandedView.setVisibility(View.VISIBLE);
                        animatingView.setVisibility(View.GONE);

                        if (item.isImage()) {
                            expandedView
                                    .setOnTouchListener(new ExpandedViewOnTouchListener());
                        } else {
                            ((VideoView) expandedView).setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                                @Override
                                public void onCompletion(MediaPlayer mp) {
                                    expandedView.setOnTouchListener(new ExpandedViewOnTouchListener());
                                }
                            });
                            ((VideoView) expandedView).start();
                        }
                    }
                });

                thumbnailImageView.setVisibility(View.INVISIBLE);
                set.start();
                mCurrentAnimator = set;
            }

            private final class ExpandedViewOnTouchListener implements View.OnTouchListener {
                private int activePointerId = MotionEvent.INVALID_POINTER_ID;

                private float lastTouchX = 0;
                private float lastTouchY = 0;

                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    final int action = MotionEventCompat.getActionMasked(event);

                    switch (action) {
                        case MotionEvent.ACTION_DOWN: {
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

                            finalBounds = new Rect((int) v.getX(), (int) v.getY(),(int) v.getX() + v.getWidth(),
                                    (int) v.getY() + v.getHeight());
                            animateViewShrinking(v);
                        }
                    }

                    return true;
                }
            }

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
                set.play(ObjectAnimator.ofFloat(animatingView, View.X, finalBounds.left, startBounds.left))
                        .with(ObjectAnimator.ofFloat(animatingView, View.Y, finalBounds.top, startBounds.top))
                        .with(widthAnimator).with(heightAnimator);
                set.setDuration(ANIM_DURATION);
                set.setInterpolator(INTERPOLATOR);
                set.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        thumbnailImageView.setVisibility(View.VISIBLE);
                        mCurrentAnimator = null;
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
