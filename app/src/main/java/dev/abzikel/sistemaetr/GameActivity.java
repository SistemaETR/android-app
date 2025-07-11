package dev.abzikel.sistemaetr;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.SystemClock;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Date;
import java.util.Locale;

import dev.abzikel.sistemaetr.services.BLEServerService;
import dev.abzikel.sistemaetr.utils.SharedPreferencesManager;

public class GameActivity extends AppCompatActivity {
    private BLEServerService mService;
    private Handler handler;
    private TextView tvHits, tvMisses, tvScore, tvPrecision, tvReactionTime, tvTimer;
    private ImageButton btnStartStop;
    private String action, gameMode;
    private long startTime, timeInMilliseconds, timeSwapBuff, lastUpdateTimeInterval, timeInterval = 0;
    private byte maxShots = 6;
    private boolean classic, restartTime, mBound = false;

    private final ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            BLEServerService.LocalBinder binder = (BLEServerService.LocalBinder) service;
            mService = binder.getService();
            mBound = true;

            // Change value of new mode
            mService.setMode(getResources().getString(R.string.restart));
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);
        setup();
    }

    private void setup() {
        // Link XML to Java
        tvHits = findViewById(R.id.tvHits);
        tvMisses = findViewById(R.id.tvMisses);
        tvScore = findViewById(R.id.tvScore);
        tvPrecision = findViewById(R.id.tvPrecision);
        tvReactionTime = findViewById(R.id.tvReactionTime);
        tvTimer = findViewById(R.id.tvTimer);
        btnStartStop = findViewById(R.id.btnStartStop);

        // Get game mode, max shots and time interval
        String mode = getIntent().getStringExtra("mode");
        if (mode == null || mode.isEmpty()) {
            // Finish if mode is null
            finish();
            return;
        }

        // Define max number of shots until the game mode stop automatically
        if (mode.equals("Infinite")) maxShots = -1;
        else if (mode.endsWith("12")) maxShots = 12;

        // Define time interval before a target changes automatically
        if (mode.startsWith("Reaction")) timeInterval = 1000L;
        else if (mode.startsWith("Advanced")) timeInterval = 700L;

        // Get game mode
        switch (mode) {
            case "Classic6":
                gameMode = getResources().getString(R.string.short_weapon);
                break;
            case "Classic12":
                gameMode = getResources().getString(R.string.long_weapon);
                break;
            case "Reaction6":
                gameMode = getResources().getString(R.string.reaction);
                break;
            case "Advanced6":
                gameMode = getResources().getString(R.string.advanced_reaction);
                break;
            default:
                gameMode = getResources().getString(R.string.infinite);
                break;
        }

        // Initialize variables
        action = getString(R.string.start);
        handler = new Handler();
        classic = mode.startsWith("Classic");
        restartTime = false;
        timeSwapBuff = 0L;
        lastUpdateTimeInterval = 0L;

        // Add listeners
        btnStartStop.setOnClickListener(v -> {
            if (action.equals(getResources().getString(R.string.start))) {
                // Start the game
                startGame();
                btnStartStop.setImageResource(R.drawable.ic_pause);
            } else if (action.equals(getResources().getString(R.string.stop))) {
                // Stop the game
                stopGame();
                btnStartStop.setImageResource(R.drawable.ic_restart);
            } else if (action.equals(getResources().getString(R.string.restart))) {
                // Change mode
                mService.setMode(action);
                action = getResources().getString(R.string.start);

                // Restart timer and button image resource
                tvTimer.setText(getString(R.string.zero_time));
                btnStartStop.setImageResource(R.drawable.ic_play);
            }
        });
    }

    private final BroadcastReceiver gameStatsReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Get hits and failures
            int hits = intent.getIntExtra("hits", 0);
            int failures = intent.getIntExtra("failures", 0);

            // Update user interface
            tvHits.setText(String.valueOf(hits));
            tvMisses.setText(String.valueOf(failures));

            // Stop game
            if (hits + failures == maxShots) stopGame();

            restartTime = true;
        }
    };

    @Override
    protected void onStart() {
        super.onStart();

        Intent intent = new Intent(this, BLEServerService.class);
        bindService(intent, connection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Start broadcast receiver
        registerReceiver(gameStatsReceiver, new IntentFilter("GAME_STATS_UPDATED"));
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(gameStatsReceiver);
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (mBound) {
            // Stop the game
            mService.setMode(getString(R.string.stop));
            handler.removeCallbacks(updateTimerThread);

            // Unbind the service
            unbindService(connection);
            mBound = false;
        }
    }

    private void startGame() {
        // Change mode
        String newMode = getString(R.string.stop);
        changeMode(newMode);

        // Start timer
        startTime = SystemClock.uptimeMillis();
        timeSwapBuff = 0;
        handler.postDelayed(updateTimerThread, 0);
    }

    private void stopGame() {
        // Change mode
        String newMode = getResources().getString(R.string.restart);
        changeMode(newMode);

        // Stop timer
        timeSwapBuff += timeInMilliseconds;
        handler.removeCallbacks(updateTimerThread);

        // Get hits and failures
        int hits = Integer.parseInt(tvHits.getText().toString());
        int failures = Integer.parseInt(tvMisses.getText().toString());
    }

    private void changeMode(String newMode) {
        // Change mode
        mService.setMode(action);
        action = newMode;
    }

    private final Runnable updateTimerThread = new Runnable() {
        public void run() {
            if (!mBound) return;

            timeInMilliseconds = SystemClock.uptimeMillis() - startTime;
            long updatedTime = timeSwapBuff + timeInMilliseconds;

            int seconds = (int) (updatedTime / 1000);
            int minutes = seconds / 60;
            seconds = seconds % 60;
            int milliseconds = (int) (updatedTime % 1000);

            // Verify if 1 minute has passed
            if (minutes == 1 && seconds == 0 && classic) {
                // Stop game only in classic mode
                stopGame();

                // Change text time
                tvTimer.setText(getResources().getString(R.string.minute_time));

                return;
            }

            // Restart time
            if (timeInterval != 0 && restartTime) {
                // Restart interval time for reaction modes
                lastUpdateTimeInterval = updatedTime;
                restartTime = false;
            }

            // Verify time interval
            if (timeInterval != 0 && (updatedTime - lastUpdateTimeInterval) >= timeInterval) {
                // Inform server
                mService.noImpact();
                restartTime = true;
            }

            // Update user interface
            String textTime = "" + minutes + ":" + String.format(Locale.getDefault(), "%02d",
                    seconds) + "." + String.format(Locale.getDefault(), "%03d", milliseconds);
            tvTimer.setText(textTime);
            handler.postDelayed(this, 0);
        }
    };

}
