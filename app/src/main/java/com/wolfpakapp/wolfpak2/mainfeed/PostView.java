package com.wolfpakapp.wolfpak2.mainfeed;

import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.VideoView;

import com.squareup.picasso.Picasso;
import com.wolfpakapp.wolfpak2.Post;

public class PostView extends RelativeLayout {
    private Context mContext;
    private Post mPost;

    private View overlayView;
    private View contentView;

    public PostView(Context context) {
        super(context);
    }

    public PostView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public PostView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public PostView(Context context, Post post) {
        super(context);
        mContext = context;
        mPost = post;

        init();
    }

    private void init() {
        setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);

        ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT);

        if (mPost.isImage()) {
            ImageView imageView = new ImageView(mContext);

            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            Picasso.with(mContext).load(mPost.getMediaUrl()).into(imageView);

            addView(imageView, params);
            contentView = imageView;
        } else {
            VideoView videoView = new VideoView(mContext);

            videoView.setVideoPath(mPost.getMediaUrl());

            addView(videoView, params);
            contentView = videoView;
        }

        overlayView = new View(mContext);
        overlayView.setAlpha(0.5f);
        overlayView.setBackgroundColor(Color.TRANSPARENT);

        addView(overlayView, params);
    }

    public void start() {
        if (!mPost.isImage()) {
            ((VideoView) contentView).start();
        }
    }

    public void setTint(Post.VoteStatus voteStatus) {
        switch (voteStatus) {
            case NOT_VOTED: {
                overlayView.setBackgroundColor(Color.TRANSPARENT);
                return;
            }
            case DOWNVOTED:
            case UPVOTED: {
                overlayView.setBackgroundColor(voteStatus.getStatusColor(mContext));
            }
        }
    }
}
