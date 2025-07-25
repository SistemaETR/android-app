package dev.abzikel.sistemaetr.services;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.ParcelUuid;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import dev.abzikel.sistemaetr.R;
import dev.abzikel.sistemaetr.utils.SharedPreferencesManager;

public class BLEServerService extends Service {
    private static final int NOTIFICATION_ID = 1;
    private static final String TAG = BLEServerService.class.getSimpleName();
    private static final String CHANNEL_ID = "BLEServerServiceChannel";
    private final IBinder mBinder = new LocalBinder();
    private SharedPreferencesManager sharedPreferencesManager;
    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothGattServer mGattServer;
    private UUID SERVICE_UUID;
    private static final String[] NOTIFY_STRINGS = {
            "3729d2dd-5f45-4800-9f67-1548f7b6c326",
            "9baaf20c-001c-4832-9de7-0dd1e75dc2e2",
            "58a0ed54-aad4-4583-bbea-49911cc32f2f",
            "e0faa180-bcde-4f6b-8a1e-d2b2d8e0cf4e",
            "ae0e8aa1-fdf8-4c8d-b5cd-91ec7aaebcc4",
            "c187977a-ba30-40cf-86be-e1d59ad9a6b7"
    };
    private static final String[] WRITE_STRINGS = {
            "f0e0ada8-bedb-426f-a6cd-f5e00e4285bd",
            "e90df139-0301-446e-a6d1-edc72cee2db3",
            "b8c13652-664d-44dd-91ec-4812fb67a9bb",
            "8615a4ba-1032-431c-899f-e5aa6ffd644e",
            "7bee5d28-4df8-4e74-a19d-71aeb3b76f7f",
            "1de54dfe-1aca-4756-937d-4b88838a4e53",
            "4af6732d-b146-423c-964e-ad621d828cdc"
    };
    private String[] MAC_ADDRESSES = new String[6];

    // Important variables
    private String mode;
    private final byte[] batteries = {0, 0, 0, 0, 0, 0};
    private int hits = 0;
    private int failures = 0;
    private byte randomNumber = 5;
    private byte connectedDevices = 0;

    @Override
    public void onCreate() {
        super.onCreate();

        // Initialize variables
        sharedPreferencesManager = SharedPreferencesManager.getInstance(this);
        SERVICE_UUID = UUID.fromString(sharedPreferencesManager.getServiceUUID());
        MAC_ADDRESSES = sharedPreferencesManager.getMacAddresses();
        mode = getResources().getString(R.string.restart);

        mBluetoothManager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        mBluetoothAdapter = mBluetoothManager.getAdapter();

        if (mBluetoothAdapter == null) {
            Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            Log.e(TAG, "Bluetooth is not enabled");

            return START_NOT_STICKY;
        }

        startInForeground();
        startAdvertising();
        startServer();

        return START_STICKY;
    }

    private void startInForeground() {
        // Create notification channel
        NotificationChannel serviceChannel = new NotificationChannel(
                CHANNEL_ID,
                getString(R.string.app_name),
                NotificationManager.IMPORTANCE_LOW
        );

        // Create notification manager
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.createNotificationChannel(serviceChannel);
        }

        // Create notification
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(getString(R.string.target_system_active))
                .setContentText(getString(R.string.ble_server_working))
                .setSmallIcon(R.drawable.logo)
                .build();

