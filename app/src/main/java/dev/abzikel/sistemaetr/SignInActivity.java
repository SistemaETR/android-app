package dev.abzikel.sistemaetr;

import static com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL;

import android.content.Intent;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
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
import androidx.credentials.exceptions.GetCredentialCancellationException;
import androidx.credentials.exceptions.GetCredentialException;
import androidx.credentials.exceptions.NoCredentialException;

import com.bumptech.glide.Glide;
import com.google.android.libraries.identity.googleid.GetGoogleIdOption;
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.AdditionalUserInfo;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.crashlytics.FirebaseCrashlytics;

import java.util.Objects;

import dev.abzikel.sistemaetr.pojos.User;
import dev.abzikel.sistemaetr.utils.ErrorClearingTextWatcher;
import dev.abzikel.sistemaetr.utils.FirebaseManager;

public class SignInActivity extends AppCompatActivity {
    private FirebaseAuth mAuth;
    private CredentialManager credentialManager;
    private TextInputLayout tilEmail, tilPassword;
    private TextInputEditText etvEmail, etvPassword;

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
        ImageView ivLogo = findViewById(R.id.ivLogo);
        TextView tvForgotPassword = findViewById(R.id.tvForgotPassword);
        TextView tvCreateAccount = findViewById(R.id.tvCreateAccount);
        tilEmail = findViewById(R.id.tilEmail);
        tilPassword = findViewById(R.id.tilPassword);
        etvEmail = findViewById(R.id.etvEmail);
        etvPassword = findViewById(R.id.etvPassword);
        Button btnSignIn = findViewById(R.id.btnSignIn);
        Button btnGoogleSignIn = findViewById(R.id.btnGoogleSignIn);

        // Initialize view
        Glide.with(this)
                .load(R.drawable.logo)
                .circleCrop()
                .into(ivLogo);

        // Initialize Firebase Authentication and Credential Manager
        mAuth = FirebaseAuth.getInstance();
        credentialManager = CredentialManager.create(this);

        // Add text watchers
        etvEmail.addTextChangedListener(new ErrorClearingTextWatcher(tilEmail));
        etvPassword.addTextChangedListener(new ErrorClearingTextWatcher(tilPassword));

        // Add click listener to sign in button
        btnSignIn.setOnClickListener(v -> signIn());

        // Add click listener to google sign in button
        btnGoogleSignIn.setOnClickListener(v -> signInWithGoogle());

        // Add click listener to text view to navigate to password restoration screen
        tvForgotPassword.setOnClickListener(v ->
                startActivity(new Intent(SignInActivity.this, PasswordRestorationActivity.class)));

        // Add click listener to text view to navigate to sign up screen
        tvCreateAccount.setOnClickListener(v ->
                startActivity(new Intent(SignInActivity.this, SignUpActivity.class)));
    }

    private void signIn() {
        // Clear errors
        tilEmail.setError(null);
        tilPassword.setError(null);

        // Get email and password
        String email = Objects.requireNonNull(etvEmail.getText()).toString().trim();
        String password = Objects.requireNonNull(etvPassword.getText()).toString().trim();

        // Make validations
        if (email.isEmpty()) {
            tilEmail.setError(getString(R.string.unfilled_fields));
            return;
        } else if (password.isEmpty()) {
            tilPassword.setError(getString(R.string.unfilled_fields));
            return;
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.setError(getString(R.string.invalid_email));
            return;
        }

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
                        if (e instanceof GetCredentialCancellationException) {
                            // The user canceled the sign-in flow
                            Log.d("SignInGoogle", getString(R.string.user_canceled_dialog));
                        } else if (e instanceof NoCredentialException) {
                            // No credentials were returned
                            String message = getString(R.string.no_google_account_credentials);
                            Log.d("SignInGoogle", message);
                            Toast.makeText(SignInActivity.this, message, Toast.LENGTH_SHORT).show();
                        } else {
                            // Handle other errors
                            Toast.makeText(SignInActivity.this,
                                    getString(R.string.error_signing_in) + " " + e.getMessage(),
                                    Toast.LENGTH_SHORT).show();
                            FirebaseCrashlytics.getInstance().recordException(e);
                        }
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
                        // Get the result of the sign in and the user information
                        AuthResult authResult = task.getResult();
                        FirebaseUser firebaseUser = authResult.getUser();
                        AdditionalUserInfo additionalUserInfo = authResult.getAdditionalUserInfo();

                        // Verify if the user is new and has the necessary information
                        if (additionalUserInfo != null && additionalUserInfo.isNewUser() && firebaseUser != null) {
                            // New user, create a new profile
                            User newUser = new User();
                            newUser.setUserId(firebaseUser.getUid());
                            newUser.setEmail(firebaseUser.getEmail());

                            // Generate a unique username and set photo url to empty
                            String tempUsername = "user" + firebaseUser.getUid().substring(0, 8);
                            newUser.setUsername(tempUsername);
                            newUser.setPhotoUrl("");

                            // Initialize statistics
                            newUser.setTotalTrainings(0);
                            newUser.setAverageShotTime(0.0);
                            newUser.setAccuracy(0.0);

                            FirebaseManager.getInstance().createUserDocument(this, newUser, new FirebaseManager.OnSimpleListener() {
                                @Override
                                public void onSuccess() {
                                    // Document created successfully, navigate to Bluetooth activity
                                    Toast.makeText(SignInActivity.this,
                                            getString(R.string.login_successful), Toast.LENGTH_SHORT).show();
                                    startActivity(new Intent(SignInActivity.this, BluetoothActivity.class));
                                    finish();
                                }

                                @Override
                                public void onFailure(Exception e) {
                                    // Handle error
                                    FirebaseAuth.getInstance().signOut();
                                    Toast.makeText(SignInActivity.this, getString(R.string.error_creating_account), Toast.LENGTH_SHORT).show();
                                }
                            });
                        } else {
                            // Login successful, navigate to Bluetooth activity
                            Toast.makeText(SignInActivity.this,
                                    getString(R.string.login_successful), Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(SignInActivity.this, BluetoothActivity.class));
                            finish();
                        }
                    } else {
                        // Login failed, display a message to the user
                        Toast.makeText(SignInActivity.this,
                                getString(R.string.error_signing_in) + " " + Objects.requireNonNull(task.getException()).getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

}
