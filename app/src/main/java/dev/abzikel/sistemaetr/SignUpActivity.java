package dev.abzikel.sistemaetr;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import dev.abzikel.sistemaetr.pojos.User;

public class SignUpActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FirebaseApp.initializeApp(this);
        setContentView(R.layout.activity_sign_up);
        setup();
    }

    private void setup() {
        // Link XML to Java
        EditText etvEmail = findViewById(R.id.etvEmail);
        EditText etvUsername = findViewById(R.id.etvUsername);
        EditText etvPassword = findViewById(R.id.etvPassword);
        EditText etvConfirmPassword = findViewById(R.id.etvConfirmPassword);
        Button btnCreateAccount = findViewById(R.id.btnCreateAccount);

        // Add click listener to button
        btnCreateAccount.setOnClickListener(v -> {
            // Get text from EditText Views
            String email = etvEmail.getText().toString().trim();
            String username = etvUsername.getText().toString().trim();
            String password = etvPassword.getText().toString().trim();
            String confirmPassword = etvConfirmPassword.getText().toString().trim();

            // Verifications
            if (email.isEmpty() || username.isEmpty() || password.isEmpty() || confirmPassword.isEmpty())
                Toast.makeText(this, getString(R.string.unfilled_fields), Toast.LENGTH_SHORT).show();
            else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches())
                Toast.makeText(this, getString(R.string.invalid_email), Toast.LENGTH_SHORT).show();
            else if (username.length() < 4)
                Toast.makeText(this, getString(R.string.username_length), Toast.LENGTH_SHORT).show();
            else if (password.length() < 6)
                Toast.makeText(this, getString(R.string.password_length), Toast.LENGTH_SHORT).show();
            else if (!password.equals(confirmPassword))
                Toast.makeText(this, getString(R.string.unmatched_passwords), Toast.LENGTH_SHORT).show();
            else createAccount(email, username, password);
        });
    }

    private void createAccount(String email, String username, String password) {
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
                            userData.setUsername(username);
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
