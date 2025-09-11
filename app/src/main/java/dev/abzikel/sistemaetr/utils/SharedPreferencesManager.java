package dev.abzikel.sistemaetr.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import androidx.appcompat.app.AppCompatDelegate;

import java.util.List;
import java.util.Locale;

public class SharedPreferencesManager {
    private static final String MY_PREFERENCES = "myPrefs";
    private static final String SERVICE_UUID = "serviceUUID";
    private static final String MAC_ADDRESSES = "macAddresses";
    private static final String LED_RED_INTENSITY = "ledRedIntensity";
    private static final String LED_BLUE_INTENSITY = "ledBlueIntensity";
    private static final String SENSOR_SENSIBILITY = "sensorSensibility";
    private static final String THEME = "theme";
    private static final String SELECTED_LANGUAGE = "Locale.Helper.Selected.Language";
    private static SharedPreferencesManager instance;
    private final SharedPreferences sharedPreferences;

    private SharedPreferencesManager(Context context) {
        sharedPreferences = context.getSharedPreferences(MY_PREFERENCES, Context.MODE_PRIVATE);
    }

    public static synchronized SharedPreferencesManager getInstance(Context context) {
        if (instance == null) instance = new SharedPreferencesManager(context);

        return instance;
    }

    public void saveServiceUID(String serviceUUID) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(SERVICE_UUID, serviceUUID);
        editor.apply();
    }

    public String getServiceUUID() {
        return sharedPreferences.getString(SERVICE_UUID, "c7867f06-3b90-4e28-98ec-d23adf5a0013");
    }

    public void removeServiceUUID() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove(SERVICE_UUID);
        editor.apply();
    }

    public void saveMacAddresses(List<String> macAddresses) {
        String macAddressesStr = TextUtils.join(",", macAddresses);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(MAC_ADDRESSES, macAddressesStr);
        editor.apply();
    }

    public String[] getMacAddresses() {
        String savedStr = sharedPreferences.getString(MAC_ADDRESSES, "3C:E9:0E:6C:CD:56,EC:64:C9:96:FE:9E,3C:E9:0E:6C:ED:2E,3C:E9:0E:6C:BF:12,EC:64:C9:96:FE:96,3C:E9:0E:6C:CD:D2");

        if (!savedStr.isEmpty()) return savedStr.split(",");
        else return new String[0];
    }

    public void removeMacAddresses() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove(MAC_ADDRESSES);
        editor.apply();
    }


    public String getLedRedIntensity() {
        return sharedPreferences.getString(LED_RED_INTENSITY, "050");
    }

    public void saveLedRedIntensity(String ledRedIntensity) {
        String formattedIntensity = String.format(Locale.getDefault(), "%03d", Integer.parseInt(ledRedIntensity));
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(LED_RED_INTENSITY, formattedIntensity);
        editor.apply();
    }

    public void removeLedRedIntensity() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove(LED_RED_INTENSITY);
        editor.apply();
    }

    public String getLedBlueIntensity() {
        return sharedPreferences.getString(LED_BLUE_INTENSITY, "050");
    }

    public void saveLedBlueIntensity(String ledBlueIntensity) {
        String formattedIntensity = String.format(Locale.getDefault(), "%03d", Integer.parseInt(ledBlueIntensity));
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(LED_BLUE_INTENSITY, formattedIntensity);
        editor.apply();
    }

    public void removeLedBlueIntensity() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove(LED_BLUE_INTENSITY);
        editor.apply();
    }

    public String getSensorSensibility() {
        return sharedPreferences.getString(SENSOR_SENSIBILITY, "15");
    }

    public void saveSensorSensibility(String sensorSensibility) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(SENSOR_SENSIBILITY, sensorSensibility);
        editor.apply();
    }

    public void removeSensorSensibility() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove(SENSOR_SENSIBILITY);
        editor.apply();
    }

    public int getTheme() {
        return sharedPreferences.getInt(THEME, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
    }

    public void saveTheme(int theme) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(THEME, theme);
        editor.apply();
    }

    public void removeTheme() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove(THEME);
        editor.apply();
    }

    public String getSelectedLanguage() {
        return sharedPreferences.getString(SELECTED_LANGUAGE, Locale.getDefault().getLanguage());
    }

    public void setSelectedLanguage(String language) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(SELECTED_LANGUAGE, language);
        editor.apply();
    }

    public void removeSelectedLanguage() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove(SELECTED_LANGUAGE);
        editor.apply();
    }

}
