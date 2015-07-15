package com.wolfpakapp.wolfpak2;


/**
 * The LeaderboardListItem class represents posts on the leaderboard.
 */
public class LeaderboardListItem {
    private String handle;
    private int originalVoteCount;

    public LeaderboardListItem(String handle, int originalVoteCount) {
        this.handle = handle;
        this.originalVoteCount = originalVoteCount;
    }

    public String getHandle() {
        return handle;
    }

    public int getOriginalVoteCount() {
        return originalVoteCount;
    }
}
