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
import dev.abzikel.sistemaetr.dialogs.BluetoothDialog;
import dev.abzikel.sistemaetr.utils.OnSingleClickListener;

public class HomeFragment extends Fragment {
    private String selectedMode;

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

        // Listen for the Bluetooth request
        getParentFragmentManager().setFragmentResultListener("bluetooth_request_training", this, (requestKey, bundle) -> {
            boolean isReady = bundle.getBoolean("isBluetoothReady");
            if (isReady) launchGameActivity();
        });

        // Link XML to Java
        LinearLayout btnShortWeapon = view.findViewById(R.id.btnShortWeapon);
        LinearLayout btnLongWeapon = view.findViewById(R.id.btnLongWeapon);
        LinearLayout btnReaction = view.findViewById(R.id.btnReaction);
        LinearLayout btnAdvancedReaction = view.findViewById(R.id.btnAdvancedReaction);
        LinearLayout btnInfinite = view.findViewById(R.id.btnInfinite);

        // Listeners
        btnShortWeapon.setOnClickListener(new OnSingleClickListener() {
            @Override
            public void onSingleClick(View v) {
                onModeSelected("Classic6");
            }
        });
        btnLongWeapon.setOnClickListener(new OnSingleClickListener() {
            @Override
            public void onSingleClick(View v) {
                onModeSelected("Classic12");
            }
        });
        btnReaction.setOnClickListener(new OnSingleClickListener() {
            @Override
            public void onSingleClick(View v) {
                onModeSelected("Reaction6");
            }
        });
        btnAdvancedReaction.setOnClickListener(new OnSingleClickListener() {
            @Override
            public void onSingleClick(View v) {
                onModeSelected("Advanced6");
            }
        });
        btnInfinite.setOnClickListener(new OnSingleClickListener() {
            @Override
            public void onSingleClick(View v) {
                onModeSelected("Infinite");
            }
        });
    }

    private void onModeSelected(String mode) {
        // Save selected mode
        selectedMode = mode;

        // Show dialog
        BluetoothDialog dialog = BluetoothDialog.newInstance("bluetooth_request_training");
        dialog.show(getParentFragmentManager(), "BluetoothCheckDialog");
    }

    public void launchGameActivity() {
        // Go to GameActivity
        if (selectedMode != null) {
            Intent activityIntent = new Intent(requireContext(), GameActivity.class);
            activityIntent.putExtra("mode", selectedMode);
            startActivity(activityIntent);
        }
    }

}
