package com.wolfpakapp.wolfpak2;

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

            int statusColor = voteStatus.getStatusColor(mParentManager.getParentContext());
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

            private int ANIM_DURATION = 350;

            float lastTouchX = 0;
            float lastTouchY = 0;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                final int action = MotionEventCompat.getActionMasked(event);

                switch (action) {
                    case MotionEvent.ACTION_DOWN: {
                        // When a finger presses on the view count, store the original location of
                        // the View and record the pointer ID of the finger.
                        mLayout.setEnabled(false);

                        final float x = event.getRawX();
                        final float y = event.getRawY();

                        lastTouchX = x;
                        lastTouchY = y;

                        activePointerId = MotionEventCompat.getPointerId(event, 0);

                        // Ensure that the View receives touch events before any elements under it.
                        v.getParent().requestDisallowInterceptTouchEvent(true);

                        initialViewX = v.getX();
                        initialViewY = v.getY();

                        // Ensure that the RecyclerView draws the listItemView containing the view
                        // count before all other listItemViews.
//                        RecyclerView recyclerView = mParentManager.getRecyclerView();
//                        final int indexOfFrontChild = recyclerView.indexOfChild(itemView);
//                        recyclerView.setChildDrawingOrderCallback(new RecyclerView.ChildDrawingOrderCallback() {
//                            private int nextChildIndexToRender;
//                            @Override
//                            public int onGetChildDrawingOrder(int childCount, int iteration) {
//                                if (iteration == childCount - 1) {
//                                    nextChildIndexToRender = 0;
//                                    return indexOfFrontChild;
//                                } else {
//                                    if (nextChildIndexToRender == indexOfFrontChild) {
//                                        nextChildIndexToRender++;
//                                    }
//                                    return nextChildIndexToRender++;
//                                }
//                            }
//                        });
//                        recyclerView.invalidate();

                        break;
                    }
                    case MotionEvent.ACTION_MOVE: {
                        final float x = event.getRawX();
                        final float y = event.getRawY();

                        // Calculate the change in pointer position.
                        final float dx = x - lastTouchX;
                        final float dy = y - lastTouchY;

                        // Adjusts the view count based on dx and dy.
                        v.setX(v.getX() + dx);
                        v.setY(v.getY() + dy);

                        // Update the color of the view count.
                        if (v.getY() < initialViewY) {
                            if (item.getVoteStatus() == LeaderboardListItem.VoteStatus.UPVOTED) {
                                updateViewCountBackground(LeaderboardListItem.VoteStatus.NOT_VOTED);
                            } else {
                                updateViewCountBackground(LeaderboardListItem.VoteStatus.UPVOTED);
                            }
                        } else if (v.getY() > initialViewY) {
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
                        // When a finger is lifted (but not the last finger)
                        final int pointerIndex = MotionEventCompat.getActionIndex(event);
                        final int pointerId = MotionEventCompat.findPointerIndex(event, pointerIndex);
                        // Ensure that the correct pointer ID is being used (in case the view
                        // switched pointers
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

                        // Determine the new VoteStatus of the post.
                        LeaderboardListItem.VoteStatus newStatus = item.getVoteStatus();
                        if (v.getY() < initialViewY) {
                            if (item.getVoteStatus() == LeaderboardListItem.VoteStatus.UPVOTED) {
                                newStatus = LeaderboardListItem.VoteStatus.NOT_VOTED;
                            } else {
                                newStatus = LeaderboardListItem.VoteStatus.UPVOTED;
                            }
                        } else if (v.getY() > initialViewY) {
                            if (item.getVoteStatus() == LeaderboardListItem.VoteStatus.DOWNVOTED) {
                                newStatus = LeaderboardListItem.VoteStatus.NOT_VOTED;
                            } else {
                                newStatus = LeaderboardListItem.VoteStatus.DOWNVOTED;
                            }
                        }
                        // Update the post's VoteStatus and the view count background.
                        item.setVoteStatus(newStatus);
                        updateViewCountBackground(newStatus);

                        // Update the vote status of the post on the server.
//                        WolfpakLikeClient.updateVoteStatus(listItem.getId(), newStatus);

                        activePointerId = MotionEvent.INVALID_POINTER_ID;

                        // Animate the view count so that it returns to the starting position.
                        AnimatorSet animatorSet = new AnimatorSet();
                        ObjectAnimator xAnim = ObjectAnimator.ofFloat(v, "X", v.getX(), initialViewX);
                        ObjectAnimator yAnim = ObjectAnimator.ofFloat(v, "Y", v.getY(), initialViewY);
                        animatorSet.play(xAnim).with(yAnim);
                        animatorSet.setDuration(ANIM_DURATION);
                        animatorSet.setInterpolator(LeaderboardTabManager.getViewCountInterpolator());

                        animatorSet.start();

                        // Reset the RecyclerView's drawing order of the posts.
//                        mParentManager.getRecyclerView().setChildDrawingOrderCallback(null);
                    }
                }

                return true;
            }
        }
    }
}
