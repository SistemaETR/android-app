package dev.abzikel.sistemaetr.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import dev.abzikel.sistemaetr.GameActivity;
import dev.abzikel.sistemaetr.R;

public class HomeFragment extends Fragment {

    public HomeFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Link XML to Java
        LinearLayout btnShortWeapon = view.findViewById(R.id.btnShortWeapon);
        LinearLayout btnLongWeapon = view.findViewById(R.id.btnLongWeapon);
        LinearLayout btnReaction = view.findViewById(R.id.btnReaction);
        LinearLayout btnAdvancedReaction = view.findViewById(R.id.btnAdvancedReaction);
        LinearLayout btnInfinite = view.findViewById(R.id.btnInfinite);

        // Listeners
        btnShortWeapon.setOnClickListener(v -> changeActivity("Classic6"));
        btnLongWeapon.setOnClickListener(v -> changeActivity("Classic12"));
        btnReaction.setOnClickListener(v -> changeActivity("Reaction6"));
        btnAdvancedReaction.setOnClickListener(v -> changeActivity("Advanced6"));
        btnInfinite.setOnClickListener(v -> changeActivity("Infinite"));
    }

    private void changeActivity(String gameMode) {
        // Go to control activity
        Intent intent = new Intent(requireContext(), GameActivity.class);
        intent.putExtra("mode", gameMode);
        startActivity(intent);
    }

}
