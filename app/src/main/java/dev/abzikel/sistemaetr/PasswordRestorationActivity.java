package dev.abzikel.sistemaetr;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;

import java.util.Objects;

import dev.abzikel.sistemaetr.utils.BaseActivity;
import dev.abzikel.sistemaetr.utils.ErrorClearingTextWatcher;
import dev.abzikel.sistemaetr.utils.OnSingleClickListener;

public class PasswordRestorationActivity extends BaseActivity {
    private FirebaseAuth mAuth;
    private TextInputLayout tilEmail;
    private TextInputEditText etvEmail;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_password_restoration);
        setup();
    }

    private void setup() {
        // Link XML to Java
        tilEmail = findViewById(R.id.tilEmail);
        etvEmail = findViewById(R.id.etvEmail);
        Button btnSendEmail = findViewById(R.id.btnSendEmail);

        // Initialize Firebase Authentication
        mAuth = FirebaseAuth.getInstance();

        // Add text watchers
        etvEmail.addTextChangedListener(new ErrorClearingTextWatcher(tilEmail));

        // Add click listener to send email button
        btnSendEmail.setOnClickListener(new OnSingleClickListener() {
            @Override
            public void onSingleClick(View v) {
                restorePassword();
            }
        });
    }

    private void restorePassword() {
        // Clear errors
        tilEmail.setError(null);

        // Get email
        String email = Objects.requireNonNull(etvEmail.getText()).toString().trim();

        // Make validations
        if (email.isEmpty()) {
            tilEmail.setError(getString(R.string.unfilled_fields));
            return;
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.setError(getString(R.string.invalid_email));
            return;
        }

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
