package com.wolfpakapp.wolfpak2.mainfeed;

import android.content.Context;
import android.graphics.Color;
import android.net.Uri;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.VideoView;

import com.squareup.picasso.Picasso;
import com.wolfpakapp.wolfpak2.Post;
import com.wolfpakapp.wolfpak2.R;

import java.util.Objects;

public class MediaView extends RelativeLayout {
    // Private variables
    private ImageView mediaImageView;
    public VideoView mediaVideoView;

//    public ImageView mediaVideoViewThumbnail;
    private View likeStatusOverlayView;

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
     * Handles Changing Opaque View on Top of ImageView/VideoView Based on LikeStatus
     * - Upvote: Green
     * - Downvote: Red
     * - Neutral: Transparent
     *
     * @param voteStatus Status of the view
     */
    public void setLikeStatus(Post.VoteStatus voteStatus) {
        switch (voteStatus) {
            case UPVOTED:
                likeStatusOverlayView.setBackgroundColor(Color.argb(100, 0, 255, 0));
                break;

            case DOWNVOTED:
                likeStatusOverlayView.setBackgroundColor(Color.argb(100, 255, 0, 0));
                break;

            case NOT_VOTED:
            default:
                likeStatusOverlayView.setBackgroundColor(Color.argb(0,0,0,0));
                break;
        }
    }

    /**
     * Initialize media view based on url and media type
     *
     */
    public void setMediaView(Post post) {
        if (post.isImage()) {
            mediaImageView.setVisibility(View.VISIBLE);
            Picasso.with(mediaImageView.getContext()).load(post.getMediaUrl()).into(mediaImageView);

        } else {
            mediaVideoView.setVisibility(View.VISIBLE);
            mediaVideoView.setVideoPath(post.getMediaUrl());
            mediaVideoView.requestFocus();
//            mediaVideoViewThumbnail.setVisibility(View.VISIBLE);
//            Picasso.with(mediaVideoViewThumbnail.getContext()).load(thumbnail).into(mediaVideoViewThumbnail);
        }
    }

    /**
     * Base Initialization for this class
     */
    private void baseInit() {
        LayoutInflater.from(getContext()).inflate(R.layout.media_view, this);

        mediaImageView = (ImageView)findViewById(R.id.mediaImageView);
        mediaVideoView = (VideoView)findViewById(R.id.mediaVideoView);
//        this.mediaVideoViewThumbnail = (ImageView) findViewById(R.id.mediaVideoViewThumbnail);

        likeStatusOverlayView = findViewById(R.id.likeStatusOverlayView);
    }
}
