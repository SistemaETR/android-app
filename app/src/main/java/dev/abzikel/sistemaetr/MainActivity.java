package dev.abzikel.sistemaetr;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import dev.abzikel.sistemaetr.fragments.HomeFragment;
import dev.abzikel.sistemaetr.fragments.ProfileFragment;
import dev.abzikel.sistemaetr.pojos.User;
import dev.abzikel.sistemaetr.utils.BaseActivity;
import dev.abzikel.sistemaetr.utils.FirebaseManager;

public class MainActivity extends BaseActivity {
    private final HomeFragment homeFragment = new HomeFragment();
    private final ProfileFragment profileFragment = new ProfileFragment();
    private ActivityResultLauncher<String> requestPermissionLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Set layout and initialize activity
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Start listening for user's documents changes
        startListeningForUserChanges();

        // Request notification permission
        requestPermissionLauncher =
                registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                    if (isGranted) Log.d("Permissions", "Notification permission granted.");
                    else Log.d("Permissions", "Notification permission denied.");
                });
        askForNotificationPermission();

        // Initialize BottomNavigationView
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);

        // Set default fragment (HomeFragment)
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.nav_host_fragment, new HomeFragment())
                    .commit();
        }

        // Handle navigation item clicks
        bottomNavigationView.setOnItemSelectedListener(item -> {
            // Initialize selected fragment
            Fragment selectedFragment = null;

            // Check which item is selected
            int itemId = item.getItemId();
            if (itemId == R.id.navigation_home) selectedFragment = homeFragment;
            else if (itemId == R.id.navigation_profile) selectedFragment = profileFragment;

            // Replace the fragment
            if (selectedFragment != null) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.nav_host_fragment, selectedFragment)
                        .commit();
            }

            return true;
        });
    }

    private void startListeningForUserChanges() {
        // Start listening for user's documents changes
        FirebaseManager.getInstance().startListeningForUserChanges(this, new FirebaseManager.OnUserDataChangedListener() {
            @Override
            public void onDataChanged(User user) {
                // Log user data update
                Log.d("MainActivity", getString(R.string.user_data_updated) + user.getUsername());
            }

            @Override
            public void onError(Exception e) {
                // Handle errors
                Log.e("MainActivity", getString(R.string.error_listening_changes), e);
            }
        });
    }

    private void askForNotificationPermission() {
        // Only ask for permission if the device is running Android 13 (API 33) or higher
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Verify if the notification permission has been granted
            String permission = "android.permission.POST_NOTIFICATIONS";
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                // If the permission has not been granted, request it
                requestPermissionLauncher.launch(permission);
            }
        }
    }

}
