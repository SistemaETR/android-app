package dev.abzikel.sistemaetr;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;

import java.util.Objects;

import dev.abzikel.sistemaetr.utils.BaseActivity;
import dev.abzikel.sistemaetr.utils.ErrorClearingTextWatcher;
import dev.abzikel.sistemaetr.utils.OnSingleClickListener;

public class SignUpActivity extends BaseActivity {
    private TextInputLayout tilEmail, tilPassword, tilConfirmPassword;
    private TextInputEditText etvEmail, etvPassword, etvConfirmPassword;
    private ProgressBar progressBar;
    private Button btnCreateAccount;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FirebaseApp.initializeApp(this);
        setContentView(R.layout.activity_sign_up);
        setup();
    }

    private void setup() {
        // Link XML to Java
        tilEmail = findViewById(R.id.tilEmail);
        tilPassword = findViewById(R.id.tilPassword);
        tilConfirmPassword = findViewById(R.id.tilConfirmPassword);
        etvEmail = findViewById(R.id.etvEmail);
        etvPassword = findViewById(R.id.etvPassword);
        etvConfirmPassword = findViewById(R.id.etvConfirmPassword);
        progressBar = findViewById(R.id.progressBar);
        btnCreateAccount = findViewById(R.id.btnCreateAccount);

        // Add text watchers
        etvEmail.addTextChangedListener(new ErrorClearingTextWatcher(tilEmail));
        etvPassword.addTextChangedListener(new ErrorClearingTextWatcher(tilPassword));
        etvConfirmPassword.addTextChangedListener(new ErrorClearingTextWatcher(tilConfirmPassword));

        // Add click listener to button
        btnCreateAccount.setOnClickListener(new OnSingleClickListener() {
            @Override
            public void onSingleClick(View v) {
                changeVisibility(false);
                createAccount();
            }
        });
    }

    private void createAccount() {
        // Clear errors
        tilEmail.setError(null);
        tilPassword.setError(null);
        tilConfirmPassword.setError(null);

        // Get email, username and passwords
        String email = Objects.requireNonNull(etvEmail.getText()).toString().trim();
        String password = Objects.requireNonNull(etvPassword.getText()).toString().trim();
        String confirmPassword = Objects.requireNonNull(etvConfirmPassword.getText()).toString().trim();

        // Make validations
        if (email.isEmpty()) {
            tilEmail.setError(getString(R.string.unfilled_fields));
            changeVisibility(true);
            return;
        } else if (password.isEmpty()) {
            tilPassword.setError(getString(R.string.unfilled_fields));
            changeVisibility(true);
            return;
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.setError(getString(R.string.invalid_email));
            changeVisibility(true);
            return;
        } else if (password.length() < 6) {
            tilPassword.setError(getString(R.string.password_length));
            changeVisibility(true);
            return;
        } else if (!password.equals(confirmPassword)) {
            tilConfirmPassword.setError(getString(R.string.unmatched_passwords));
            changeVisibility(true);
            return;
        }

        // Initialize Firebase Authentication and Firebase Firestore
        FirebaseAuth auth = FirebaseAuth.getInstance();

        // Create user in Firebase Authentication
        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // User data saved successfully
                        Toast.makeText(this, getString(R.string.user_created), Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        // If sign in fails, display a message to the user.
                        String errorMsg = task.getException() != null ? task.getException().getMessage() : getString(R.string.unknown_error);
                        Toast.makeText(this, getString(R.string.error_creating_account) + errorMsg, Toast.LENGTH_LONG).show();
                        changeVisibility(true);
                    }
                });
    }

    private void changeVisibility(boolean visibility) {
        if (visibility) {
            // Show button
            btnCreateAccount.setVisibility(View.VISIBLE);
            progressBar.setVisibility(View.GONE);
        } else {
            // Hide button
            btnCreateAccount.setVisibility(View.GONE);
            progressBar.setVisibility(View.VISIBLE);
        }
    }

}
