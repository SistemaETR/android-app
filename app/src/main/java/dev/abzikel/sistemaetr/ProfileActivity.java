package dev.abzikel.sistemaetr;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.widget.ImageView;
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
import com.yalantis.ucrop.UCrop;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import dev.abzikel.sistemaetr.pojos.User;
import dev.abzikel.sistemaetr.utils.BaseActivity;
import dev.abzikel.sistemaetr.utils.FirebaseManager;

public class ProfileActivity extends BaseActivity {
    private ActivityResultLauncher<Intent> imagePickerLauncher;
    private User currentUser;
    private ImageView ivProfile;
    private TextInputEditText etvUsername;
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
        etvUsername = findViewById(R.id.etvUsername);
        MaterialButton btnSaveChanges = findViewById(R.id.btnSaveChanges);

        // Initialize toolbar
        setupToolbar(getString(R.string.edit_profile), true);

        // Get user data from Firebase
        currentUser = FirebaseManager.getInstance().getCurrentUserData();

        // Initialize views
        etvUsername.setText(currentUser.getUsername());

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

        // Add listeners
        tvChangePhoto.setOnClickListener(v -> displayMenu());
        btnSaveChanges.setOnClickListener(v -> saveChanges());
    }

    private void saveChanges() {
        // Get new username from EditText
        Editable text = etvUsername.getText();
        String newUsername = (text != null) ? text.toString().trim() : "";

        // Check if there is nothing to change
        boolean usernameChanged = !newUsername.isEmpty() && !newUsername.equals(currentUser.getUsername());
        if (!usernameChanged && selectedImageUri == null && !imageRemoved) {
            Toast.makeText(this, getString(R.string.no_changes), Toast.LENGTH_SHORT).show();
            return;
        }

        // Verify if the image was removed
        if (imageRemoved) {
            // User removed the image
            FirebaseManager.getInstance().deleteProfileImage(this, new FirebaseManager.OnSimpleListener() {
                @Override
                public void onSuccess() {
                    updateFirestoreData(usernameChanged, newUsername, "");
                }

                @Override
                public void onFailure(Exception e) {
                    Toast.makeText(ProfileActivity.this, "Error al borrar la imagen.", Toast.LENGTH_SHORT).show();
                }
            });
        } else if (selectedImageUri != null) {
            // User selected a new image
            FirebaseManager.getInstance().uploadProfileImage(this, selectedImageUri, new FirebaseManager.OnImageUploadListener() {
                @Override
                public void onSuccess(Uri downloadUri) {
                    updateFirestoreData(usernameChanged, newUsername, downloadUri.toString());
                }

                @Override
                public void onFailure(Exception e) {
                    Toast.makeText(ProfileActivity.this, "Error al subir la imagen.", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            // The user has changed its username
            updateFirestoreData(usernameChanged, newUsername, null);
        }
    }

    private void updateFirestoreData(boolean usernameChanged, String newUsername, String photoUrl) {
        // There is nothing to change
        if (!usernameChanged && photoUrl == null) return;

        Map<String, Object> updates = new HashMap<>();
        if (usernameChanged) updates.put("username", newUsername);
        if (photoUrl != null) updates.put("photoUrl", photoUrl);

        FirebaseManager.getInstance().updateUserProfile(this, updates, new FirebaseManager.OnSimpleListener() {
            @Override
            public void onSuccess() {
                Toast.makeText(ProfileActivity.this, "Perfil actualizado.", Toast.LENGTH_SHORT).show();
                finish();
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(ProfileActivity.this, "Error al guardar los datos.", Toast.LENGTH_SHORT).show();
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
