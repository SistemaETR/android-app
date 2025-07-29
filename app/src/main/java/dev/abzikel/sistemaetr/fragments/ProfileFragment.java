package dev.abzikel.sistemaetr.fragments;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;

import dev.abzikel.sistemaetr.R;
import dev.abzikel.sistemaetr.SignInActivity;
import dev.abzikel.sistemaetr.utils.FirebaseManager;

public class ProfileFragment extends Fragment {

    public ProfileFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Link XML to Java
        Button btnSignOut = view.findViewById(R.id.btnSignOut);

        // Add listeners
        btnSignOut.setOnClickListener(v -> signOut());
    }

    private void signOut() {
        // Inflate custom content dialog
        Context context = requireContext();
        LayoutInflater inflater = LayoutInflater.from(context);
        View dialogView = inflater.inflate(R.layout.content_dialog_confirmation, null);

        // Link XML to Java
        TextView tvMessage = dialogView.findViewById(R.id.tvMessage);
        Button btnPositive = dialogView.findViewById(R.id.btnPositive);
        Button btnNegative = dialogView.findViewById(R.id.btnNegative);

        // Initialize views
        tvMessage.setText(getResources().getString(R.string.sign_out_confirmation));
        btnPositive.setText(getResources().getString(R.string.sign_out));

        // Create Alert Dialog to ask confirmation about logout
        AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.CustomAlertDialogStyle);
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();

        // Add listeners
        btnPositive.setOnClickListener(v -> {
            // Connect to Firebase Authentication and get user
            FirebaseAuth auth = FirebaseAuth.getInstance();

            // Remove listener for user's documents changes
            FirebaseManager.getInstance().stopListening();

            // Sign out
            auth.signOut();

            // Return to login activity
            Toast.makeText(context, context.getResources().getString(R.string.session_closed), Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(context, SignInActivity.class);
            startActivity(intent);
            requireActivity().finish();

            // Dismiss dialog
            if (dialog.isShowing()) dialog.dismiss();
        });
        btnNegative.setOnClickListener(v -> dialog.dismiss());

        // Show Alert Dialog
        dialog.show();
    }

}
