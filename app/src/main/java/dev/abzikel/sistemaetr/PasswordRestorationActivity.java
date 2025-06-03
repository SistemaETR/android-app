package dev.abzikel.sistemaetr;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

public class PasswordRestorationActivity extends AppCompatActivity {
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_password_restoration);
        setup();
    }

    private void setup() {
        // Link XML to Java
        EditText etvEmail = findViewById(R.id.etvEmail);
        Button btnSendEmail = findViewById(R.id.btnSendEmail);

        // Initialize Firebase Authentication
        mAuth = FirebaseAuth.getInstance();

        // Add click listener to send email button
        btnSendEmail.setOnClickListener(v -> {
            // Get text from EditText Views
            String email = etvEmail.getText().toString().trim();

            // Verifications
            if (email.isEmpty())
                Toast.makeText(this, getString(R.string.unfilled_fields), Toast.LENGTH_SHORT).show();
            else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches())
                Toast.makeText(this, getString(R.string.invalid_email), Toast.LENGTH_SHORT).show();
            else restorePassword(email);
        });
    }

    private void restorePassword(String email) {
        // Use Firebase Authentication to send password reset email
        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // Email sent successfully
                        Toast.makeText(PasswordRestorationActivity.this,
                                getString(R.string.email_sent),
                                Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        // Email sending failed
                        Toast.makeText(PasswordRestorationActivity.this,
                                getString(R.string.error_sending_email),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

}
