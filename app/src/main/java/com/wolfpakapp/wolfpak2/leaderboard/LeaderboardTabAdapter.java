package com.wolfpakapp.wolfpak2.leaderboard;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.wolfpakapp.wolfpak2.Post;
import com.wolfpakapp.wolfpak2.R;

import java.util.ArrayList;

/**
 * The LeaderboardTabAdapter provides a binding from the list of Posts to the RecyclerView. It
 * contains the ViewHolder class that is responsible for each post's view.
 */
public class LeaderboardTabAdapter extends RecyclerView.Adapter<LeaderboardTabAdapter.ViewHolder> {
    private ArrayList<Post> mPosts;
    private LeaderboardTabManager mParentManager;

    public LeaderboardTabAdapter(ArrayList<Post> posts, LeaderboardTabManager parentManager) {
        mPosts = posts;
        mParentManager = parentManager;
    }

    @Override
    public int getItemCount() {
        return mPosts.size();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_leaderboard, parent, false);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        holder.bindPost(mPosts.get(position));
    }

    /**
     * The ViewHolder class describes the view for each post and initializes its elements.
     */
    public class ViewHolder extends RecyclerView.ViewHolder {
        private TextView handleTextView;
        private VoteCountTextView voteCountTextView;

        private ThumbnailImageView thumbnailImageView;

        public ViewHolder(View itemView) {
            super(itemView);

            handleTextView = (TextView) itemView
                    .findViewById(R.id.leaderboard_item_handle_text_view);
            voteCountTextView = (VoteCountTextView) itemView
                    .findViewById(R.id.leaderboard_item_vote_count_text_view);
            thumbnailImageView = (ThumbnailImageView) itemView
                    .findViewById(R.id.leaderboard_item_thumbnail_image_view);
        }

        public void bindPost(Post post) {
            handleTextView.setText(post.getHandle());

            voteCountTextView.initialize(mParentManager, post, itemView);
            thumbnailImageView.initialize(mParentManager, post, itemView);
        }
    }
}
