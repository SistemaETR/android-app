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

import java.text.DateFormat;
import java.util.Date;

import dev.abzikel.sistemaetr.R;
import dev.abzikel.sistemaetr.pojos.Training;

public class TrainingAdapter extends ListAdapter<Training, TrainingAdapter.TrainingViewHolder> {

    public TrainingAdapter() {
        super(DIFF_CALLBACK);
    }

    // DiffUtil to calculate efficiently the differences in the list
    private static final DiffUtil.ItemCallback<Training> DIFF_CALLBACK = new DiffUtil.ItemCallback<Training>() {
        @Override
        public boolean areItemsTheSame(@NonNull Training oldItem, @NonNull Training newItem) {
            return oldItem.getTrainingId().equals(newItem.getTrainingId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull Training oldItem, @NonNull Training newItem) {
            // Compare the contents of the old and new items
            return oldItem.getScore() == newItem.getScore() &&
                    oldItem.getHits() == newItem.getHits() &&
                    oldItem.getMisses() == newItem.getMisses();
        }
    };

    @NonNull
    @Override
    public TrainingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflate the layout for each item in the RecyclerView
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_training, parent, false);

        return new TrainingViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TrainingViewHolder holder, int position) {
        // Get current item and bind it to the ViewHolder
        Training training = getItem(position);
        holder.bind(training);
    }

    // ViewHolder that contains the views for each item
    class TrainingViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvModalityTitle, tvTrainingDate, tvScore, tvAccuracy, tvReactionTime, tvHits, tvMisses;
        private final Context context;

        public TrainingViewHolder(@NonNull View itemView) {
            super(itemView);
            context = itemView.getContext();

            // Link XML to Java
            tvModalityTitle = itemView.findViewById(R.id.tvModalityTitle);
            tvTrainingDate = itemView.findViewById(R.id.tvTrainingDate);
            tvScore = itemView.findViewById(R.id.tvScore);
            tvAccuracy = itemView.findViewById(R.id.tvAccuracy);
            tvReactionTime = itemView.findViewById(R.id.tvReactionTime);
            tvHits = itemView.findViewById(R.id.tvHits);
            tvMisses = itemView.findViewById(R.id.tvMisses);
        }

        public void bind(Training training) {
            // Modality
            tvModalityTitle.setText(getModalityString(training.getModality()));

            // Date and Time
            Date date = training.getCreatedAt();
            if (date != null) {
                // Format the date and time as a string
                DateFormat dateTimeFormat = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);
                tvTrainingDate.setText(dateTimeFormat.format(date));
            }

            // Score
            tvScore.setText(context.getString(R.string.score_value, training.getScore()));

            // Accuracy
            double totalShots = training.getHits() + training.getMisses();
            double accuracy = (totalShots > 0) ? (training.getHits() / totalShots) * 100.0 : 0.0;
            tvAccuracy.setText(context.getString(R.string.accuracy_value, accuracy));

            // Reaction time
            tvReactionTime.setText(context.getString(R.string.reaction_time_value, training.getAverageShotTime()));

            // Hits and misses
            tvHits.setText(context.getString(R.string.hits_value, training.getHits()));
            tvMisses.setText(context.getString(R.string.misses_value, training.getMisses()));
        }

        // Method to get the modality string based on the ID
        private String getModalityString(int modalityId) {
            switch (modalityId) {
                case 1:
                    return context.getString(R.string.short_weapon);
                case 2:
                    return context.getString(R.string.long_weapon);
                case 3:
                    return context.getString(R.string.reaction);
                case 4:
                    return context.getString(R.string.advanced_reaction);
                case 5:
                    return context.getString(R.string.infinite);
                default:
                    return context.getString(R.string.unknown_modality);
            }
        }
    }
}
