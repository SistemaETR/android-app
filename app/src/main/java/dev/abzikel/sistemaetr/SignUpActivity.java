package dev.abzikel.sistemaetr;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Objects;

import dev.abzikel.sistemaetr.pojos.User;
import dev.abzikel.sistemaetr.utils.BaseActivity;
import dev.abzikel.sistemaetr.utils.ErrorClearingTextWatcher;
import dev.abzikel.sistemaetr.utils.OnSingleClickListener;

public class SignUpActivity extends BaseActivity {
    private TextInputLayout tilEmail, tilUsername, tilPassword, tilConfirmPassword;
    private TextInputEditText etvEmail, etvUsername, etvPassword, etvConfirmPassword;

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
        tilUsername = findViewById(R.id.tilUsername);
        tilPassword = findViewById(R.id.tilPassword);
        tilConfirmPassword = findViewById(R.id.tilConfirmPassword);
        etvEmail = findViewById(R.id.etvEmail);
        etvUsername = findViewById(R.id.etvUsername);
        etvPassword = findViewById(R.id.etvPassword);
        etvConfirmPassword = findViewById(R.id.etvConfirmPassword);
        Button btnCreateAccount = findViewById(R.id.btnCreateAccount);

        // Add text watchers
        etvEmail.addTextChangedListener(new ErrorClearingTextWatcher(tilEmail));
        etvUsername.addTextChangedListener(new ErrorClearingTextWatcher(tilUsername));
        etvPassword.addTextChangedListener(new ErrorClearingTextWatcher(tilPassword));
        etvConfirmPassword.addTextChangedListener(new ErrorClearingTextWatcher(tilConfirmPassword));

        // Add click listener to button
        btnCreateAccount.setOnClickListener(new OnSingleClickListener() {
            @Override
            public void onSingleClick(View v) {
                createAccount();
            }
        });
    }

    private void createAccount() {
        // Clear errors
        tilEmail.setError(null);
        tilUsername.setError(null);
        tilPassword.setError(null);
        tilConfirmPassword.setError(null);

        // Get email, username and passwords
        String email = Objects.requireNonNull(etvEmail.getText()).toString().trim();
        String username = Objects.requireNonNull(etvUsername.getText()).toString().trim();
        String password = Objects.requireNonNull(etvPassword.getText()).toString().trim();
        String confirmPassword = Objects.requireNonNull(etvConfirmPassword.getText()).toString().trim();

        // Make validations
        if (email.isEmpty()) {
            tilEmail.setError(getString(R.string.unfilled_fields));
            return;
        } else if (username.isEmpty()) {
            tilUsername.setError(getString(R.string.unfilled_fields));
            return;
        } else if (password.isEmpty()) {
            tilPassword.setError(getString(R.string.unfilled_fields));
            return;
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.setError(getString(R.string.invalid_email));
            return;
        } else if (username.length() < 4) {
            tilUsername.setError(getString(R.string.username_length));
            return;
        } else if (password.length() < 6) {
            tilPassword.setError(getString(R.string.password_length));
            return;
        } else if (!password.equals(confirmPassword)) {
            tilConfirmPassword.setError(getString(R.string.unmatched_passwords));
            return;
        }

        // Initialize Firebase Authentication and Firebase Firestore
        FirebaseAuth auth = FirebaseAuth.getInstance();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Create user in Firebase Authentication
        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Sign in success, get user from Firebase Authentication
                        FirebaseUser user = auth.getCurrentUser();
                        if (user != null) {
                            // Get user ID
                            String uid = user.getUid();

                            // Save user data in Firestore
                            User userData = new User();
                            userData.setUserId(uid);
                            userData.setEmail(email);
                            userData.setPhotoUrl("");
                            userData.setUsername(username);
                            userData.setTotalTrainings(0);
                            userData.setAverageShotTime(0.0);
                            userData.setAccuracy(0.0);

                            // Save user data in Firebase Firestore
                            db.collection("users").document(uid)
                                    .set(userData)
                                    .addOnSuccessListener(aVoid -> {
                                        // User data saved successfully
                                        Toast.makeText(this, getString(R.string.user_created), Toast.LENGTH_SHORT).show();
                                        finish();
                                    })
                                    .addOnFailureListener(e -> Toast.makeText(this, getString(R.string.error_saving_user), Toast.LENGTH_SHORT).show());
                        }
                    } else {
                        // If sign in fails, display a message to the user.
                        String errorMsg = task.getException() != null ? task.getException().getMessage() : getString(R.string.unknown_error);
                        Toast.makeText(this, getString(R.string.error_creating_account) + errorMsg, Toast.LENGTH_LONG).show();
                    }
                });
    }

}
