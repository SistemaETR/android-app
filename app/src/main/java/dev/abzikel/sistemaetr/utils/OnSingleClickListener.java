package dev.abzikel.sistemaetr.utils;

import android.os.SystemClock;
import android.view.View;

public abstract class OnSingleClickListener implements View.OnClickListener {
    // The minimum interval between two clicks, in milliseconds. 1000ms = 1 second
    private static final long MIN_CLICK_INTERVAL = 1000;

    // Stores the time of the last valid click
    private long lastClickTime = 0;

    @Override
    public final void onClick(View v) {
        // Get the current time
        long currentClickTime = SystemClock.elapsedRealtime();

        // If the time since the last click is less than the minimum interval, ignore this click
        if ((currentClickTime - lastClickTime) < MIN_CLICK_INTERVAL) return;

        // If enough time has passed, update the last click time
        lastClickTime = currentClickTime;

        // Execute the actual click logic defined in the abstract method
        onSingleClick(v);
    }

    public abstract void onSingleClick(View v);
}