package com.wolfpakapp.wolfpak2.mainfeed;

import android.graphics.Point;
import android.support.v4.view.MotionEventCompat;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.RelativeLayout;

import com.wolfpakapp.wolfpak2.Post;

/**
 * Created by Vishaal on 7/20/15.
 */
public class MainFeedLayoutManager {
    private RelativeLayout myLayout;
    private MainFeedFragment mainFeed;
    private MainFeedNetworkingManager network;

    public MediaView[] views;
    public int num;


    public MainFeedLayoutManager(MainFeedFragment mainFeed, MainFeedNetworkingManager network){
        this.mainFeed = mainFeed;
        this.network = network;

        views = new MediaView[6];
        num = network.count-1;
    }

    /** PreLoad Views **/
    public void loadView(Post post){
        myLayout = mainFeed.baseLayout;
        MediaView mediaView = new MediaView(mainFeed.getActivity());

        mediaView.setMediaView(post);

        mediaView.setOnTouchListener(new ImageOnTouchListener());
        myLayout.addView(mediaView);

        views[num] = mediaView;
        Log.v("DEBUG", String.valueOf(num));
        num--;

        mainFeed.bringButtonsToFront();
    }

    /** Slide Up Animation **/
    public void SlideToAbove(View v) {
        Animation slide;
        slide = new TranslateAnimation(Animation.RELATIVE_TO_PARENT, 0.0f,
                Animation.RELATIVE_TO_PARENT, 0.0f, Animation.RELATIVE_TO_PARENT,
                0.0f, Animation.RELATIVE_TO_PARENT, -5.0f);
        slide.setDuration(750);

        v.startAnimation(slide);
        v.animate().rotation(-30).start();

        slide.setAnimationListener(new Animation.AnimationListener() {

            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
            }

        });

        myLayout.removeView(v);

    }

    /** Slide Down Animation **/
    public void SlideToDown(View v) {
        Animation slide;
        slide = new TranslateAnimation(Animation.RELATIVE_TO_PARENT, 0.0f,
                Animation.RELATIVE_TO_PARENT, 0.0f, Animation.RELATIVE_TO_PARENT,
                0.0f, Animation.RELATIVE_TO_PARENT, 5.2f);
        slide.setDuration(751);

        v.startAnimation(slide);
        v.animate().rotation(30).start();

        slide.setAnimationListener(new Animation.AnimationListener() {

            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
            }

        });

        myLayout.removeView(v);


    }

    /** DragView Function **/
    public final class ImageOnTouchListener implements View.OnTouchListener {
        private float lastTouchX = 0;
        private float lastTouchY = 0;

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            final int action = MotionEventCompat.getActionMasked(event);

            switch (action) {
                case MotionEvent.ACTION_DOWN: {
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

                    Display display = mainFeed.getActivity().getWindowManager().getDefaultDisplay();
                    Point size = new Point();
                    display.getSize(size);
                    double maxY = size.y;
                    double green = maxY * 0.35;
                    double red = maxY * 0.65;

                    if(event.getRawY()<green){
                        mediaView.setLikeStatus(Post.VoteStatus.UPVOTED);
                    }
                    else if(event.getRawY()>red){
                        mediaView.setLikeStatus(Post.VoteStatus.DOWNVOTED);
                    }
                    else{
                        mediaView.setLikeStatus(Post.VoteStatus.NOT_VOTED);
                    }
                    break;
                }
                case MotionEvent.ACTION_POINTER_UP:
                case MotionEvent.ACTION_CANCEL:
                    break;
                case MotionEvent.ACTION_UP: {
                    MediaView mediaView = (MediaView) v;
                    Display display = mainFeed.getActivity().getWindowManager().getDefaultDisplay();
                    Point size = new Point();
                    display.getSize(size);
                    double maxY = size.y;
                    double green = maxY * 0.35;
                    double red = maxY * 0.66;

                    if(event.getRawY()<green){
                        network.updateLikeStatus(Post.VoteStatus.UPVOTED);
                        mainFeed.number++;
                        mediaView.setLikeStatus(Post.VoteStatus.UPVOTED);
                        SlideToAbove(v);
                    }
                    else if(event.getRawY()>red){
                        network.updateLikeStatus(Post.VoteStatus.DOWNVOTED);
                        mainFeed.number++;
                        mediaView.setLikeStatus(Post.VoteStatus.DOWNVOTED);
                        SlideToDown(v);
                    } else {
                        //TODO Animate to original position.
                        v.setX(0);
                        v.setY(0);
                        mediaView.setLikeStatus(Post.VoteStatus.NOT_VOTED);
                    }

                    if (network.mPosts[mainFeed.number] != null && !network.mPosts[mainFeed.number].isImage()) {
//                        views[mainFeed.number].mediaVideoViewThumbnail.setVisibility(View.GONE);
                        views[mainFeed.number].mediaVideoView.start();
                    }


                    /** AUTO REFRESH **/
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    if(mainFeed.number == network.count){
                        mainFeed.number = 0;
                        num = 0;
                        network.getHowls();
                    }

                    break;
                }
            }
            return true;
        }
    }
}
