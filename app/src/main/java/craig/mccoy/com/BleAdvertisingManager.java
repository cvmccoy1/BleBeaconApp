package craig.mccoy.com;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.util.Log;

import androidx.annotation.NonNull;

import java.nio.ByteBuffer;
import java.util.UUID;

public class BleAdvertisingManager {
    private static final String TAG = "BleAdvertisingManager";

    private static volatile BleAdvertisingManager instance;
    private static BluetoothAdapter bluetoothAdapter;

    private BleAdvertisingManager() {}

    public static BleAdvertisingManager getInstance() {
        if (instance == null){
            synchronized (BleAdvertisingManager.class) {
                if(instance == null){
                    instance = new BleAdvertisingManager();
                    BluetoothManager bluetoothManager = App.bluetoothManager;
                    if (bluetoothManager == null) {
                        Log.e(TAG, "getInstance(): Unable to access the Bluetooth Manager");
                    } else {
                        bluetoothAdapter = bluetoothManager.getAdapter();
                        if (bluetoothAdapter == null) {
                            Log.e(TAG, "getInstance(): Unable to access the Bluetooth Adapter");
                        }
                    }
                }
            }
        }
        return instance;
    }

    public boolean isBluetoothEnabled() {
        boolean isEnabled = bluetoothAdapter != null && bluetoothAdapter.isEnabled();
        Log.i(TAG, "isBluetoothEnabled(): " + isEnabled);
        return isEnabled;
    }

    /**
     * It is assumed the caller has already verified and/or obtained the Manifest.permission.BLUETOOTH_CONNECT
     * permission.  Otherwise, this method will throw a permission denied exceptions.
     */
    @SuppressLint("MissingPermission")
    public void enableBluetooth() {
        boolean isEnabled = false;
        Log.i(TAG, "isBluetoothEnabled(): Enter");
        if (bluetoothAdapter != null) {
            isEnabled = bluetoothAdapter.enable();
        }
        Log.i(TAG, "isBluetoothEnabled(): Exit " + isEnabled);
    }

    public boolean isBleAdvertisingSupported() {
        boolean isSupported = bluetoothAdapter != null && bluetoothAdapter.isMultipleAdvertisementSupported();
        Log.i(TAG, "isBleAdvertisingSupported(): " + isSupported);
        return isSupported;
    }

    /**
     * It is assumed the caller has already verified and/or obtained the Manifest.permission.BLUETOOTH_ADVERTISE
     * permission.  Otherwise, this method will throw a permission denied exceptions.
     */
    @SuppressLint("MissingPermission")
    public void startAdvertising(int uniqueCode) {
        Log.i(TAG, "startAdvertising(): Enter");
        BluetoothLeAdvertiser bluetoothAdvertiser = getBluetoothLeAdvertiser();

        if (bluetoothAdvertiser != null) {
            AdvertiseSettings settings = getAdvertiseSettings();
            AdvertiseData data = getAltBeaconAdvertiseData(uniqueCode);
            AdvertiseCallback advertisingCallback = getAdvertiseCallback();

            bluetoothAdvertiser.startAdvertising(settings, data, advertisingCallback);
        }
        else {
            Log.e(TAG, "startAdvertising(): Failed");
        }
        Log.i(TAG, "startAdvertising(): Exit");
    }

    /**
     * It is assumed the caller has already verified and/or obtained the Manifest.permission.BLUETOOTH_ADVERTISE
     * permission.  Otherwise, this method will throw a permission denied exceptions.
     */
    @SuppressLint("MissingPermission")
    public void stopAdvertising() {
        Log.i(TAG, "stopAdvertising(): Enter");
        BluetoothLeAdvertiser bluetoothLeAdvertiser = getBluetoothLeAdvertiser();

        if (bluetoothLeAdvertiser != null) {
            AdvertiseCallback advertisingCallback = getAdvertiseCallback();
            bluetoothLeAdvertiser.stopAdvertising(advertisingCallback);
        } else {
            Log.w(TAG, "stopAdvertising(): Failed...possibly due to Bluetooth being disabled by the user");
        }
        Log.i(TAG, "stopAdvertising(): Exit");
    }

    private BluetoothLeAdvertiser bluetoothLeAdvertiser = null;

    private BluetoothLeAdvertiser getBluetoothLeAdvertiser() {
        Log.i(TAG, "getBluetoothLeAdvertiser(): Enter");
        if (bluetoothAdapter != null) {
            if (bluetoothLeAdvertiser == null) {
                bluetoothLeAdvertiser = bluetoothAdapter.getBluetoothLeAdvertiser();
            }
            Log.i(TAG, "getBluetoothLeAdvertiser(): Exit");
            return bluetoothLeAdvertiser;
        }
        else {
            Log.e(TAG, "Unable to access the Bluetooth LE Advertiser...no bluetoothAdapter");
            return null;
        }
    }

    @NonNull
    private AdvertiseSettings getAdvertiseSettings() {
        return new AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_POWER)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
                .setConnectable(false)
                .setTimeout(0)
                .build();
    }

    @NonNull
    private AdvertiseData getAltBeaconAdvertiseData(int code) {
        ByteBuffer manufacturerData = ByteBuffer.allocate(24);
        // Beacon Code - 0xBEAC the AltBeacon advertisement code
        manufacturerData.putShort(0, (short)0xBEAC); // AltBeacon Identifier
        // Beacon ID - UUID
        byte[] uuid = getIdAsByte(UUID.fromString(App.resources.getString(R.string.ble_uuid)));
        for (int i=2; i<=17; i++) {
            manufacturerData.put(i, uuid[i-2]); // adding the UUID
        }
        // 8 Byte Data - Unique Code
        manufacturerData.putInt(18, code);  // unique code
        // Reference RSSI - A 1-byte value representing the average received signal strength
        // at 1m from the advertiser
        manufacturerData.put(22, (byte)0xCC); // reference RSSI (-52 dBm)
        // Mfg ID - using google's company ID
        int manufacturerId = 224;

        return new AdvertiseData.Builder()
                .addManufacturerData(manufacturerId, manufacturerData.array())
                .build();
    }

    private volatile AdvertiseCallback advertiseCallback = null;
    @NonNull
    private AdvertiseCallback getAdvertiseCallback() {
        if (advertiseCallback == null) {
            synchronized (BleAdvertisingManager.class) {
                if (advertiseCallback == null) {
                    advertiseCallback = new AdvertiseCallback() {
                        @Override
                        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
                            Log.i(TAG, "Advertising onStartSuccess");
                            super.onStartSuccess(settingsInEffect);
                        }

                        @Override
                        public void onStartFailure(int errorCode) {
                            Log.e(TAG, "Advertising onStartFailure: " + errorCode);
                            super.onStartFailure(errorCode);
                        }
                    };
                }
            }
        }
        return advertiseCallback;
    }

    private static byte[] getIdAsByte(java.util.UUID uuid)
    {
        ByteBuffer bb = ByteBuffer.wrap(new byte[16]);
        bb.putLong(uuid.getMostSignificantBits());
        bb.putLong(uuid.getLeastSignificantBits());
        return bb.array();
    }
}
