package dev.abzikel.sistemaetr.fragments;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;

import java.text.DateFormat;
import java.util.Date;

import dev.abzikel.sistemaetr.ChangePasswordActivity;
import dev.abzikel.sistemaetr.MyTrainingsActivity;
import dev.abzikel.sistemaetr.ProfileActivity;
import dev.abzikel.sistemaetr.R;
import dev.abzikel.sistemaetr.SignInActivity;
import dev.abzikel.sistemaetr.pojos.User;
import dev.abzikel.sistemaetr.utils.FirebaseManager;

public class ProfileFragment extends Fragment {
    private TextView tvUsername;

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
        ImageView ivProfile = view.findViewById(R.id.ivProfile);
        tvUsername = view.findViewById(R.id.tvUsername);
        TextView tvRegistrationDate = view.findViewById(R.id.tvRegistrationDate);
        TextView tvPrecision = view.findViewById(R.id.tvPrecision);
        TextView tvReactionTime = view.findViewById(R.id.tvReactionTime);
        TextView tvTotalTrainings = view.findViewById(R.id.tvTotalTrainings);
        TextView tvMyTrainings = view.findViewById(R.id.tvMyTrainings);
        TextView tvChangePassword = view.findViewById(R.id.tvChangePassword);
        ImageButton btnEditProfile = view.findViewById(R.id.btnEditProfile);
        Button btnSignOut = view.findViewById(R.id.btnSignOut);

        // Get user data from Firebase
        User currentUser = FirebaseManager.getInstance().getCurrentUserData();

        // Verify if the current user is not null
        String formattedDate = getString(R.string.loading);
        if (currentUser != null) {
            // Verify if the creation date is already updated
            if (currentUser.getCreatedAt() != null) {
                // Format registration date
                Date registrationDate = currentUser.getCreatedAt();
                DateFormat dateFormat = DateFormat.getDateInstance(DateFormat.MEDIUM);
                formattedDate = dateFormat.format(registrationDate);
            }

            // Initialize views
            tvRegistrationDate.setText(getString(R.string.member_since, formattedDate));
            tvPrecision.setText(getString(R.string.accuracy_value, currentUser.getAccuracy()));
            tvReactionTime.setText(getString(R.string.reaction_time_value, currentUser.getAverageShotTime()));
            tvTotalTrainings.setText(getString(R.string.total_trainings_value, currentUser.getTotalTrainings()));
            if (FirebaseManager.getInstance().isPasswordProviderEnabled())
                tvChangePassword.setVisibility(View.VISIBLE);

            // Load profile image using Glide
            if (currentUser.getPhotoUrl() != null && !currentUser.getPhotoUrl().isEmpty()) {
                Glide.with(this)
                        .load(currentUser.getPhotoUrl())
                        .circleCrop()
                        .placeholder(R.drawable.ic_loading)
                        .error(R.drawable.ic_profile)
                        .into(ivProfile);
            }
        }

        // Add listeners
        btnEditProfile.setOnClickListener(v -> startActivity(new Intent(requireContext(), ProfileActivity.class)));
        tvMyTrainings.setOnClickListener(v -> startActivity(new Intent(requireContext(), MyTrainingsActivity.class)));
        tvChangePassword.setOnClickListener(v -> startActivity(new Intent(requireContext(), ChangePasswordActivity.class)));
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

    @Override
    public void onResume() {
        super.onResume();

        // Get user data from Firebase and update view
        User currentUser = FirebaseManager.getInstance().getCurrentUserData();
        String username = (currentUser != null && currentUser.getUsername() != null)
                ? currentUser.getUsername()
                : getString(R.string.loading);
        tvUsername.setText(username);
    }

}
