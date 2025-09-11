package dev.abzikel.sistemaetr.utils;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;

import java.util.Locale;

public class LocaleHelper {
    private static SharedPreferencesManager sharedPreferencesManager;

    public static Context onAttach(Context context) {
        // Initialize SharedPreferencesManager
        sharedPreferencesManager = SharedPreferencesManager.getInstance(context);

        // Get the selected language
        String lang = sharedPreferencesManager.getSelectedLanguage();
        return setLocale(context, lang);
    }

    public static String getLanguage() {
        return sharedPreferencesManager.getSelectedLanguage();
    }

    public static Context setLocale(Context context, String language) {
        // Save the selected language
        sharedPreferencesManager.setSelectedLanguage(language);
        Locale locale = new Locale(language);
        Locale.setDefault(locale);

        // Update the configuration
        Resources res = context.getResources();
        Configuration config = new Configuration(res.getConfiguration());
        config.setLocale(locale);
        return context.createConfigurationContext(config);
    }

}