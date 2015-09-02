package com.wolfpakapp.wolfpak2;

import com.wolfpakapp.wolfpak2.leaderboard.LeaderboardFragment;

import org.json.JSONObject;

/**
 * The Post represents a single post. Every object stores info like the ID, handle, URL of the
 * image/video, and vote count.
 */
public class Post {
    private int id;
    private String handle;
    private boolean isImage;
    private String mediaUrl;
    private String thumbnailUrl;
    private int originalVoteCount;
    private int updatedVoteCount;
    private VoteStatus voteStatus;

    private Post(int id, String handle, boolean isImage, String mediaUrl, String thumbnailUrl,
                 int updatedVoteCount, VoteStatus voteStatus) {
        this.id = id;
        this.handle = handle;
        this.isImage = isImage;
        this.thumbnailUrl = thumbnailUrl;
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

    /**
     * Return a Post object created using the passed JSONObject. The tag determines how to parse the
     * JSONObject and instantiate the Post.
     *
     * @param tag        The tag of the current leaderboard tab (or null otherwise).
     * @param jsonObject The JSONObject of the post.
     * @return A new Post object.
     */
    public static Post parsePostJSONObject(String tag, JSONObject jsonObject) {
        int id = jsonObject.optInt("id");
        String handle = jsonObject.optString("handle");
        boolean isImage = jsonObject.optBoolean("is_image");
        String mediaUrl = jsonObject.optString("media_url");
        String thumbnailUrl = jsonObject.optString("thumbnail_url");
        int originalVoteCount = jsonObject.optInt("likes");
        // "Default" like status is 0
        int likeStatus = 0;
        if (tag == null) {
            tag = "";
        }
        switch (tag) {
            case LeaderboardFragment.LOCAL_TAG: {
                likeStatus = jsonObject.optInt("like_status");
                break;
            }
            case LeaderboardFragment.ALL_TIME_TAG: {
                likeStatus = 0;
                break;
            }
            case LeaderboardFragment.DEN_TAG: {
                // Since the user can't like his own post, the like status takes on a different
                // meaning. Maybe the color reflects the vote count?
                likeStatus = 0;
                break;
            }
        }
        return new Post(id, handle, isImage, mediaUrl, thumbnailUrl, originalVoteCount,
                VoteStatus.getVoteStatus(likeStatus));
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

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    public int getUpdatedVoteCount() {
        return updatedVoteCount;
    }


    public VoteStatus getVoteStatus() {
        return voteStatus;
    }

    /**
     * Set the item's vote status, and update the vote count.
     *
     * @param voteStatus The new VoteStatus.
     */
    public void setVoteStatus(VoteStatus voteStatus) {
        updatedVoteCount = originalVoteCount + voteStatus.change;
        this.voteStatus = voteStatus;
    }

    @Override
    public String toString() {
        return "Post{" +
                "handle='" + handle + '\'' +
                ", id=" + id +
                ", isImage=" + isImage +
                ", mediaUrl='" + mediaUrl + '\'' +
                ", originalVoteCount=" + originalVoteCount +
                ", updatedVoteCount=" + updatedVoteCount +
                ", voteStatus=" + voteStatus +
                '}';
    }
}
