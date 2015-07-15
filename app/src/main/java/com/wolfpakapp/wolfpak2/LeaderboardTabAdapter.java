package com.wolfpakapp.wolfpak2;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.animation.Interpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.TextView;

import java.util.ArrayList;

public class LeaderboardTabAdapter extends RecyclerView.Adapter<LeaderboardTabAdapter.ViewHolder> {
    private ArrayList<LeaderboardListItem> mLeaderboardListItems;
    private LeaderboardTabManager mParentManager;

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

    public class ViewHolder extends RecyclerView.ViewHolder {
        private LeaderboardListItem item;

        private TextView handleTextView;
        private TextView viewCountTextView;

        public ViewHolder(View itemView) {
            super(itemView);

            handleTextView = (TextView) itemView
                    .findViewById(R.id.leaderboard_item_handle_text_view);
            viewCountTextView = (TextView) itemView
                    .findViewById(R.id.leaderboard_item_view_count_text_view);
        }

        public void bindListItem(LeaderboardListItem item) {
            this.item = item;

            handleTextView.setText(item.getHandle());

            updateViewCountBackground(item.getVoteStatus());
            viewCountTextView.setOnTouchListener(new ViewCountOnTouchListener());
        }

        private void updateViewCountBackground(LeaderboardListItem.VoteStatus voteStatus) {
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
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    viewCountTextView.setBackground(bg);
                } else {
                    viewCountTextView.setBackgroundDrawable(bg);
                }
            }

            viewCountTextView.setText(Integer.toString(item.getUpdatedVoteCount()));

            viewCountTextView.invalidate();
        }

        private final class ViewCountOnTouchListener implements View.OnTouchListener {
            private SwipeRefreshLayout mLayout = mParentManager.getTabLayout();

            private int activePointerId = MotionEvent.INVALID_POINTER_ID;

            private float initialViewX;
            private float initialViewY;

            float lastTouchX = 0;
            float lastTouchY = 0;

            private int ANIM_DURATION = 350;
            private Interpolator INTERPOLATOR = new OvershootInterpolator(1.4f);

            @Override
            public boolean onTouch(View vow, MotionEvent event) {
                final int action = MotionEventCompat.getActionMasked(event);

                switch (action) {
                    case MotionEvent.ACTION_DOWN: {
                        activePointerId = MotionEventCompat.getPointerId(event, 0);

                        mLayout.setEnabled(false);
                        setClipChildrenForParents(viewCountTextView, false);
                        requestDisallowInterceptTouchEventForParents(viewCountTextView, true);

                        lastTouchX = event.getRawX();
                        lastTouchY = event.getRawY();

                        initialViewX = viewCountTextView.getX();
                        initialViewY = viewCountTextView.getY();

                        RecyclerView recyclerView = mParentManager.getRecyclerView();
                        final int indexOfFrontChild = recyclerView.indexOfChild(itemView);
                        recyclerView.setChildDrawingOrderCallback(new RecyclerView.ChildDrawingOrderCallback() {
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
                        recyclerView.invalidate();

                        break;
                    }
                    case MotionEvent.ACTION_MOVE: {
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
                    case MotionEvent.ACTION_UP: {
                        mLayout.setEnabled(true);
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

                        item.setVoteStatus(newStatus);
                        updateViewCountBackground(newStatus);

//                        WolfpakLikeClient.updateVoteStatus(listItem.getId(), newStatus);

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
                                mParentManager.getRecyclerView().setChildDrawingOrderCallback(null);
                                setClipChildrenForParents(viewCountTextView, true);
                            }
                        });

                        animatorSet.start();
                    }
                }

                return true;
            }

            private void setClipChildrenForParents(View v, boolean clipChildren) {
                ViewParent parent = v.getParent();
                while (parent instanceof ViewGroup) {
                    ((ViewGroup) parent).setClipChildren(clipChildren);
                    parent = parent.getParent();
                }
            }

            private void requestDisallowInterceptTouchEventForParents(View v,
                                                                      boolean disallowIntercept) {
                ViewParent parent = v.getParent();
                while (parent != null) {
                    parent.requestDisallowInterceptTouchEvent(disallowIntercept);
                    parent = parent.getParent();
                }

            }
        }
    }
}
