package dev.abzikel.sistemaetr;

import static com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL;

import android.content.Intent;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.credentials.Credential;
import androidx.credentials.CredentialManager;
import androidx.credentials.CredentialManagerCallback;
import androidx.credentials.CustomCredential;
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.GetCredentialResponse;
import androidx.credentials.exceptions.GetCredentialException;

import com.google.android.libraries.identity.googleid.GetGoogleIdOption;
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

import java.util.Objects;

public class SignInActivity extends AppCompatActivity {
    private FirebaseAuth mAuth;
    private CredentialManager credentialManager;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in);
        setup();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Check if user is signed in (non-null) and update UI accordingly.
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            // User is already logged in, navigate to BluetoothActivity
            startActivity(new Intent(SignInActivity.this, BluetoothActivity.class));
            finish();
        }
    }

    private void setup() {
        // Link XML to Java
        TextView tvForgotPassword = findViewById(R.id.tvForgotPassword);
        TextView tvCreateAccount = findViewById(R.id.tvCreateAccount);
        EditText etvEmail = findViewById(R.id.etvEmail);
        EditText etvPassword = findViewById(R.id.etvPassword);
        Button btnSignIn = findViewById(R.id.btnSignIn);
        Button btnGoogleSignIn = findViewById(R.id.btnGoogleSignIn);

        // Initialize Firebase Authentication and Credential Manager
        mAuth = FirebaseAuth.getInstance();
        credentialManager = CredentialManager.create(this);

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

        // Add click listener to google sign in button
        btnGoogleSignIn.setOnClickListener(v -> signInWithGoogle());

        // Add click listener to text view to navigate to password restoration screen
        tvForgotPassword.setOnClickListener(v ->
                startActivity(new Intent(SignInActivity.this, PasswordRestorationActivity.class)));

        // Add click listener to text view to navigate to sign up screen
        tvCreateAccount.setOnClickListener(v ->
                startActivity(new Intent(SignInActivity.this, SignUpActivity.class)));
    }

    private void signIn(String email, String password) {
        // Use Firebase Authentication to sign in
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Login successful, navigate to Bluetooth activity
                        Toast.makeText(SignInActivity.this,
                                getString(R.string.login_successful), Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(SignInActivity.this, BluetoothActivity.class));
                        finish();
                    } else {
                        // Login failed, display a message to the user
                        Toast.makeText(SignInActivity.this,
                                getString(R.string.error_signing_in) + " " + Objects.requireNonNull(task.getException()).getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void signInWithGoogle() {
        // Instantiate a Google sign-in request
        GetGoogleIdOption googleIdOption = new GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(getString(R.string.web_client_id))
                .build();

        // Create the Credential Manager request
        GetCredentialRequest request = new GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build();

        credentialManager.getCredentialAsync(
                this,
                request,
                new CancellationSignal(),
                ContextCompat.getMainExecutor(this),
                new CredentialManagerCallback<GetCredentialResponse, GetCredentialException>() {
                    @Override
                    public void onResult(GetCredentialResponse result) {
                        // Handle the result
                        handleSignIn(result);
                    }

                    @Override
                    public void onError(@NonNull GetCredentialException e) {
                        // Handle error
                        Toast.makeText(SignInActivity.this,
                                getString(R.string.error_signing_in) + " " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    private void handleSignIn(GetCredentialResponse result) {
        // Get the credential from the result
        Credential credential = result.getCredential();

        // Check if credential is of type Google ID
        if (credential instanceof CustomCredential) {
            // Cast the credential to Google ID
            CustomCredential customCredential = (CustomCredential) credential;
            if (customCredential.getType().equals(TYPE_GOOGLE_ID_TOKEN_CREDENTIAL)) {
                // Create Google ID Token
                Bundle credentialData = customCredential.getData();
                GoogleIdTokenCredential googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credentialData);

                // Sign in to Firebase with using the token
                firebaseAuthWithGoogle(googleIdTokenCredential.getIdToken());
            }
        } else {
            // Credential is not of type Google ID
            Log.w("SignInActivity", "Credential is not of type Google ID!");
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Sign in success, update UI with the signed-in user's information
                        Toast.makeText(SignInActivity.this,
                                getString(R.string.login_successful), Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(SignInActivity.this, BluetoothActivity.class));
                        finish();
                    } else {
                        // If sign in fails, display a message to the user
                        Toast.makeText(SignInActivity.this,
                                getString(R.string.error_signing_in) + " "
                                        + Objects.requireNonNull(task.getException()).getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

}
