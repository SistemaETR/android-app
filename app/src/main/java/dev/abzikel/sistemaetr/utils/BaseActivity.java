package dev.abzikel.sistemaetr.utils;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import dev.abzikel.sistemaetr.R;

public abstract class BaseActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Edge to edge support
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        // Apply the selected language before attaching the base context
        super.attachBaseContext(LocaleHelper.onAttach(newBase));
    }

    protected void setupToolbar(String title, boolean showUpButton) {
        // Initialize toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        View appBarLayout = findViewById(R.id.appBarLayout);
        if (appBarLayout != null) {
            // Set padding to the AppBarLayout
            ViewCompat.setOnApplyWindowInsetsListener(appBarLayout, (v, insets) -> {
                int statusBarHeight = insets.getInsets(WindowInsetsCompat.Type.systemBars()).top;
                v.setPadding(v.getPaddingLeft(), statusBarHeight, v.getPaddingRight(), v.getPaddingBottom());
                return insets;
            });
        }

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(title);
            if (showUpButton) {
                // Enable the Up button (back arrow)
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setDisplayShowHomeEnabled(true);
            }
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

}
