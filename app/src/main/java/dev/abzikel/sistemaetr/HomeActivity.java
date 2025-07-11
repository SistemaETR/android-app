package dev.abzikel.sistemaetr;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

public class HomeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        setup();
    }

    private void setup() {
        // Link XML to Java
        LinearLayout btnShortWeapon = findViewById(R.id.btnShortWeapon);
        LinearLayout btnLongWeapon = findViewById(R.id.btnLongWeapon);
        LinearLayout btnReaction = findViewById(R.id.btnReaction);
        LinearLayout btnAdvancedReaction = findViewById(R.id.btnAdvancedReaction);
        LinearLayout btnInfinite = findViewById(R.id.btnInfinite);
        Button btnSignOut = findViewById(R.id.btnSignOut);

        // Listeners
        btnSignOut.setOnClickListener(v -> signOut());
        btnShortWeapon.setOnClickListener(v -> changeActivity("Classic6"));
        btnLongWeapon.setOnClickListener(v -> changeActivity("Classic12"));
        btnReaction.setOnClickListener(v -> changeActivity("Reaction6"));
        btnAdvancedReaction.setOnClickListener(v -> changeActivity("Advanced6"));
        btnInfinite.setOnClickListener(v -> changeActivity("Infinite"));
    }

    private void changeActivity(String gameMode) {
        // Go to control activity
        Intent intent = new Intent(this, GameActivity.class);
        intent.putExtra("mode", gameMode);
        startActivity(intent);
    }

    private void signOut() {
        // Inflate custom content dialog
        Context context = this;
        LayoutInflater inflater = LayoutInflater.from(context);
        View dialogView = inflater.inflate(R.layout.content_dialog_confirmation, null);

        // Link XML to Java
        TextView tvMessage = dialogView.findViewById(R.id.tvMessage);
        Button btnPositive = dialogView.findViewById(R.id.btnPositive);
        Button btnNegative = dialogView.findViewById(R.id.btnNegative);

        // Initialize views
        tvMessage.setText(getResources().getString(R.string.sign_out_confirmation));
        btnPositive.setText(getResources().getString(R.string.sign_out));

        // Create Alert Dialog to ask confirmation about logout
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.CustomAlertDialogStyle);
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();

        // Add listeners
        btnPositive.setOnClickListener(v -> {
            // Connect to Firebase Authentication and get user
            FirebaseAuth auth = FirebaseAuth.getInstance();

            // Sign out
            auth.signOut();

            // Return to login activity
            Toast.makeText(context, context.getResources().getString(R.string.session_closed), Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(context, SignInActivity.class);
            startActivity(intent);
            finish();

            // Dismiss dialog
            if (dialog.isShowing()) dialog.dismiss();
        });
        btnNegative.setOnClickListener(v -> dialog.dismiss());

        // Show Alert Dialog
        dialog.show();
    }

}
