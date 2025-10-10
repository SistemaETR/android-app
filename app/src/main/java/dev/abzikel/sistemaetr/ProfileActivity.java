package dev.abzikel.sistemaetr;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.yalantis.ucrop.UCrop;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import dev.abzikel.sistemaetr.pojos.User;
import dev.abzikel.sistemaetr.utils.BaseActivity;
import dev.abzikel.sistemaetr.utils.ErrorClearingTextWatcher;
import dev.abzikel.sistemaetr.utils.FirebaseManager;
import dev.abzikel.sistemaetr.utils.OnSingleClickListener;

public class ProfileActivity extends BaseActivity {
    private final Handler holdHandler = new Handler(Looper.getMainLooper());
    private ActivityResultLauncher<Intent> imagePickerLauncher;
    private Runnable deleteRunnable;
    private CountDownTimer countDownTimer;
    private User currentUser;
    private ImageView ivProfile;
    private TextInputLayout tilUsername;
    private TextInputEditText etvUsername;
    private ProgressBar progressBar;
    private MaterialButton btnSaveChanges, btnDeleteAccount;
    private Uri selectedImageUri;
    private boolean imageRemoved = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
        registerLaunchers();
        setup();
    }

    private void registerLaunchers() {
        // Register for the image picker result
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    // Handle the result
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri sourceUri = result.getData().getData();
                        if (sourceUri != null) {
                            // Create temp file for the crop result
                            String destinationFileName = "croppedImage_" + System.currentTimeMillis() + ".jpg";
                            Uri destinationUri = Uri.fromFile(new File(getCacheDir(), destinationFileName));
                            UCrop.of(sourceUri, destinationUri)
                                    .withAspectRatio(1f, 1f)
                                    .withMaxResultSize(1080, 1080)
                                    .start(this);
                        }
                    }
                }
        );
    }

    private void setup() {
        // Link XML to Java
        ivProfile = findViewById(R.id.ivProfile);
        TextView tvChangePhoto = findViewById(R.id.tvChangePhoto);
        tilUsername = findViewById(R.id.tilUsername);
        etvUsername = findViewById(R.id.etvUsername);
        progressBar = findViewById(R.id.progressBar);
        btnSaveChanges = findViewById(R.id.btnSaveChanges);
        btnDeleteAccount = findViewById(R.id.btnDeleteAccount);

        // Get user data from Firebase
        currentUser = FirebaseManager.getInstance().getCurrentUserData();

        // Verify if there is a user and try to load data
        if (currentUser != null) {
            etvUsername.setText(currentUser.getUsername());
            // Load profile image using Glide
            if (currentUser.getPhotoUrl() != null && !currentUser.getPhotoUrl().isEmpty()) {
                Glide.with(this)
                        .load(currentUser.getPhotoUrl())
                        .circleCrop()
                        .placeholder(R.drawable.ic_loading)
                        .error(R.drawable.ic_profile)
                        .into(ivProfile);
            }
        }

        // Add text watchers
        etvUsername.addTextChangedListener(new ErrorClearingTextWatcher(tilUsername));

        // Add listeners
        tvChangePhoto.setOnClickListener(new OnSingleClickListener() {
            @Override
            public void onSingleClick(View v) {
                displayMenu();
            }
        });
        btnSaveChanges.setOnClickListener(new OnSingleClickListener() {
            @Override
            public void onSingleClick(View v) {
                changeVisibility(false);
                saveChanges();
            }
        });
        setupDeleteButtonListener();
    }

    private void saveChanges() {
        // Clear errors
        tilUsername.setError(null);

        // Get new username
        Editable text = etvUsername.getText();
        String newUsername = (text != null) ? text.toString().trim() : "";

        // Make validations
        if (newUsername.isEmpty()) {
            tilUsername.setError(getString(R.string.unfilled_fields));
            changeVisibility(true);
            return;
        } else if (newUsername.length() < 4) {
            tilUsername.setError(getString(R.string.username_length));
            changeVisibility(true);
            return;
        } else if (currentUser != null && currentUser.getUsername() != null && newUsername.equals(currentUser.getUsername()) && !imageRemoved && selectedImageUri == null) {
            tilUsername.setError(getString(R.string.no_changes));
            changeVisibility(true);
        }

        // Verify if the image was removed
        if (imageRemoved) {
            // User removed the image
            FirebaseManager.getInstance().deleteProfileImage(this, new FirebaseManager.OnSimpleListener() {
                @Override
                public void onSuccess() {
                    updateFirestoreData(newUsername, "");
                }

                @Override
                public void onFailure(Exception e) {
                    Toast.makeText(ProfileActivity.this, getString(R.string.error_deleting_image), Toast.LENGTH_SHORT).show();
                    changeVisibility(true);
                }
            });
        } else if (selectedImageUri != null) {
            // User selected a new image
            FirebaseManager.getInstance().uploadProfileImage(this, selectedImageUri, new FirebaseManager.OnImageUploadListener() {
                @Override
                public void onSuccess(Uri downloadUri) {
                    updateFirestoreData(newUsername, downloadUri.toString());
                }

                @Override
                public void onFailure(Exception e) {
                    Toast.makeText(ProfileActivity.this, getString(R.string.error_uploading_image), Toast.LENGTH_SHORT).show();
                    changeVisibility(true);
                }
            });
        } else {
            // Verify if the username has changed
            if (currentUser == null || currentUser.getUsername() == null || !newUsername.equals(currentUser.getUsername())) {
                updateFirestoreData(newUsername, null);
            }
        }
    }

    private void updateFirestoreData(String newUsername, String photoUrl) {
        // Create map for changes
        Map<String, Object> updates = new HashMap<>();
        if (newUsername != null) updates.put("username", newUsername);
        if (photoUrl != null) updates.put("photoUrl", photoUrl);

        FirebaseManager.getInstance().updateUserProfile(this, updates, new FirebaseManager.OnSimpleListener() {
            @Override
            public void onSuccess() {
                Toast.makeText(ProfileActivity.this, getString(R.string.user_data_updated), Toast.LENGTH_SHORT).show();
                finish();
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(ProfileActivity.this, getString(R.string.error_saving_user), Toast.LENGTH_SHORT).show();
                changeVisibility(true);
            }
        });
    }

    private void openImagePicker() {
        // Check for permission
        if (android.os.Build.VERSION.SDK_INT <= android.os.Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE},
                        1001);
                return;
            }
        }

        // Open the image picker
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        imagePickerLauncher.launch(Intent.createChooser(intent, getString(R.string.select_image)));
    }

    private void displayMenu() {
        // Create an array of options for the image picker menu
        CharSequence[] options = new CharSequence[]{
                getString(R.string.select_image),
                getString(R.string.remove_image),
                getString(R.string.cancel)
        };

        // Create an AlertDialog with the options
        AlertDialog.Builder builder = new AlertDialog.Builder(ProfileActivity.this);
        builder.setItems(options, (dialog, which) -> {
            switch (which) {
                case 0:
                    openImagePicker();
                    break;
                case 1:
                    // Default image
                    ivProfile.setImageResource(R.drawable.ic_profile);
                    selectedImageUri = null;
                    imageRemoved = true;
                    break;
                case 2:
                    dialog.dismiss();
                    break;
            }
        });
        builder.show();
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setupDeleteButtonListener() {
        // Runnable to be executed after the hold delay
        deleteRunnable = this::performAccountDeletion;

        // Set a touch listener to detect press and release events
        btnDeleteAccount.setOnTouchListener((view, motionEvent) -> {
            switch (motionEvent.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    // Schedule the deletion to run after a 5-second delay
                    holdHandler.postDelayed(deleteRunnable, 5000);

                    // Start a countdown timer to update the button's text
                    countDownTimer = new CountDownTimer(5000, 1000) {
                        @Override
                        public void onTick(long millisUntilFinished) {
                            // Update text every second to show the remaining time
                            long secondsLeft = millisUntilFinished / 1000 + 1;
                            btnDeleteAccount.setText(getString(R.string.hold_to_confirm_countdown, secondsLeft));
                        }

                        @Override
                        public void onFinish() {
                            // Change text to "Deleting account..." when the countdown finishes
                            btnDeleteAccount.setText(getString(R.string.deleting_account));
                        }
                    }.start();

                    // Animate the button press for visual feedback.
                    view.animate().scaleX(0.95f).scaleY(0.95f).setDuration(150).start();
                    return true;

                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    // Cancel the scheduled deletion.
                    holdHandler.removeCallbacks(deleteRunnable);

                    // Cancel the text update countdown.
                    if (countDownTimer != null) countDownTimer.cancel();

                    // Restore the button to its original state.
                    view.animate().scaleX(1f).scaleY(1f).setDuration(150).start();
                    btnDeleteAccount.setText(getString(R.string.delete_account));
                    Toast.makeText(this, getString(R.string.deletion_canceled), Toast.LENGTH_SHORT).show();
                    return true;
            }
            return false;
        });
    }

    private void performAccountDeletion() {
        // Call the FirebaseManager to delete the user's Firestore document
        FirebaseManager.getInstance().deleteUserDocument(this, new FirebaseManager.OnSimpleListener() {
            @Override
            public void onSuccess() {
                // The Cloud Function will handle the rest (deleting auth, storage, etc.)
                Toast.makeText(ProfileActivity.this, getString(R.string.account_deleted), Toast.LENGTH_LONG).show();

                // Get the Firebase Auth instance
                FirebaseAuth auth = FirebaseAuth.getInstance();

                // Stop any active Firestore listeners to prevent memory leaks
                FirebaseManager.getInstance().stopListening();

                // Sign the user out locally
                auth.signOut();

                // Prepare to navigate the user away from the app's secure content
                Intent intent = new Intent(ProfileActivity.this, SignInActivity.class);

                // Add flags to clear the activity stack, preventing the user from navigating back.
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }

            @Override
            public void onFailure(Exception e) {
                // Callback for when the deletion fails
                Toast.makeText(ProfileActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();

                // Restore the button's text so the user can try again
                btnDeleteAccount.setText(getString(R.string.delete_account));
            }
        });
    }

    private void changeVisibility(boolean visibility) {
        if (visibility) {
            // Show button
            btnSaveChanges.setVisibility(View.VISIBLE);
            progressBar.setVisibility(View.GONE);
        } else {
            // Hide button
            btnSaveChanges.setVisibility(View.GONE);
            progressBar.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && requestCode == UCrop.REQUEST_CROP) {
            final Uri resultUri = UCrop.getOutput(data);
            if (resultUri != null) {
                // Update the profile image
                imageRemoved = false;
                selectedImageUri = resultUri;
                Glide.with(this)
                        .load(selectedImageUri)
                        .circleCrop()
                        .placeholder(R.drawable.ic_loading)
                        .error(R.drawable.ic_profile)
                        .into(ivProfile);
            }
        } else if (resultCode == UCrop.RESULT_ERROR) {
            final Throwable cropError = UCrop.getError(data);
            if (cropError != null) {
                Toast.makeText(this, R.string.error + cropError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1001) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, open the image picker
                openImagePicker();
            } else {
                // Permission denied, show a message to the user
                Toast.makeText(this, getResources().getString(R.string.error_image_permission), Toast.LENGTH_SHORT).show();
            }
        }
    }

}
