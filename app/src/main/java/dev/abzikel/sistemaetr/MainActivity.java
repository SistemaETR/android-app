package dev.abzikel.sistemaetr;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Set layout and initialize activity
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Start listening for user's documents changes
        startListeningForUserChanges();

        // Initialize toolbar
        setupToolbar(getString(R.string.app_name), false);

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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu
        getMenuInflater().inflate(R.menu.toolbar, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.configuration) {
            // Handle configuration item click (open SettingsActivity)
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);

            return true;
        }

        return super.onOptionsItemSelected(item);
    }

}