        // Start foreground service
        startForeground(NOTIFICATION_ID, notification);
    }

    public void noImpact() {
        // Generate new random number
        generateRandomNumber();

        // Increase failure
        failures++;
        updateGameStats();
    }

    public void turnOnAllLed(boolean blue) {
        for (byte counter = 0; counter < NOTIFY_STRINGS.length; counter++) {
            // Send notifications to all sensors
            String sensorSensibility = sharedPreferencesManager.getSensorSensibility();

            if (blue) {
                // Get blue LED intensity
                String blueIntensity = sharedPreferencesManager.getLedBlueIntensity();
                notifyCharacteristicChangedValue(counter, "000" + blueIntensity + sensorSensibility);
            } else {
                // Get red LED intensity
                String redIntensity = sharedPreferencesManager.getLedRedIntensity();
                notifyCharacteristicChangedValue(counter, redIntensity + "000" + sensorSensibility);
            }
        }
    }

    public void turnOnAllLedTemp(boolean blue, String intensity) {
        String sensibility = sharedPreferencesManager.getSensorSensibility();

        for (byte counter = 0; counter < NOTIFY_STRINGS.length; counter++) {
            // Send notifications to all sensors
            if (blue) notifyCharacteristicChangedValue(counter, "000" + intensity + sensibility);
            else notifyCharacteristicChangedValue(counter, intensity + "000" + sensibility);
        }
    }

    public void setMode(String newMode) {
        String sensorSensibility = sharedPreferencesManager.getSensorSensibility();
        mode = newMode;

        if (newMode.equals(getResources().getString(R.string.stop))) {
            // Turn OFF all LEDs
            for (byte counter = 0; counter < NOTIFY_STRINGS.length; counter++) {
                // Send notifications to all sensors
                notifyCharacteristicChangedValue(counter, "000000" + sensorSensibility);
            }
        } else if (newMode.equals(getResources().getString(R.string.restart))) {
            // Clear variables
            hits = 0;
            failures = 0;
            updateGameStats();
        } else {
            // Start game
            generateRandomNumber();

            // Ged red LED intensity
            String ledRedIntensity = sharedPreferencesManager.getLedRedIntensity();

            // Turn ON all LEDs to red except current number
            for (byte counter = 0; counter < NOTIFY_STRINGS.length; counter++) {
                // Send notifications to all sensors except current number
                if (counter != randomNumber)
                    notifyCharacteristicChangedValue(counter, ledRedIntensity + "000" + sensorSensibility);
            }
        }
    }

    private void updateGameStats() {
        Intent intent = new Intent("GAME_STATS_UPDATED");
        intent.putExtra("hits", hits);
        intent.putExtra("failures", failures);
        sendBroadcast(intent);
    }

    private final BluetoothGattServerCallback mGattServerCallback = new BluetoothGattServerCallback() {
        @Override
        public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
            super.onConnectionStateChange(device, status, newState);

            if (newState == BluetoothProfile.STATE_CONNECTED) {
                // A device has been connected
                if (isAllowed(device.getAddress())) {
                    // Increase number of connected devices
                    connectedDevices++;
                    if (connectedDevices == 6) stopAdvertising();
                } else {
                    // Disconnect device
                    if (checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED || Build.VERSION.SDK_INT < Build.VERSION_CODES.S)
                        mGattServer.cancelConnection(device);
                }
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                // A device has been disconnected
                if (isAllowed(device.getAddress())) {
                    if (connectedDevices == 6) startAdvertising();
                    connectedDevices--;
                }
            }
        }

        @Override
        public void onCharacteristicReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicReadRequest(device, requestId, offset, characteristic);

            // Default value
            if (characteristic.getValue() == null) characteristic.setValue("000000" +
                    sharedPreferencesManager.getSensorSensibility());

            // Send response
            if (checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED || Build.VERSION.SDK_INT < Build.VERSION_CODES.S)
                mGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, characteristic.getValue());
        }

        @Override
        public void onCharacteristicWriteRequest(BluetoothDevice device, int requestId, BluetoothGattCharacteristic characteristic, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
            super.onCharacteristicWriteRequest(device, requestId, characteristic, preparedWrite, responseNeeded, offset, value);

            if (characteristic.getUuid().equals(UUID.fromString(WRITE_STRINGS[6]))) {
                // Impact characteristic, get non-playable modes
                List<String> nonPlayableModes = new ArrayList<>();
                nonPlayableModes.add(getResources().getString(R.string.stop));
                nonPlayableModes.add(getResources().getString(R.string.restart));
                nonPlayableModes.add(getResources().getString(R.string.configuration));

                if (!nonPlayableModes.contains(mode)) {
                    // Verify if there was a hit or a failure
                    if (value[0] == randomNumber) hits++;
                    else failures++;

                    // Update game stats
                    updateGameStats();

                    // Generate new random number
                    generateRandomNumber();
                }
            } else {
                // Battery characteristic
                byte position = getCharacteristicPosition(characteristic.getUuid());
                if (position != -1) batteries[position] = value[0];
                if (checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED || Build.VERSION.SDK_INT < Build.VERSION_CODES.S)
                    mGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, null);
            }
        }
    };

    private boolean isAllowed(String deviceAddress) {
        // Verify device MAC address
        boolean allowed = false;
        for (String MAC_ADDRESS : MAC_ADDRESSES) {
            if (deviceAddress.equals(MAC_ADDRESS)) {
                // Change variable value
                allowed = true;

                break;
            }
        }

        return allowed;
    }

    private byte getCharacteristicPosition(UUID characteristicUUID) {
        for (byte counter = 0; counter < WRITE_STRINGS.length; counter++) {
            if (characteristicUUID.equals(UUID.fromString(WRITE_STRINGS[counter]))) return counter;
        }

        return -1;
    }

    private void generateRandomNumber() {
        // Generate a different random number
        Random rand = new Random();
        byte newNumber;

        do {
            newNumber = (byte) rand.nextInt(6);
        } while (newNumber == randomNumber);

        // Get sensor sensibility
        String sensorSensibility = sharedPreferencesManager.getSensorSensibility();

        // Get led red intensity and led blue intensity
        String ledRedIntensity = sharedPreferencesManager.getLedRedIntensity();
        String ledBlueIntensity = sharedPreferencesManager.getLedBlueIntensity();

        // Update LEDs
        notifyCharacteristicChangedValue(randomNumber, ledRedIntensity + "000" + sensorSensibility);
        randomNumber = newNumber;
        notifyCharacteristicChangedValue(randomNumber, "000" + ledBlueIntensity + sensorSensibility);
    }

    private void notifyCharacteristicChangedValue(byte noSensor, String value) {
        BluetoothGattService service = mGattServer.getService(SERVICE_UUID);
        if (service != null) {
            BluetoothGattCharacteristic characteristic = service.getCharacteristic(UUID.fromString(NOTIFY_STRINGS[noSensor]));
            if (characteristic != null) {
                // Change value of the characteristic
                characteristic.setValue(value);

                // Notify the to the device that the characteristic's value changed
                BluetoothDevice bluetoothDevice = getBluetoothDevice(MAC_ADDRESSES[noSensor]);
                if (checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED || Build.VERSION.SDK_INT < Build.VERSION_CODES.S)
                    if (bluetoothDevice != null)
                        mGattServer.notifyCharacteristicChanged(bluetoothDevice, characteristic, false);
            }
        }
    }

    private BluetoothDevice getBluetoothDevice(String macAddress) {
        // Get the bluetooth device that matches the MAC address
        if (checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED || Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            List<BluetoothDevice> deviceList = mBluetoothManager.getConnectedDevices(BluetoothProfile.GATT_SERVER);
            for (BluetoothDevice device : deviceList)
                if (device.getAddress().equals(macAddress)) return device;
        }

        return null;
    }

    @Override
    public void onDestroy() {
        stopServer();
        stopAdvertising();

        super.onDestroy();
    }

    private AdvertiseSettings createAdvertiseSettings() {
        return new AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
                .setConnectable(true)
                .build();
    }

    private AdvertiseData createAdvertiseData() {
        AdvertiseData.Builder dataBuilder = new AdvertiseData.Builder();
        dataBuilder.setIncludeDeviceName(false);
        dataBuilder.addServiceUuid(new ParcelUuid(SERVICE_UUID));
        return dataBuilder.build();
    }

    private void startAdvertising() {
        if (checkSelfPermission(Manifest.permission.BLUETOOTH_ADVERTISE) == PackageManager.PERMISSION_GRANTED || Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            BluetoothLeAdvertiser advertiser = mBluetoothAdapter.getBluetoothLeAdvertiser();
            if (advertiser != null) {
                AdvertiseSettings settings = createAdvertiseSettings();
                AdvertiseData data = createAdvertiseData();
                if (!mBluetoothAdapter.isMultipleAdvertisementSupported()) {
                    Log.e(TAG, "Multiple advertisement not supported");
                    return;
                }
                advertiser.startAdvertising(settings, data, mAdvertiseCallback);
            } else {
                Log.e(TAG, "Failed to create advertiser");
            }
        }
    }


    private void stopAdvertising() {
        BluetoothLeAdvertiser advertiser = mBluetoothAdapter.getBluetoothLeAdvertiser();
        if (advertiser != null) {
            if (checkSelfPermission(Manifest.permission.BLUETOOTH_ADVERTISE) == PackageManager.PERMISSION_GRANTED || Build.VERSION.SDK_INT < Build.VERSION_CODES.S)
                advertiser.stopAdvertising(mAdvertiseCallback);
        }
    }

    private final AdvertiseCallback mAdvertiseCallback = new AdvertiseCallback() {
        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
            super.onStartSuccess(settingsInEffect);
            Log.d(TAG, "Advertising successfully started");
        }

        @Override
        public void onStartFailure(int errorCode) {
            Log.e(TAG, "Advertising failed to start with error code: " + errorCode);
        }
    };

    private void startServer() {
        if (checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED || Build.VERSION.SDK_INT < Build.VERSION_CODES.S)
            mGattServer = mBluetoothManager.openGattServer(this, mGattServerCallback);

        if (mGattServer == null) {
            Log.e(TAG, "Unable to create GATT server");

            return;
        }

        // Initialize service and characteristics
        BluetoothGattService service = new BluetoothGattService(SERVICE_UUID, BluetoothGattService.SERVICE_TYPE_PRIMARY);

        // Add notify characteristics
        for (String NOTIFY_STRING : NOTIFY_STRINGS) {
            // Get UUID from String and add them to the service
            UUID NOTIFY_UUID = UUID.fromString(NOTIFY_STRING);

            BluetoothGattCharacteristic characteristic = new BluetoothGattCharacteristic(
                    NOTIFY_UUID,
                    BluetoothGattCharacteristic.PROPERTY_NOTIFY | BluetoothGattCharacteristic.PROPERTY_READ,
                    BluetoothGattCharacteristic.PERMISSION_READ
            );

            service.addCharacteristic(characteristic);
        }

        // Add write characteristics
        for (String WRITE_STRING : WRITE_STRINGS) {
            // Get UUID from String and add them to the service
            UUID WRITE_UUID = UUID.fromString(WRITE_STRING);

            BluetoothGattCharacteristic characteristic = new BluetoothGattCharacteristic(
                    WRITE_UUID,
                    BluetoothGattCharacteristic.PROPERTY_WRITE,
                    BluetoothGattCharacteristic.PERMISSION_WRITE
            );

            service.addCharacteristic(characteristic);
        }

        if (checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED || Build.VERSION.SDK_INT < Build.VERSION_CODES.S)
            mGattServer.addService(service);
    }

    private void stopServer() {
        if (mGattServer != null) {
            if (checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED || Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                List<BluetoothDevice> connectedDevices = mBluetoothManager.getConnectedDevices(BluetoothProfile.GATT_SERVER);
                for (BluetoothDevice device : connectedDevices)
                    mGattServer.cancelConnection(device);
                mGattServer.close();
                mGattServer = null;
            }
        }
    }

    public class LocalBinder extends Binder {
        public BLEServerService getService() {
            return BLEServerService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public byte[] getBatteryStatus() {
        return batteries;
    }

}
