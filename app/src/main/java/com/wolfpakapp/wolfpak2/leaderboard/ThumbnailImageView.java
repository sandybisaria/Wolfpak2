package com.wolfpakapp.wolfpak2.leaderboard;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.media.Image;
import android.media.MediaPlayer;
import android.os.Build;
import android.support.v4.view.MotionEventCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Interpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.VideoView;

import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;
import com.wolfpakapp.wolfpak2.Post;
import com.wolfpakapp.wolfpak2.R;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

public class ThumbnailImageView extends ImageView {
    private LeaderboardTabManager mManager = null;
    private Post mPost = null;
    private View parentItemView = null;

    public ThumbnailImageView(Context context) {
        super(context);
    }

    public ThumbnailImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ThumbnailImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void initialize(LeaderboardTabManager manager, Post post, View parentItemView) {
        mManager = manager;
        mPost = post;
        this.parentItemView = parentItemView;

        final ProgressBar progressBar = (ProgressBar) parentItemView.findViewById(R.id.progress_bar);

        if (post.isImage()) {
            Picasso.with(getContext()).load(post.getMediaUrl()).into(this, new Callback() {
                @Override
                public void onSuccess() {
                    progressBar.setVisibility(GONE);
                    setOnClickListener(new ThumbnailOnClickListener());
                }

                @Override
                public void onError() {

                }
            });
        } else {
            // Overlay a play icon on top of the video thumbnail
            final Drawable thumbnailDrawable;
            final Drawable overlayDrawable;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                //TODO Retrieve thumbnails from the server
                thumbnailDrawable = getContext().getDrawable(R.drawable.empty_video_icon);
                overlayDrawable = getContext().getDrawable(R.drawable.play_icon);
            } else {
                thumbnailDrawable = getContext().getResources()
                        .getDrawable(R.drawable.empty_video_icon);
                overlayDrawable = getContext().getResources().getDrawable(R.drawable.play_icon);

            }

            final ImageView dummyImageView = new ImageView(getContext());
            Picasso.with(getContext()).load(post.getThumbnailUrl()).into(dummyImageView, new Callback() {
                @Override
                public void onSuccess() {
                    Drawable newThumbnailDrawable = dummyImageView.getDrawable();

                    Drawable[] layers = {newThumbnailDrawable, overlayDrawable};
                    setImageDrawable(new LayerDrawable(layers));
                    setBackgroundColor(Color.BLACK);
                    setOnClickListener(new ThumbnailOnClickListener());
                }

                @Override
                public void onError() {
                    Drawable[] layers = {thumbnailDrawable, overlayDrawable};
                    setImageDrawable(new LayerDrawable(layers));
                    setBackgroundColor(Color.BLACK);
                    setOnClickListener(new ThumbnailOnClickListener());
                }
            });
        }
    }

    /**
     * The ThumbnailOnClickListener handles clicks on the thumbnail.
     * TODO Fix bug - can swipe left/right while view is expanding
     */
    private final class ThumbnailOnClickListener implements View.OnClickListener {
        private FrameLayout baseFrameLayout = mManager.getParentFragment().getBaseFrameLayout();

        private ImageView animatingView;

        private Rect startBounds;
        private Rect finalBounds;

        private int thumbnailSize;

        private final int ANIM_DURATION = 500;
        private final Interpolator INTERPOLATOR = new AccelerateDecelerateInterpolator();

        @Override
        public void onClick(View view) {
            // Do not respond if another element is selected.
            if (mManager.isItemSelected()) {
                return;
            }
            mManager.setIsItemSelected(true);

            if (mPost.isImage()) {
                // Create an expanded ImageView.
                final ImageView expandedImageView = new ImageView(getContext());

                expandedImageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                expandedImageView.setVisibility(View.GONE);
                expandedImageView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN);
                Picasso.with(getContext()).load(mPost.getMediaUrl()).into(expandedImageView, new Callback() {
                    @Override
                    public void onSuccess() {
                        baseFrameLayout.addView(expandedImageView);

                        animateViewExpansion(expandedImageView);
                    }

                    @Override
                    public void onError() {

                    }
                });
            } else {
                // Create an expanded VideoView.
                VideoView expandedVideoView = new VideoView(getContext());
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
            animatingView = new ImageView(getContext());
            animatingView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            animatingView.setVisibility(View.GONE);
            animatingView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
            animatingView.setImageDrawable(getDrawable());
            baseFrameLayout.addView(animatingView);

            startBounds = new Rect();
            finalBounds = new Rect();
            thumbnailSize = (int) getContext().getResources()
                    .getDimension(R.dimen.leaderboard_item_thumbnail_size);

            // Determine the start and final bounds for the animating view.
            getGlobalVisibleRect(startBounds);
            mManager.getParentActivity().getWindow().getDecorView().getGlobalVisibleRect(finalBounds);

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
                    setVisibility(View.INVISIBLE);
                    expandedView.setVisibility(View.VISIBLE);
                    animatingView.setVisibility(View.GONE);

                    if (mPost.isImage()) {
                        expandedView.setOnTouchListener(new ExpandedViewOnTouchListener());
                    } else {
                        // Do not set the OnTouchListener until the video is completed.
                        ((VideoView) expandedView)
                                .setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
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
        }

        /**
         * The ExpandedViewOnTouchListener handles touch events on the expanded view. The drag
         * implementation is similar to that of the vote count.
         */
        private final class ExpandedViewOnTouchListener implements View.OnTouchListener {
            private float lastTouchX = 0;
            private float lastTouchY = 0;

            @Override
            public boolean onTouch(View view, MotionEvent event) {
                final int action = MotionEventCompat.getActionMasked(event);

                switch (action) {
                    case MotionEvent.ACTION_DOWN: {
                        // Make sure that the SwipeRefreshLayout is disabled.
                        mManager.getSwipeRefreshLayout().setEnabled(false);
                        // May not be necessary...
                        mManager.requestDisallowInterceptTouchEventForParents(view, true);

                        lastTouchX = event.getRawX();
                        lastTouchY = event.getRawY();

                        return true;
                    }
                    case MotionEvent.ACTION_MOVE: {
                        final float x = event.getRawX();
                        final float y = event.getRawY();

                        final float dx = x - lastTouchX;
                        final float dy = y - lastTouchY;

                        view.setX(view.getX() + dx);
                        view.setY(view.getY() + dy);

                        lastTouchX = x;
                        lastTouchY = y;

                        return true;
                    }
                    case MotionEvent.ACTION_POINTER_UP: {
                        return true;
                    }
                    case MotionEvent.ACTION_CANCEL:
                    case MotionEvent.ACTION_UP: {
                        mManager.getSwipeRefreshLayout().setEnabled(true);
                        mManager.getRecyclerView().setEnabled(true);

                        // May not be necessary...
                        mManager.requestDisallowInterceptTouchEventForParents(view, false);

                        // Recalculate the finalBounds as the expanded View may have been dragged
                        finalBounds = new Rect((int) view.getX(), (int) view.getY(),
                                (int) view.getX() + view.getWidth(), (int) view.getY() + view.getHeight());

                        animateViewShrinking(view);

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
            mManager.safeEnableSwipeRefreshLayout();

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
                    setVisibility(View.VISIBLE);
                    mManager.setIsItemSelected(false);
                    animatingView.setVisibility(View.GONE);
                }
            });

            set.start();
        }
    }
}
