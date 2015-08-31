package com.wolfpakapp.wolfpak2;

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
    public static VoteStatus getVoteStatus(int change) {
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
}