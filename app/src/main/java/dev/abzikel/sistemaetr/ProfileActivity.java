package dev.abzikel.sistemaetr;

import android.os.Bundle;
import android.text.Editable;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import dev.abzikel.sistemaetr.pojos.User;
import dev.abzikel.sistemaetr.utils.BaseActivity;
import dev.abzikel.sistemaetr.utils.FirebaseManager;

public class ProfileActivity extends BaseActivity {
    private User currentUser;
    private TextInputEditText etvUsername;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
        setup();
    }

    private void setup() {
        // Link XML to Java
        etvUsername = findViewById(R.id.etvUsername);
        MaterialButton btnSaveChanges = findViewById(R.id.btnSaveChanges);

        // Initialize toolbar
        setupToolbar(getString(R.string.edit_profile), true);

        // Get user data from Firebase
        currentUser = FirebaseManager.getInstance().getCurrentUserData();

        // Initialize views
        etvUsername.setText(currentUser.getUsername());

        // Add listeners
        btnSaveChanges.setOnClickListener(v -> changeUsername());
    }

    private void changeUsername() {
        // Get new username from EditText
        Editable text = etvUsername.getText();
        String newUsername = (text != null) ? text.toString().trim() : "";

        // Make validation
        if (newUsername.isEmpty()) {
            // Show error message if fields are empty
            Toast.makeText(this, getString(R.string.unfilled_fields), Toast.LENGTH_SHORT).show();
            return;
        }
        if (newUsername.equals(currentUser.getUsername())) {
            // Show error message if there are no changes to save
            Toast.makeText(this, getString(R.string.no_changes), Toast.LENGTH_SHORT).show();
            return;
        }
        if (newUsername.length() < 4) {
            // Show error message if username is too short
            Toast.makeText(this, getString(R.string.username_length), Toast.LENGTH_SHORT).show();
            return;
        }

        // Update username in Firebase
        FirebaseManager.getInstance().updateUsername(this, newUsername, new FirebaseManager.OnUserUpdateListener() {
            @Override
            public void onSuccess() {
                // Show success message and update user object
                Toast.makeText(ProfileActivity.this, getString(R.string.user_data_updated), Toast.LENGTH_SHORT).show();
                currentUser.setUsername(newUsername);
                finish();
            }

            @Override
            public void onFailure(Exception e) {
                // Show error message if update fails
                Toast.makeText(ProfileActivity.this, getString(R.string.error_saving_user) + ": " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

}
