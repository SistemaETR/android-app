package dev.abzikel.sistemaetr;

import android.app.Application;

import androidx.appcompat.app.AppCompatDelegate;

import com.google.firebase.FirebaseApp;
import com.google.firebase.appcheck.FirebaseAppCheck;
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory;
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory;
import com.google.firebase.crashlytics.FirebaseCrashlytics;

import dev.abzikel.sistemaetr.utils.SharedPreferencesManager;

public class MyApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        // Initialize Firebase
        FirebaseApp.initializeApp(this);

        // Disable crashlytics collection in debug mode
        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(!BuildConfig.DEBUG);

        // Initialize Firebase App Check
        FirebaseAppCheck firebaseAppCheck = FirebaseAppCheck.getInstance();

        if (BuildConfig.DEBUG) {
            // We're in a debug build, use the debug provider.
            firebaseAppCheck.installAppCheckProviderFactory(
                    DebugAppCheckProviderFactory.getInstance()
            );
        } else {
            // We're in a release build, use the Play Integrity provider.
            firebaseAppCheck.installAppCheckProviderFactory(
                    PlayIntegrityAppCheckProviderFactory.getInstance()
            );
        }

        // Initialize theme from SharedPreferences
        SharedPreferencesManager sharedPreferencesManager = SharedPreferencesManager.getInstance(this);
        int theme = sharedPreferencesManager.getTheme();
        AppCompatDelegate.setDefaultNightMode(theme);
    }

}
