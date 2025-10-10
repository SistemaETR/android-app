package dev.abzikel.sistemaetr;

import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import dev.abzikel.sistemaetr.utils.BaseActivity;
import dev.abzikel.sistemaetr.utils.ErrorClearingTextWatcher;
import dev.abzikel.sistemaetr.utils.FirebaseManager;
import dev.abzikel.sistemaetr.utils.OnSingleClickListener;

public class ChangePasswordActivity extends BaseActivity {
    private TextInputLayout tilCurrentPassword, tilNewPassword, tilConfirmPassword;
    private TextInputEditText etvCurrentPassword, etvNewPassword, etvConfirmPassword;
    private ProgressBar progressBar;
    private MaterialButton btnChangePassword;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_password);
        setup();
    }

    private void setup() {
        // Link XML to Java
        tilCurrentPassword = findViewById(R.id.tilCurrentPassword);
        tilNewPassword = findViewById(R.id.tilNewPassword);
        tilConfirmPassword = findViewById(R.id.tilConfirmPassword);
        etvCurrentPassword = findViewById(R.id.etvCurrentPassword);
        etvNewPassword = findViewById(R.id.etvNewPassword);
        etvConfirmPassword = findViewById(R.id.etvConfirmPassword);
        progressBar = findViewById(R.id.progressBar);
        btnChangePassword = findViewById(R.id.btnChangePassword);

        // Add text watchers
        etvCurrentPassword.addTextChangedListener(new ErrorClearingTextWatcher(tilCurrentPassword));
        etvNewPassword.addTextChangedListener(new ErrorClearingTextWatcher(tilNewPassword));
        etvConfirmPassword.addTextChangedListener(new ErrorClearingTextWatcher(tilConfirmPassword));

        // Add listeners
        btnChangePassword.setOnClickListener(new OnSingleClickListener() {
            @Override
            public void onSingleClick(View v) {
                changeVisibility(false);
                changePassword();
            }
        });
    }

    private void changePassword() {
        // Clear errors
        tilCurrentPassword.setError(null);
        tilNewPassword.setError(null);
        tilConfirmPassword.setError(null);

        // Get passwords
        String currentPassword = etvCurrentPassword.getText() != null ? etvCurrentPassword.getText().toString() : "";
        String newPassword = etvNewPassword.getText() != null ? etvNewPassword.getText().toString() : "";
        String confirmPassword = etvConfirmPassword.getText() != null ? etvConfirmPassword.getText().toString() : "";

        // Make validations
        if (currentPassword.isEmpty()) {
            tilCurrentPassword.setError(getString(R.string.incorrect_password));
            changeVisibility(true);
            return;
        }
        if (newPassword.length() < 6) {
            tilNewPassword.setError(getString(R.string.password_length));
            changeVisibility(true);
            return;
        }
        if (!newPassword.equals(confirmPassword)) {
            tilConfirmPassword.setError(getString(R.string.unmatched_passwords));
            changeVisibility(true);
            return;
        }

        // Change password
        FirebaseManager.getInstance().changeUserPassword(this, currentPassword, newPassword, new FirebaseManager.OnSimpleListener() {
            @Override
            public void onSuccess() {
                Toast.makeText(ChangePasswordActivity.this, getString(R.string.password_updated), Toast.LENGTH_SHORT).show();
                finish();
            }

            @Override
            public void onFailure(Exception e) {
                // Handle error
                String errorMessage = e.getMessage();
                if (errorMessage != null) {
                    if (e.getMessage().contains("CREDENTIAL_MISMATCH") || e.getMessage().contains("WRONG_PASSWORD") || e.getMessage().contains("incorrect")) {
                        tilCurrentPassword.setError(getString(R.string.incorrect_password));
                    } else {
                        Toast.makeText(ChangePasswordActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                }

                // Show button again
                changeVisibility(true);
            }
        });
    }

    private void changeVisibility(boolean visibility) {
        if (visibility) {
            // Show button
            btnChangePassword.setVisibility(View.VISIBLE);
            progressBar.setVisibility(View.GONE);
        } else {
            // Hide button
            btnChangePassword.setVisibility(View.GONE);
            progressBar.setVisibility(View.VISIBLE);
        }
    }

}
