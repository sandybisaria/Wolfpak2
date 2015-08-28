package com.wolfpakapp.wolfpak2.mainfeed;

import android.animation.Animator;
import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.VideoView;

import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;
import com.wolfpakapp.wolfpak2.Post;
import com.wolfpakapp.wolfpak2.R;

import java.util.Objects;

public class MediaView extends RelativeLayout {

    private Post mPost;

    private ImageView mediaImageView;
    private VideoView mediaVideoView;
    private ImageView likeStatusOverlayView;

    /** Constructors **/
    public MediaView(Context context) {
        super(context);
        baseInit();
    }

    public MediaView(Context context, AttributeSet attrs) {
        super(context, attrs);
        baseInit();
    }

    public MediaView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        baseInit();
    }

    /**
     * Base initialization for this class
     */
    private void baseInit() {
        LayoutInflater.from(getContext()).inflate(R.layout.media_view, this);

        mediaImageView = (ImageView)findViewById(R.id.mediaImageView);
        mediaVideoView = (VideoView)findViewById(R.id.mediaVideoView);

        likeStatusOverlayView = (ImageView) findViewById(R.id.likeStatusOverlayView);
    }
    /**
     * Set the tint of the view based on the vote status.
     *
     * @param voteStatus Status of the view
     */
    public void setTint(Post.VoteStatus voteStatus) {
        switch (voteStatus) {
            case UPVOTED: {
                Picasso.with(getContext()).load(R.drawable.main_feed_up_vote).into(likeStatusOverlayView, new Callback() {
                    @Override
                    public void onSuccess() {
                        likeStatusOverlayView.setBackgroundColor(Color.argb(100, 0, 255, 0));
                    }

                    @Override
                    public void onError() {

                    }
                });
                break;
            }
            case DOWNVOTED: {
                Picasso.with(getContext()).load(R.drawable.main_feed_down_vote).into(likeStatusOverlayView, new Callback() {
                    @Override
                    public void onSuccess() {
                        likeStatusOverlayView.setBackgroundColor(Color.argb(100, 255, 0, 0));
                    }

                    @Override
                    public void onError() {

                    }
                });
                break;
            }
            case NOT_VOTED:
            default: {
                likeStatusOverlayView.setBackgroundColor(Color.TRANSPARENT);
                likeStatusOverlayView.setImageDrawable(null);
                break;
            }
        }
    }

    /**
     * Initialize media view based on the post.
     */
    public void setContent(Post post) {
        mPost = post;

        if (post.isImage()) {
            mediaImageView.setVisibility(View.VISIBLE);
            Picasso.with(mediaImageView.getContext()).load(post.getMediaUrl()).into(mediaImageView);

        } else {
            mediaVideoView.setVisibility(View.VISIBLE);
            mediaVideoView.setVideoPath(post.getMediaUrl());
            mediaVideoView.requestFocus();
        }
    }

    /**
     * If this MediaView contains a video, start it.
     */
    public void start() {
        if (!mPost.isImage()) {
            mediaVideoView.start();
        }
    }
}
