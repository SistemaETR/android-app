package dev.abzikel.sistemaetr;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

import java.util.Objects;

public class SignInActivity extends AppCompatActivity {
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in);
        setup();
    }

    private void setup() {
        // Link XML to Java
        TextView tvCreateAccount = findViewById(R.id.tvCreateAccount);
        EditText etvEmail = findViewById(R.id.etvEmail);
        EditText etvPassword = findViewById(R.id.etvPassword);
        Button btnSignIn = findViewById(R.id.btnSignIn);
        Button btnGoogleSignIn = findViewById(R.id.btnGoogleSignIn);

        // Initialize Firebase Authentication
        mAuth = FirebaseAuth.getInstance();

        // Add click listener to sign in button
        btnSignIn.setOnClickListener(v -> {
            // Get text from EditText Views
            String email = etvEmail.getText().toString().trim();
            String password = etvPassword.getText().toString().trim();

            // Verifications
            if (email.isEmpty() || password.isEmpty())
                Toast.makeText(this, getString(R.string.unfilled_fields), Toast.LENGTH_SHORT).show();
            else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches())
                Toast.makeText(this, getString(R.string.invalid_email), Toast.LENGTH_SHORT).show();
            else signIn(email, password);
        });

        // Add click listener to text view to navigate to sign up screen
        tvCreateAccount.setOnClickListener(v ->
                startActivity(new Intent(SignInActivity.this, SignUpActivity.class)));
    }

    private void signIn(String email, String password) {
        // Use Firebase Authentication to sign in
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Login successful, navigate to home activity
                        Toast.makeText(SignInActivity.this,
                                getString(R.string.login_successful), Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(SignInActivity.this, HomeActivity.class));
                    } else {
                        // Login failed, display a message to the user
                        Toast.makeText(SignInActivity.this,
                                getString(R.string.error_signing_in) + " " + Objects.requireNonNull(task.getException()).getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

}
