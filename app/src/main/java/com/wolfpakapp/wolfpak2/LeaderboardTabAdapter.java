package com.wolfpakapp.wolfpak2;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;

/**
 * The LeaderboardAdapter provides a binding from a set of
 * {@link com.wolfpakapp.wolfpak2.LeaderboardListItem} objects to a
 * {@link RecyclerView}.
 *
 * @see android.support.v7.widget.RecyclerView.Adapter
 * @see RecyclerView
 * @see com.wolfpakapp.wolfpak2.LeaderboardTabAdapter.ViewHolder
 */
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

        public ViewHolder(View itemView) {
            super(itemView);
        }

        public void bindListItem(LeaderboardListItem leaderboardListItem) {
            this.leaderboardListItem = leaderboardListItem;
        }
    }
}
