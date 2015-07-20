package com.wolfpakapp.wolfpak2.leaderboard;


import android.content.Context;
import android.content.res.Resources;

import com.wolfpakapp.wolfpak2.R;

/**
 * The LeaderboardListItem represents a single list item on a leaderboard. Every object stores info
 * like the ID, handle, URL of the image/video, and vote count.
 */
public class LeaderboardListItem {
    private int id;
    private String handle;
    private boolean isImage;
    private String mediaUrl;
    private int originalVoteCount;
    private int updatedVoteCount;
    private VoteStatus voteStatus;

    public LeaderboardListItem(int id, String handle, boolean isImage, String mediaUrl,
                               int updatedVoteCount, VoteStatus voteStatus) {
        this.id = id;
        this.handle = handle;
        this.isImage = isImage;
        this.mediaUrl = mediaUrl;
        this.voteStatus = voteStatus;
        this.updatedVoteCount = updatedVoteCount;
        // Depending on the user's vote status, the original vote count may be different than what
        // the displayed vote count is.
        switch (voteStatus) {
            case NOT_VOTED: {
                originalVoteCount = updatedVoteCount;
                break;
            }
            case UPVOTED:
            case DOWNVOTED: {
                // This "negates" the user's up- or down-vote.
                originalVoteCount = updatedVoteCount - voteStatus.change;
                break;
            }
        }
    }

    public int getId() {
        return id;
    }

    public String getHandle() {
        return handle;
    }

    public boolean isImage() {
        return isImage;
    }

    public String getMediaUrl() {
        return mediaUrl;
    }

    public int getUpdatedVoteCount() {
        return updatedVoteCount;
    }

    /**
     * The VoteStatus enumeration represents the three states that a post can be in. Each status has
     * an associated color which is used to change the view count backgrounds.
     */
    public enum VoteStatus {
        NOT_VOTED(0), UPVOTED(1), DOWNVOTED(-1);
        public final int change;

        VoteStatus(int change) {
            this.change = change;
        }

        /**
         * Since the server uses integers to represent vote statuses, this method will return the
         * corresponding VoteStatus enum.
         * @param change The integral like status (from the server response).
         * @return The corresponding VoteStatus.
         */
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

        /**
         * @param context The Context which will be used to retrieve the color.
         * @return The color integer corresponding to the VoteStatus enum.
         */
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

    /**
     * Set the item's vote status, and update the vote count.
     * @param voteStatus The new VoteStatus.
     */
    public void setVoteStatus(VoteStatus voteStatus) {
        updatedVoteCount = originalVoteCount + voteStatus.change;
        this.voteStatus = voteStatus;
    }
}
