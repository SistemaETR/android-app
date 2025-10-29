package dev.abzikel.sistemaetr.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.imageview.ShapeableImageView;

import dev.abzikel.sistemaetr.R;
import dev.abzikel.sistemaetr.pojos.LeaderboardEntry;

public class LeaderboardAdapter extends ListAdapter<LeaderboardEntry, LeaderboardAdapter.LeaderboardViewHolder> {

    // Interface for handling item clicks
    public interface OnItemClickListener {
        void onItemClick(LeaderboardEntry entry);
    }

    private final OnItemClickListener listener;

    // Constructor for the adapter
    public LeaderboardAdapter(OnItemClickListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    // DiffUtil callback for efficient list updates
    private static final DiffUtil.ItemCallback<LeaderboardEntry> DIFF_CALLBACK = new DiffUtil.ItemCallback<LeaderboardEntry>() {
        @Override
        public boolean areItemsTheSame(@NonNull LeaderboardEntry oldItem, @NonNull LeaderboardEntry newItem) {
            // Use leaderboardId as the unique identifier for each item
            return oldItem.getLeaderboardId().equals(newItem.getLeaderboardId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull LeaderboardEntry oldItem, @NonNull LeaderboardEntry newItem) {
            // Check if the displayed content has changed
            return oldItem.getScore() == newItem.getScore() &&
                    oldItem.getUsername().equals(newItem.getUsername()) &&
                    oldItem.getPhotoUrl().equals(newItem.getPhotoUrl());
        }
    };

    @NonNull
    @Override
    public LeaderboardViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflate the item layout
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_leaderboard, parent, false);
        return new LeaderboardViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull LeaderboardViewHolder holder, int position) {
        // Bind data from the Leaderboard object to the ViewHolder's views
        LeaderboardEntry currentEntry = getItem(position);

        // We pass the rank based on the position in the list
        holder.bind(currentEntry, position + 1, listener);
    }

    // ViewHolder class holds references to the views in item_leaderboard.xml
    static class LeaderboardViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvRank;
        private final ShapeableImageView ivProfile;
        private final TextView tvUsername;
        private final TextView tvScore;

        public LeaderboardViewHolder(@NonNull View itemView) {
            super(itemView);

            // Link XML to Java
            tvRank = itemView.findViewById(R.id.tvRank);
            ivProfile = itemView.findViewById(R.id.ivProfile);
            tvUsername = itemView.findViewById(R.id.tvUsername);
            tvScore = itemView.findViewById(R.id.tvScore);
        }

        public void bind(final LeaderboardEntry entry, final int rank, final OnItemClickListener listener) {
            // Get the context
            Context context = itemView.getContext();

            // Set the rank text
            tvRank.setText(String.valueOf(rank));

            // Load the profile image using Glide
            Glide.with(context)
                    .load(entry.getPhotoUrl())
                    .circleCrop()
                    .placeholder(R.drawable.ic_loading)
                    .error(R.drawable.ic_profile)
                    .into(ivProfile);

            // Set the username
            tvUsername.setText(entry.getUsername());

            // Set the score, using a formatted string resource for better readability
            tvScore.setText(context.getString(R.string.score_value_leaderboard, entry.getScore()));

            // Set the click listener for the entire item view
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onItemClick(entry);
                }
            });
        }
    }
}