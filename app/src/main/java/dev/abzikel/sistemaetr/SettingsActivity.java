package dev.abzikel.sistemaetr;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatSeekBar;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.util.Locale;

import dev.abzikel.sistemaetr.services.BLEServerService;
import dev.abzikel.sistemaetr.utils.BaseActivity;
import dev.abzikel.sistemaetr.utils.SharedPreferencesManager;

public class SettingsActivity extends BaseActivity {
    private BLEServerService mService;
    private SharedPreferencesManager sharedPreferencesManager;
    private final Handler batteryUpdateHandler = new Handler();
    private final Runnable batteryUpdateRunnable = new Runnable() {
        @Override
        public void run() {
            updateBatteryStatus();
            batteryUpdateHandler.postDelayed(this, 3000);
        }
    };
    private final TextView[] tvBatteries = new TextView[6];
    private boolean blue = false, mBound = false;

    private final ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            BLEServerService.LocalBinder binder = (BLEServerService.LocalBinder) service;
            mService = binder.getService();
            mBound = true;

            // Change value of new mode
            mService.setMode(getResources().getString(R.string.configuration));
            mService.turnOnAllLed(false);

            // Start battery UI updating
            batteryUpdateHandler.post(batteryUpdateRunnable);
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            // Remove bound and callbacks
            mBound = false;
            batteryUpdateHandler.removeCallbacks(batteryUpdateRunnable);
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        setup();
    }

    private void setup() {
        // Link XML to Java
        ChipGroup chipGroupSensibility = findViewById(R.id.chipGroupSensibility);
        Chip chipLow = findViewById(R.id.chipLow);
        Chip chipNormal = findViewById(R.id.chipNormal);
        Chip chipHigh = findViewById(R.id.chipHigh);
        Chip chipTest = findViewById(R.id.chipTest);
        tvBatteries[0] = findViewById(R.id.tvBattery1);
        tvBatteries[1] = findViewById(R.id.tvBattery2);
        tvBatteries[2] = findViewById(R.id.tvBattery3);
        tvBatteries[3] = findViewById(R.id.tvBattery4);
        tvBatteries[4] = findViewById(R.id.tvBattery5);
        tvBatteries[5] = findViewById(R.id.tvBattery6);
        ImageView ivLED = findViewById(R.id.ivLED);
        SwitchCompat switchChangeLedColor = findViewById(R.id.switchChangeLedColor);
        AppCompatSeekBar seekBarIntensity = findViewById(R.id.seekBarIntensity);
        TextView tvIntensity = findViewById(R.id.tvIntensity);
        Button btnSaveConfiguration = findViewById(R.id.btnSaveConfiguration);

        // Initialize toolbar
        setupToolbar(getString(R.string.settings), true);

        // Initialize variables
        sharedPreferencesManager = SharedPreferencesManager.getInstance(this);
        int sensibility = Integer.parseInt(sharedPreferencesManager.getSensorSensibility());

        // Initialize views
        String textIntensity = getResources().getString(R.string.intensity) + " "
                + Integer.parseInt(sharedPreferencesManager.getLedRedIntensity())
                + "%";
        seekBarIntensity.setProgress(Integer.parseInt(sharedPreferencesManager.getLedRedIntensity()) - 1);
        tvIntensity.setText(textIntensity);

        switch (sensibility) {
            case 5:
                chipTest.setChecked(true);
                break;
            case 10:
                chipHigh.setChecked(true);
                break;
            case 20:
                chipLow.setChecked(true);
                break;
            default:
                chipNormal.setChecked(true);
                break;
        }

        // Add listeners
        switchChangeLedColor.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // Get color resource based on switch state
            int colorRes;
            if (isChecked) colorRes = R.color.blue;
            else colorRes = R.color.red;

            // Change LED color
            ivLED.setColorFilter(ContextCompat.getColor(this, colorRes));

            // Change LED intensity
            blue = isChecked;
            if (blue)
                seekBarIntensity.setProgress(Integer.parseInt(sharedPreferencesManager.getLedBlueIntensity()) - 1);
            else
                seekBarIntensity.setProgress(Integer.parseInt(sharedPreferencesManager.getLedRedIntensity()) - 1);
            if (mBound) mService.turnOnAllLed(blue);
        });

        seekBarIntensity.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                // Update text intensity
                String textIntensity = getResources().getString(R.string.intensity) + " ";
                textIntensity += (progress + 1) + "%";

                tvIntensity.setText(textIntensity);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // Change LED intensity
                String formattedIntensity = String.format(Locale.getDefault(), "%03d", (seekBarIntensity.getProgress() + 1));
                if (mBound) mService.turnOnAllLedTemp(blue, formattedIntensity);
            }
        });

        btnSaveConfiguration.setOnClickListener(v -> saveConfiguration(String.valueOf(seekBarIntensity.getProgress() + 1), chipGroupSensibility.getCheckedChipId()));
    }

    private void updateBatteryStatus() {
        if (mBound) {
            // Get batteries percentage
            byte[] batteries = mService.getBatteryStatus();

            for (int counter = 0; counter < batteries.length; counter++) {
                byte percentage = batteries[counter];

                // Update user interface
                Drawable drawable;
                if (percentage == -1)
                    drawable = ContextCompat.getDrawable(this, R.drawable.ic_disconnected);
                else if (percentage <= 5)
                    drawable = ContextCompat.getDrawable(this, R.drawable.ic_battery_empty);
                else if (percentage <= 20)
                    drawable = ContextCompat.getDrawable(this, R.drawable.ic_battery_20);
                else if (percentage <= 35)
                    drawable = ContextCompat.getDrawable(this, R.drawable.ic_battery_35);
                else if (percentage <= 50)
                    drawable = ContextCompat.getDrawable(this, R.drawable.ic_battery_50);
                else if (percentage <= 65)
                    drawable = ContextCompat.getDrawable(this, R.drawable.ic_battery_65);
                else if (percentage <= 80)
                    drawable = ContextCompat.getDrawable(this, R.drawable.ic_battery_80);
                else if (percentage <= 95)
                    drawable = ContextCompat.getDrawable(this, R.drawable.ic_battery_95);
                else
                    drawable = ContextCompat.getDrawable(this, R.drawable.ic_battery_full);

                // Get text battery
                String textBattery;
                if (percentage == -1) textBattery = getString(R.string.disconnected);
                else textBattery = percentage + "%";

                // Update text
                tvBatteries[counter].setText(textBattery);
                tvBatteries[counter].setCompoundDrawablesWithIntrinsicBounds(null, null, drawable, null);
            }
        }
    }

    private void saveConfiguration(String percentage, int radioBtnId) {
        // Save LED intensity configuration
        if (blue) sharedPreferencesManager.saveLedBlueIntensity(percentage);
        else sharedPreferencesManager.saveLedRedIntensity(percentage);

        // Save sensor sensibility configuration
        if (radioBtnId == R.id.chipLow) sharedPreferencesManager.saveSensorSensibility("20");
        else if (radioBtnId == R.id.chipNormal)
            sharedPreferencesManager.saveSensorSensibility("15");
        else if (radioBtnId == R.id.chipHigh)
            sharedPreferencesManager.saveSensorSensibility("10");
        else if (radioBtnId == R.id.chipTest)
            sharedPreferencesManager.saveSensorSensibility("05");

    }

    @Override
    protected void onStart() {
        super.onStart();

        // Change mode
        if (mBound) mService.setMode(getResources().getString(R.string.configuration));

        // Bind service
        Intent intent = new Intent(this, BLEServerService.class);
        bindService(intent, connection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        super.onStop();

        // Stop battery UI updating
        batteryUpdateHandler.removeCallbacks(batteryUpdateRunnable);

        // Unbind service
        if (mBound) {
            mService.setMode(getResources().getString(R.string.stop));
            unbindService(connection);
            mBound = false;
        }
    }

}
