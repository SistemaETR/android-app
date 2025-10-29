package dev.abzikel.sistemaetr.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

import dev.abzikel.sistemaetr.R;
import dev.abzikel.sistemaetr.adapters.LeaderboardAdapter;
import dev.abzikel.sistemaetr.pojos.LeaderboardEntry;
import dev.abzikel.sistemaetr.utils.FirebaseManager;

public class LeaderboardFragment extends Fragment implements LeaderboardAdapter.OnItemClickListener {

    private static final String TAG = "LeaderboardFragment";
    private static final int PAGE_SIZE = 15;

    private RecyclerView rvLeaderboard;
    private LeaderboardAdapter adapter;
    private ProgressBar loadingProgressBar;
    private LinearLayout emptyStateLayout;
    private AutoCompleteTextView actvModality;

    private final List<LeaderboardEntry> entryList = new ArrayList<>();
    private DocumentSnapshot lastVisibleDocument = null;
    private boolean isLoading = false;
    private boolean isLastPage = false;
    private int selectedModalityId = -1;

    public LeaderboardFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_leaderboard, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Link views
        rvLeaderboard = view.findViewById(R.id.rvLeaderboard);
        loadingProgressBar = view.findViewById(R.id.loadingProgressBar);
        emptyStateLayout = view.findViewById(R.id.emptyStateLayout);
        actvModality = view.findViewById(R.id.actvModality);

        setupRecyclerView();
        setupModalityFilter();
    }

    private void setupRecyclerView() {
        adapter = new LeaderboardAdapter(this);
        rvLeaderboard.setAdapter(adapter);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        rvLeaderboard.setLayoutManager(layoutManager);

        // Listener to scroll and load more data when near the end
        rvLeaderboard.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                int visibleItemCount = layoutManager.getChildCount();
                int totalItemCount = layoutManager.getItemCount();
                int firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition();

                // Load more data when near the end
                if (!isLoading && !isLastPage) {
                    if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount
                            && firstVisibleItemPosition >= 0
                            && totalItemCount >= PAGE_SIZE) {
                        // Only load more data if all pages have been loaded
                        loadLeaderboardData(false);
                    }
                }
            }
        });
    }

    private void setupModalityFilter() {
        // Get modalities from resources
        String[] modalities = new String[]{
                getString(R.string.short_weapon),
                getString(R.string.long_weapon),
                getString(R.string.reaction),
                getString(R.string.advanced_reaction),
                getString(R.string.infinite)
        };
        ArrayAdapter<String> modalityAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, modalities);
        actvModality.setAdapter(modalityAdapter);

        // Listener for modality selection
        actvModality.setOnItemClickListener((parent, view, position, id) -> {
            selectedModalityId = position + 1;
            loadLeaderboardData(true);
        });

        // Select the first modality by default
        actvModality.setText(modalities[0], false);
        selectedModalityId = 1;
        loadLeaderboardData(true);
    }

    private void loadLeaderboardData(boolean isInitialLoad) {
        if (isLoading || isLastPage && !isInitialLoad) {
            return;
        }
        if (selectedModalityId == -1) {
            Log.w(TAG, "No modality selected, cannot load data.");
            return;
        }

        isLoading = true;
        loadingProgressBar.setVisibility(View.VISIBLE);
        emptyStateLayout.setVisibility(View.GONE);
        if (isInitialLoad) {
            entryList.clear();
            adapter.submitList(null);
            lastVisibleDocument = null;
            isLastPage = false;
        }

        Log.d(TAG, "Loading data for modality: " + selectedModalityId + ", starting after: " + (lastVisibleDocument != null ? lastVisibleDocument.getId() : "null"));

        // Fetch data from Firebase
        FirebaseManager.getInstance().fetchLeaderboard(selectedModalityId, PAGE_SIZE, lastVisibleDocument,
                new FirebaseManager.OnLeaderboardFetchedListener() {
                    @Override
                    public void onSuccess(List<LeaderboardEntry> entries, DocumentSnapshot lastVisible) {
                        if (!isAdded()) return;

                        loadingProgressBar.setVisibility(View.GONE);
                        isLoading = false;

                        if (entries.isEmpty() && isInitialLoad) {
                            emptyStateLayout.setVisibility(View.VISIBLE);
                            rvLeaderboard.setVisibility(View.GONE);
                        } else {
                            rvLeaderboard.setVisibility(View.VISIBLE);
                            entryList.addAll(entries);
                            adapter.submitList(new ArrayList<>(entryList));
                        }

                        lastVisibleDocument = lastVisible;
                        if (lastVisible == null || entries.size() < PAGE_SIZE) {
                            isLastPage = true;
                            Log.d(TAG, "Last page reached for modality: " + selectedModalityId);
                        } else {
                            Log.d(TAG, "Loaded page, last visible document: " + lastVisible.getId());
                        }
                    }

                    @Override
                    public void onFailure(Exception e) {
                        if (!isAdded()) return;
                        Log.e(TAG, "Error fetching leaderboard data", e);
                        loadingProgressBar.setVisibility(View.GONE);
                        isLoading = false;
                        Toast.makeText(getContext(), "Error leaderboard español", Toast.LENGTH_SHORT).show();
                        // Show empty state
                        if (isInitialLoad) {
                            emptyStateLayout.setVisibility(View.VISIBLE);
                            rvLeaderboard.setVisibility(View.GONE);
                        }
                    }
                });
    }

    // Implementation of LeaderboardAdapter.OnItemClickListener
    @Override
    public void onItemClick(LeaderboardEntry entry) {
        Toast.makeText(getContext(), "Clicked on: " + entry.getUsername(), Toast.LENGTH_SHORT).show();
    }

}