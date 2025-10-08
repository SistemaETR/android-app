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
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import java.util.Date;
import java.util.Locale;
import java.util.UUID;

import dev.abzikel.sistemaetr.pojos.Training;
import dev.abzikel.sistemaetr.services.BLEServerService;
import dev.abzikel.sistemaetr.utils.BaseActivity;
import dev.abzikel.sistemaetr.utils.FirebaseManager;
import dev.abzikel.sistemaetr.utils.OnSingleClickListener;

public class GameActivity extends BaseActivity {
    private BLEServerService mService;
    private Handler handler;
    private TextView tvHits, tvMisses, tvScore, tvPrecision, tvReactionTime, tvTimer;
    private ImageButton btnStartStop;
    private String action, gameMode;
    private int gameModeId;
    private long startTime, timeInMilliseconds, timeSwapBuff, lastUpdateTimeInterval, timeInterval = 0;
    private byte maxShots = 6;
    private boolean classic, restartTime, mBound = false;

    private final ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            // Get the local binder and set the service
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
                gameModeId = 1;
                break;
            case "Classic12":
                gameMode = getResources().getString(R.string.long_weapon);
                gameModeId = 2;
                break;
            case "Reaction6":
                gameMode = getResources().getString(R.string.reaction);
                gameModeId = 3;
                break;
            case "Advanced6":
                gameMode = getResources().getString(R.string.advanced_reaction);
                gameModeId = 4;
                break;
            default:
                gameMode = getResources().getString(R.string.infinite);
                gameModeId = 5;
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
        btnStartStop.setOnClickListener(new OnSingleClickListener() {
            @Override
            public void onSingleClick(View v) {
                if (action.equals(getResources().getString(R.string.start))) {
                    // Start the game
                    startGame();
                    btnStartStop.setImageResource(R.drawable.ic_pause);
                } else if (action.equals(getResources().getString(R.string.stop))) {
                    // Stop the game
                    stopGame();
                } else if (action.equals(getResources().getString(R.string.restart))) {
                    // Change mode
                    mService.setMode(action);
                    action = getResources().getString(R.string.start);

                    // Restart timer and button image resource
                    tvTimer.setText(getString(R.string.zero_time));
                    btnStartStop.setImageResource(R.drawable.ic_play);

                    // Restart text views
                    tvPrecision.setText(getString(R.string.na));
                    tvReactionTime.setText(getString(R.string.na));
                    tvScore.setText(getString(R.string.na));
                }
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
        // Change mode and button image resource
        String newMode = getResources().getString(R.string.restart);
        changeMode(newMode);
        btnStartStop.setImageResource(R.drawable.ic_restart);

        // Stop timer
        handler.removeCallbacks(updateTimerThread);

        // Get hits and failures
        int hits = Integer.parseInt(tvHits.getText().toString());
        int misses = Integer.parseInt(tvMisses.getText().toString());

        // Calculate precision
        double totalShots = hits + misses;
        double precision = 0.0;
        if (totalShots > 0) precision = (hits / totalShots) * 100.0;

        // Calculate reaction time
        long totalMilliseconds = timeSwapBuff + timeInMilliseconds;
        double reactionTime = 0.0;
        if (hits > 0) {
            double totalSeconds = totalMilliseconds / 1000.0;
            reactionTime = totalSeconds / hits;
        }

        // Calculate score based on game mode
        int score = 0;
        if (gameMode.equals(getString(R.string.short_weapon)) || gameMode.equals(getString(R.string.long_weapon))) {
            // Classic modes, bonus for finishing faster than the 60-second limit
            int timeBonus = (int) Math.max(0, (60.0 - (totalMilliseconds / 1000.0)) * 5);
            score = (hits * 100) - (misses * 50) + timeBonus;
        } else if (gameMode.equals(getString(R.string.reaction)) || gameMode.equals(getString(R.string.advanced_reaction))) {
            // Reaction modes, bonus/penalty based on average reaction time, centered around 1.5s
            double reactionBonus = (1.5 - reactionTime) * 100 * hits;
            score = (hits * 150) - (misses * 75) + (int) reactionBonus;
        } else if (gameMode.equals(getString(R.string.infinite))) {
            // Infinite mode, simple cumulative score
            score = (hits * 10) - (misses * 5);
        }

        // Ensure the score is not negative
        if (score < 0) score = 0;

        // Update user interface
        String precisionText = String.format(Locale.getDefault(), "%.2f%%", precision);
        String reactionTimeText = String.format(Locale.getDefault(), "%.3f s", reactionTime);
        String scoreText = String.format(Locale.getDefault(), "%d pts.", score);
        tvPrecision.setText(precisionText);
        tvReactionTime.setText(reactionTimeText);
        tvScore.setText(scoreText);

        // Verify that it was an acceptable training
        if (hits == 0 && misses == 0) return;

        // Create training object
        Training training = new Training();
        training.setTrainingId(UUID.randomUUID().toString());
        training.setModality(gameModeId);
        training.setHits(hits);
        training.setMisses(misses);
        training.setScore(score);
        training.setTrainingTime(totalMilliseconds);
        training.setAverageShotTime(reactionTime);
        training.setCreatedAt(new Date());

        // Save training to Firestore
        FirebaseManager.getInstance().saveTraining(this, training, new FirebaseManager.OnSaveTrainingListener() {
            @Override
            public void onSuccess() {
                // Do nothing
            }

            @Override
            public void onFailure(Exception e) {
                // Handle error
                Log.e("FirebaseError", getString(R.string.could_not_save_training), e);
            }
        });
    }

    private void changeMode(String newMode) {
        // Change mode
        mService.setMode(action);
        action = newMode;
    }

    private final Runnable updateTimerThread = new Runnable() {
        public void run() {
            if (!mBound) return;

            // Get time in milliseconds
            timeInMilliseconds = SystemClock.uptimeMillis() - startTime;
            long updatedTime = timeSwapBuff + timeInMilliseconds;

            // Convert time to minutes, seconds and milliseconds
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
            String textTime = minutes + ":" + String.format(Locale.getDefault(), "%02d",
                    seconds) + "." + String.format(Locale.getDefault(), "%03d", milliseconds);
            tvTimer.setText(textTime);
            handler.postDelayed(this, 0);
        }
    };

    @Override
    protected void onStart() {
        super.onStart();

        // Bind the service
        Intent intent = new Intent(this, BLEServerService.class);
        bindService(intent, connection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Start broadcast receiver
        IntentFilter filter = new IntentFilter("GAME_STATS_UPDATED");
        ContextCompat.registerReceiver(this, gameStatsReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED);
    }

    @Override
    protected void onPause() {
        super.onPause();

        // Stop broadcast receiver
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

}
