package dev.abzikel.sistemaetr.utils;

import android.text.Editable;
import android.text.TextWatcher;

import com.google.android.material.textfield.TextInputLayout;

public class ErrorClearingTextWatcher implements TextWatcher {
    private final TextInputLayout textInputLayout;

    public ErrorClearingTextWatcher(TextInputLayout textInputLayout) {
        this.textInputLayout = textInputLayout;
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        // No action needed here
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        // Clear the error as soon as the user starts typing
        textInputLayout.setError(null);
    }

    @Override
    public void afterTextChanged(Editable s) {
        // No action needed here
    }
}
