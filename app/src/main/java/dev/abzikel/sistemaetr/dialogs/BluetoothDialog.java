package dev.abzikel.sistemaetr.dialogs;

import android.Manifest;
import android.app.Activity;
import android.app.ActivityManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import dev.abzikel.sistemaetr.R;
import dev.abzikel.sistemaetr.services.BLEServerService;
import dev.abzikel.sistemaetr.utils.OnSingleClickListener;

public class BluetoothDialog extends DialogFragment {
    private static final String ARG_REQUEST_KEY = "request_key";
    private BluetoothAdapter bluetoothAdapter;
    private ImageView ivStatus;
    private TextView tvTitle, tvDialogMessage, tvPermissionStatus, tvBluetoothStatus;
    private Button btnContinue;
    private String requestKey;
    private boolean first = true;

    public static BluetoothDialog newInstance(String requestKey) {
        // Create a new instance of the dialog with the request key
        BluetoothDialog fragment = new BluetoothDialog();
        Bundle args = new Bundle();
        args.putString(ARG_REQUEST_KEY, requestKey);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Get the request key from the arguments
        if (getArguments() != null) requestKey = getArguments().getString(ARG_REQUEST_KEY);
        if (requestKey == null) requestKey = "bluetooth_request";
    }

    // Launcher for the Bluetooth activation intent result
    private final ActivityResultLauncher<Intent> enableBluetoothLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    // The user enabled Bluetooth, proceed to the next check
                    updateStatusUI();
                    checkAndProceed();
                } else {
                    // The user didn't enable Bluetooth, show an error message and dismiss the dialog
                    Toast.makeText(getContext(), getString(R.string.error_bluetooth_required), Toast.LENGTH_SHORT).show();
                    dismiss();
                }
            });

    // Launcher for multiple permission requests
    private final ActivityResultLauncher<String[]> requestPermissionsLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), permissions -> {
                // Check if all permissions are granted
                boolean allGranted = true;
                for (Map.Entry<String, Boolean> entry : permissions.entrySet()) {
                    if (!entry.getValue()) {
                        allGranted = false;
                        break;
                    }
                }

                // Update the status of permissions
                updateStatusUI();

                // If all permissions are granted, proceed to the next check
                if (allGranted) checkAndProceed();
                else {
                    // Verify if the user has denied the permission permanently
                    if (!shouldShowRequestPermissionRationale(Manifest.permission.BLUETOOTH_CONNECT)) {
                        // Go to settings to enable permissions
                        goToSettings();
                    } else {
                        // User denied the permission, show a message and dismiss the dialog
                        Toast.makeText(getContext(), getString(R.string.error_permission_required), Toast.LENGTH_SHORT).show();
                        dismiss();
                    }
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_bluetooth, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Link UI elements
        ivStatus = view.findViewById(R.id.ivBluetoothStatus);
        tvTitle = view.findViewById(R.id.tvTitle);
        tvDialogMessage = view.findViewById(R.id.tvDialogMessage);
        tvPermissionStatus = view.findViewById(R.id.tvPermissionStatus);
        tvBluetoothStatus = view.findViewById(R.id.tvBluetoothStatus);
        btnContinue = view.findViewById(R.id.btnContinue);

        // Get the system's Bluetooth adapter
        BluetoothManager bluetoothManager = (BluetoothManager) requireActivity().getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();

        // Check hardware support first
        if (bluetoothAdapter == null) setupUnsupportedState();
        else {
            // Update UI when the dialog is opened
            btnContinue.setOnClickListener(new OnSingleClickListener() {
                @Override
                public void onSingleClick(View v) {
                    checkAndProceed();
                }
            });
            updateStatusUI();
        }

        // First verification
        checkAndProceed();
        first = false;
    }

    private void checkAndProceed() {
        // Check permissions first
        if (!hasRequiredPermissions()) return;

        // If permissions are granted, check if Bluetooth is enabled
        if (!bluetoothAdapter.isEnabled()) {
            if (!first) {
                // Only request to enable bluetooth if it is not the first verification
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                enableBluetoothLauncher.launch(enableBtIntent);
            }
            return;
        }

        // Start BLE server service
        Context context = requireContext();
        if (!isServiceRunning(context)) {
            Intent serviceIntent = new Intent(context, BLEServerService.class);
            context.startService(serviceIntent);
        }

        // If all conditions are met, notify the FragmentManager
        Bundle result = new Bundle();
        result.putBoolean("isBluetoothReady", true);
        getParentFragmentManager().setFragmentResult(requestKey, result);
        dismiss();
    }

    private boolean hasRequiredPermissions() {
        // Check for required permissions if the Android version is 12 or higher
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Check for the required permissions to use Bluetooth Low Energy Server
            String[] requiredPermissions = {Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_ADVERTISE};
            List<String> permissionsToRequest = new ArrayList<>();

            // Check if each permission is granted
            for (String permission : requiredPermissions) {
                if (ContextCompat.checkSelfPermission(requireContext(), permission) != PackageManager.PERMISSION_GRANTED) {
                    permissionsToRequest.add(permission);
                }
            }

            if (!permissionsToRequest.isEmpty()) {
                // Ask for the permission, launch the request
                if (!first)
                    requestPermissionsLauncher.launch(permissionsToRequest.toArray(new String[0]));
                return false;
            }
        }

        // All permissions are granted, or the Android version is lower than 12
        return true;
    }

    private void updateStatusUI() {
        // Update the status of Bluetooth
        if (bluetoothAdapter != null && bluetoothAdapter.isEnabled())
            tvBluetoothStatus.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_check, 0, 0, 0);
        else
            tvBluetoothStatus.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_cross, 0, 0, 0);

        // Verify permissions
        boolean permissionsGranted = true;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_ADVERTISE) != PackageManager.PERMISSION_GRANTED) {
                permissionsGranted = false;
            }
        }

        // Update the status of permissions
        if (permissionsGranted)
            tvPermissionStatus.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_check, 0, 0, 0);
        else
            tvPermissionStatus.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_cross, 0, 0, 0);
    }

    private void setupUnsupportedState() {
        // Change views
        ivStatus.setImageResource(R.drawable.ic_bluetooth_not_supported);
        tvTitle.setText(R.string.bluetooth_not_supported);
        tvDialogMessage.setText(R.string.bluetooth_not_supported_instructions);
        btnContinue.setText(R.string.cancel);

        // Change button color to red to indicate an error/stop action.
        btnContinue.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.red));
        btnContinue.setOnClickListener(new OnSingleClickListener() {
            @Override
            public void onSingleClick(View v) {
                dismiss();
            }
        });
    }

    private void goToSettings() {
        // Create intent to open the app's settings
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", requireActivity().getPackageName(), null);
        intent.setData(uri);
        startActivity(intent);
        dismiss();
    }

    private boolean isServiceRunning(Context context) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        if (manager == null) {
            return false;
        }

        // Iterate over running services (limited to this app on modern Android, which is fine)
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (BLEServerService.class.getName().equals(service.service.getClassName())) {
                // Check if the service is actually started
                return service.started;
            }
        }
        return false;
    }

}
