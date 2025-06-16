package dev.abzikel.sistemaetr;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.text.LineBreaker;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

public class BluetoothActivity extends AppCompatActivity {
    private static final int REQUEST_BLUETOOTH_PERMISSION = 1;
    private static final int BT_NOT_SUPPORTED = 0;
    private static final int BT_DISABLED = 1;
    private static final int BT_ENABLED = 2;
    private ActivityResultLauncher<Intent> enableBTLauncher;
    private Intent enableBTIntent;
    private ImageView ivBluetoothStatus;
    private TextView tvBluetoothStatus;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth);
        setup();
    }

    private void setup() {
        // Link XML to Java
        ivBluetoothStatus = findViewById(R.id.ivBluetoothStatus);
        tvBluetoothStatus = findViewById(R.id.tvBluetoothStatus);
        Button btnEnableBluetooth = findViewById(R.id.btnEnableBluetooth);
        progressBar = findViewById(R.id.progressBar);

        // Enable Bluetooth Intent
        enableBTIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);

        // Initialize Activity Result Launcher
        enableBTLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == RESULT_OK) {
                // Bluetooth was enabled correctly
                bluetoothEnabled();
                btnEnableBluetooth.setVisibility(View.GONE);
            }
        });

        // Get bluetooth status
        switch (getBluetoothStatus()) {
            case 1:
                // Bluetooth is disabled
                ivBluetoothStatus.setBackgroundResource(R.drawable.ic_bluetooth_disabled);
                tvBluetoothStatus.setText(getResources().getString(R.string.bluetooth_disabled));
                btnEnableBluetooth.setVisibility(View.VISIBLE);
                break;
            case 2:
                // Bluetooth is enabled
                bluetoothEnabled();

                break;
            default:
                // Bluetooth is not supported
                ivBluetoothStatus.setBackgroundResource(R.drawable.ic_bluetooth_not_supported);
                tvBluetoothStatus.setText(getResources().getString(R.string.bluetooth_not_supported));
                break;
        }

        // Add listeners
        btnEnableBluetooth.setOnClickListener(v -> enableBluetooth());
    }

    private void bluetoothEnabled() {
        // Bluetooth is enabled
        ivBluetoothStatus.setBackgroundResource(R.drawable.ic_bluetooth_enabled);
        tvBluetoothStatus.setText(getResources().getString(R.string.bluetooth_enabled));
        progressBar.setVisibility(View.VISIBLE);

        // Change activity after 2 seconds
        new CountDownTimer(2000, 20) {
            public void onTick(long millisUntilFinished) {
                int progress = 100 - (int) (millisUntilFinished / 20);
                progressBar.setProgress(progress);
            }

            public void onFinish() {
                startActivity(new Intent(BluetoothActivity.this, HomeActivity.class));
                finish();
            }
        }.start();
    }

    private int getBluetoothStatus() {
        // Get Bluetooth Manager and Bluetooth Adapter
        BluetoothManager bluetoothManager = getSystemService(BluetoothManager.class);
        BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();

        // Device does not support Bluetooth
        if (bluetoothAdapter == null) return BT_NOT_SUPPORTED;

        // Bluetooth is disabled
        if (!bluetoothAdapter.isEnabled() || (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED)) {
            // Try to enable Bluetooth if permission is granted
            enableBluetooth();

            return BT_DISABLED;
        }

        return BT_ENABLED;
    }

    private void enableBluetooth() {
        // Try to enable Bluetooth if permission is granted
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED)
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH_CONNECT}, REQUEST_BLUETOOTH_PERMISSION);
        else enableBTLauncher.launch(enableBTIntent);
    }

    protected void showPermissionDialog() {
        // Create Alert Dialog to inform user about the Bluetooth permission
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getResources().getString(R.string.permission_required));

        // Create a Textview and set the text on justification mode
        String bodyText = getResources().getString(R.string.bluetooth_permission);
        TextView textView = new TextView(this);
        textView.setText(bodyText);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) textView.setJustificationMode(LineBreaker.JUSTIFICATION_MODE_INTER_WORD);
        textView.setPadding(30, 15, 30, 0);

        builder.setView(textView);

        // Positive button listener
        builder.setPositiveButton(getResources().getString(R.string.enable), (dialog, which) -> {
            // Go to the app setting to allow user to enable permission manually
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            Uri uri = Uri.fromParts("package", getPackageName(), null);
            intent.setData(uri);
            startActivity(intent);
        });

        // Negative button listener
        builder.setNegativeButton(getResources().getString(R.string.cancel), (dialog, which) -> dialog.dismiss());

        builder.create().show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_BLUETOOTH_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Bluetooth permission granted, enable Bluetooth
                enableBTLauncher.launch(enableBTIntent);
            } else {
                // Bluetooth permission was not granted, inform user about permission functionality
                showPermissionDialog();
            }
        }
    }

}
