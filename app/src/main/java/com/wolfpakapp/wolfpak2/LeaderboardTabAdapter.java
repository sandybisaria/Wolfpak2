package com.wolfpakapp.wolfpak2;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;

public class LeaderboardTabAdapter extends RecyclerView.Adapter<LeaderboardTabAdapter.ViewHolder> {
    ArrayList<LeaderboardListItem> leaderboardListItems;

    public LeaderboardTabAdapter(ArrayList<LeaderboardListItem> leaderboardListItems) {
        this.leaderboardListItems = leaderboardListItems;
    }

    @Override
    public int getItemCount() {
        return leaderboardListItems.size();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_leaderboard, parent, false);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        holder.bindListItem(leaderboardListItems.get(position));
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        LeaderboardListItem leaderboardListItem;

        TextView handleTextView;

        public ViewHolder(View itemView) {
            super(itemView);

            handleTextView = (TextView) itemView
                    .findViewById(R.id.leaderboard_item_handle_text_view);
        }

        public void bindListItem(LeaderboardListItem leaderboardListItem) {
            this.leaderboardListItem = leaderboardListItem;

            handleTextView.setText(leaderboardListItem.getHandle());
        }
    }
}
