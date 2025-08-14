package dev.abzikel.sistemaetr;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import dev.abzikel.sistemaetr.utils.BaseActivity;
import dev.abzikel.sistemaetr.utils.FirebaseManager;

public class ChangePasswordActivity extends BaseActivity {
    private TextInputLayout tilCurrentPassword, tilNewPassword, tilConfirmPassword;
    private TextInputEditText etvCurrentPassword, etvNewPassword, etvConfirmPassword;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_password);
        setup();
    }

    private void setup() {
        // Initialize toolbar
        setupToolbar(getString(R.string.change_password), true);

        // Link XML to Java
        tilCurrentPassword = findViewById(R.id.tilCurrentPassword);
        tilNewPassword = findViewById(R.id.tilNewPassword);
        tilConfirmPassword = findViewById(R.id.tilConfirmPassword);
        etvCurrentPassword = findViewById(R.id.etvCurrentPassword);
        etvNewPassword = findViewById(R.id.etvNewPassword);
        etvConfirmPassword = findViewById(R.id.etvConfirmPassword);
        MaterialButton btnChangePassword = findViewById(R.id.btnChangePassword);

        // Add text watchers
        etvCurrentPassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                tilCurrentPassword.setError(null);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        etvNewPassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                tilNewPassword.setError(null);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        etvConfirmPassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                tilConfirmPassword.setError(null);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        // Add listeners
        btnChangePassword.setOnClickListener(v -> changePassword());
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
            return;
        }
        if (newPassword.length() < 6) {
            tilNewPassword.setError(getString(R.string.password_length));
            return;
        }
        if (!newPassword.equals(confirmPassword)) {
            tilConfirmPassword.setError(getString(R.string.unmatched_passwords));
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
            }
        });
    }

}
