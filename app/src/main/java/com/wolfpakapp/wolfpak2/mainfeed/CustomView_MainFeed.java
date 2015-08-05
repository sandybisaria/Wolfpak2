package com.wolfpakapp.wolfpak2.mainfeed;

import android.graphics.Point;
import android.net.Uri;
import android.support.v4.view.MotionEventCompat;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.RelativeLayout;

import java.util.Objects;

/**
 * Created by Vishaal on 7/20/15.
 */
public class CustomView_MainFeed{
    private RelativeLayout myLayout;
    private MainFeedFragment mainFeed;
    private Networking_MainFeed network;

    public MediaView[] views;
    public int num;


    public CustomView_MainFeed(MainFeedFragment mainFeed, Networking_MainFeed network){
        this.mainFeed = mainFeed;
        this.network = network;

        views = new MediaView[6];
        num = network.count-1;
    }

    /** PreLoad Views **/
    public void loadViews(String isImage, String handle, String url){
        myLayout = mainFeed.frame;
        MediaView mediaView = new MediaView(mainFeed.getActivity());

        Uri uri = Uri.parse(url);
        mediaView.setMediaView(uri, url, handle, isImage);

        mediaView.setOnTouchListener(new ImageOnTouchListener());
        myLayout.addView(mediaView);

        views[num] = mediaView;
        Log.v("DEBUG", String.valueOf(num));
        num--;

        mainFeed.share.bringToFront();
        mainFeed.report.bringToFront();
    }

    public void startNew(){
        Log.v("DEBUG", String.valueOf(views[0]));
        views[0].setOnTouchListener(new ImageOnTouchListener());
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
        private int activePointerId = MotionEvent.INVALID_POINTER_ID;

        private float lastTouchX = 0;
        private float lastTouchY = 0;

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            final int action = MotionEventCompat.getActionMasked(event);

            switch (action) {
                case MotionEvent.ACTION_DOWN: {
                    activePointerId = MotionEventCompat.getPointerId(event, 0);

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
                        mediaView.setLikeStatus(MediaView.LikeStatus.Like);
                    }
                    else if(event.getRawY()>red){
                        mediaView.setLikeStatus(MediaView.LikeStatus.Dislike);
                    }
                    else{
                        mediaView.setLikeStatus(MediaView.LikeStatus.Neutral);
                    }
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
                        network.incrHowls(1);
                        mainFeed.number++;
                        mediaView.setLikeStatus(MediaView.LikeStatus.Like);
                        SlideToAbove(v);
                    }
                    else if(event.getRawY()>red){
                        network.incrHowls(-1);
                        mainFeed.number++;
                        mediaView.setLikeStatus(MediaView.LikeStatus.Dislike);
                        SlideToDown(v);
                    } else {
                        v.setX(0);
                        v.setY(0);
                        mediaView.setLikeStatus(MediaView.LikeStatus.Neutral);
                    }

                    if (network.HowlsIsImage[mainFeed.number] != null && Objects.equals(network.HowlsIsImage[mainFeed.number], "false")) {
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

                    activePointerId = MotionEvent.INVALID_POINTER_ID;
                    break;
                }
            }
            return true;
        }
    }
}
