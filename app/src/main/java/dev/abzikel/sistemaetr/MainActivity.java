package dev.abzikel.sistemaetr;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import dev.abzikel.sistemaetr.fragments.HomeFragment;
import dev.abzikel.sistemaetr.fragments.ProfileFragment;

public class MainActivity extends AppCompatActivity {
    private final HomeFragment homeFragment = new HomeFragment();
    private final ProfileFragment profileFragment = new ProfileFragment();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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

}
