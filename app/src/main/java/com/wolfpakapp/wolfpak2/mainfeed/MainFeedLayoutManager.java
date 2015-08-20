package com.wolfpakapp.wolfpak2.mainfeed;

import android.graphics.Point;
import android.support.v4.view.MotionEventCompat;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.RelativeLayout;

import com.wolfpakapp.wolfpak2.Post;

import java.util.ArrayDeque;
import java.util.ArrayList;

/**
 * Created by Vishaal on 7/20/15.
 */
public class MainFeedLayoutManager {
    private RelativeLayout baseLayout;
    private MainFeedFragment mainFeed;

    private MainFeedNetworkingManager networkingManager;

    private ArrayDeque<MediaView> mediaViewArrayDeque = new ArrayDeque<>();

    public MainFeedLayoutManager(MainFeedFragment mainFeed){
        this.mainFeed = mainFeed;
        baseLayout = mainFeed.getBaseLayout();
    }

    /** PreLoad Views **/
    public void loadViews(ArrayDeque<Post> postArrayDeque) {
        networkingManager = mainFeed.getNetworkingManager();

        ArrayList<MediaView> mediaViewList = new ArrayList<>();
        for (Post post : postArrayDeque) {
            MediaView mediaView = new MediaView(mainFeed.getActivity());
            mediaView.setContent(post);
            mediaView.setOnTouchListener(new ImageOnTouchListener());
            mediaViewList.add(mediaView);
            mediaViewArrayDeque.addLast(mediaView);
        }

        // Add posts to the layout in reverse order (so that the first, top, post is added
        for (int idx = mediaViewList.size() - 1; idx >= 0; idx--) {
            baseLayout.addView(mediaViewList.get(idx));
        }

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

        baseLayout.removeView(v);
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

        baseLayout.removeView(v);
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

                    if(event.getRawY() < green){
                        networkingManager.updateLikeStatus(Post.VoteStatus.UPVOTED);
                        mediaView.setLikeStatus(Post.VoteStatus.UPVOTED);
                        SlideToAbove(mediaViewArrayDeque.pollFirst());
                    }
                    else if(event.getRawY()>red){
                        networkingManager.updateLikeStatus(Post.VoteStatus.DOWNVOTED);
                        mediaView.setLikeStatus(Post.VoteStatus.DOWNVOTED);
                        SlideToDown(mediaViewArrayDeque.pollFirst());
                    } else {
                        //TODO Animate to original position.
                        v.setX(0);
                        v.setY(0);
                        mediaView.setLikeStatus(Post.VoteStatus.NOT_VOTED);
                    }

                    try {
//                        mediaViewArrayDeque[mainFeed.number].mediaVideoViewThumbnail.setVisibility(View.GONE);
                        mediaViewArrayDeque.peekFirst().mediaVideoView.start();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }


                    /** AUTO REFRESH **/
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    if(mediaViewArrayDeque.size() == 0){
                        networkingManager.getHowls();
                    }

                    break;
                }
            }
            return true;
        }
    }
}
