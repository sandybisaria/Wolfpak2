package com.wolfpakapp.wolfpak2;


import android.content.Context;
import android.content.res.Resources;

public class LeaderboardListItem {
    private String handle;
    private int originalVoteCount;
    private int updatedVoteCount;
    private VoteStatus voteStatus;

    public LeaderboardListItem(String handle, int originalVoteCount) {
        this.handle = handle;
        this.originalVoteCount = originalVoteCount;
        // TODO Get vote status from the server!
        setVoteStatus(VoteStatus.NOT_VOTED);
    }

    public String getHandle() {
        return handle;
    }

    public int getUpdatedVoteCount() {
        return updatedVoteCount;
    }

    public enum VoteStatus {
        NOT_VOTED(0), UPVOTED(1), DOWNVOTED(-1);
        public final int change;

        VoteStatus(int change) {
            this.change = change;
        }

        int getStatusColor(Context context) {
            Resources resources = context.getResources();
            switch (this) {
                case UPVOTED: {
                    return resources.getColor(R.color.leaderboard_view_count_background_green);
                }
                case DOWNVOTED: {
                    return resources.getColor(R.color.leaderboard_view_count_background_red);
                }
                case NOT_VOTED:
                default: {
                    return resources.getColor(R.color.leaderboard_view_count_background_grey);
                }
            }
        }
    }

    public VoteStatus getVoteStatus() {
        return voteStatus;
    }

    public void setVoteStatus(VoteStatus voteStatus) {
        updatedVoteCount = originalVoteCount + voteStatus.change;
        this.voteStatus = voteStatus;
    }
}
