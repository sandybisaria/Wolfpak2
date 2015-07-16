package com.wolfpakapp.wolfpak2;


import android.content.Context;
import android.content.res.Resources;

public class LeaderboardListItem {
    private int id;
    private String handle;
    private boolean isImage;
    private String mediaUrl;
    private int originalVoteCount;
    private int updatedVoteCount;
    private VoteStatus voteStatus;

    public LeaderboardListItem(int id, String handle, boolean isImage, String mediaUrl, int originalVoteCount, VoteStatus voteStatus) {
        this.id = id;
        this.handle = handle;
        this.isImage = isImage;
        this.mediaUrl = mediaUrl;
        this.originalVoteCount = originalVoteCount;
        // Simultaneously sets the vote status and updates the vote count based on it
        setVoteStatus(voteStatus);
    }

    public String getHandle() {
        return handle;
    }

    public String getMediaUrl() {
        return mediaUrl;
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

        static VoteStatus getVoteStatus(int change) {
            switch (change) {
                case -1:
                    return DOWNVOTED;
                case 1:
                    return UPVOTED;
                case 0:
                default:
                    return NOT_VOTED;
            }
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
