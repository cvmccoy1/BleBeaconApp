package craig.mccoy.com;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;

import androidx.annotation.NonNull;

import java.nio.ByteBuffer;
import java.util.UUID;

enum BeaconType {
    Ble1MBeacon,
    AltBeacon,
    IBeacon
}

public class BleAdvertisingManager {
    private static final String TAG = "BLE:BleAdvertisingManager";

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
                        MyLog.e(TAG, "getInstance(): Unable to access the Bluetooth Manager");
                    } else {
                        bluetoothAdapter = bluetoothManager.getAdapter();
                        if (bluetoothAdapter == null) {
                            MyLog.e(TAG, "getInstance(): Unable to access the Bluetooth Adapter");
                        }
                    }
                }
            }
        }
        return instance;
    }

    public boolean isBluetoothEnabled() {
        boolean isEnabled = bluetoothAdapter != null && bluetoothAdapter.isEnabled();
        MyLog.i(TAG, "isBluetoothEnabled(): " + isEnabled);
        return isEnabled;
    }

    /**
     * It is assumed the caller has already verified and/or obtained the Manifest.permission.BLUETOOTH_CONNECT
     * permission.  Otherwise, this method will throw a permission denied exceptions.
     */
    @SuppressLint("MissingPermission")
    public void enableBluetooth() {
        boolean isEnabled = false;
        MyLog.i(TAG, "isBluetoothEnabled(): Enter");
        if (bluetoothAdapter != null) {
            isEnabled = bluetoothAdapter.enable();
        }
        MyLog.i(TAG, "isBluetoothEnabled(): Exit " + isEnabled);
    }

    public boolean isBleAdvertisingSupported() {
        boolean isSupported = bluetoothAdapter != null && bluetoothAdapter.isMultipleAdvertisementSupported();
        MyLog.i(TAG, "isBleAdvertisingSupported(): " + isSupported);
        return isSupported;
    }

    /**
     * It is assumed the caller has already verified and/or obtained the Manifest.permission.BLUETOOTH_ADVERTISE
     * permission.  Otherwise, this method will throw a permission denied exceptions.
     */
    @SuppressLint("MissingPermission")
    public void startAdvertising(BeaconType beaconType, int uniqueCode) {
        MyLog.i(TAG, "startAdvertising(): Enter");
        BluetoothLeAdvertiser bluetoothAdvertiser = getBluetoothLeAdvertiser();

        if (bluetoothAdvertiser != null) {
            AdvertiseSettings settings = getAdvertiseSettings();
            AdvertiseData data = getAdvertiseData(beaconType, uniqueCode);
            AdvertiseCallback advertisingCallback = getAdvertiseCallback();

            bluetoothAdvertiser.startAdvertising(settings, data, advertisingCallback);
        }
        else {
            MyLog.e(TAG, "startAdvertising(): Failed");
        }
        MyLog.i(TAG, "startAdvertising(): Exit");
    }

    /**
     * It is assumed the caller has already verified and/or obtained the Manifest.permission.BLUETOOTH_ADVERTISE
     * permission.  Otherwise, this method will throw a permission denied exceptions.
     */
    @SuppressLint("MissingPermission")
    public void stopAdvertising() {
        MyLog.i(TAG, "stopAdvertising(): Enter");
        BluetoothLeAdvertiser bluetoothLeAdvertiser = getBluetoothLeAdvertiser();

        if (bluetoothLeAdvertiser != null) {
            AdvertiseCallback advertisingCallback = getAdvertiseCallback();
            bluetoothLeAdvertiser.stopAdvertising(advertisingCallback);
        } else {
            MyLog.w(TAG, "stopAdvertising(): Failed...possibly due to Bluetooth being disabled by the user");
        }
        MyLog.i(TAG, "stopAdvertising(): Exit");
    }

    private BluetoothLeAdvertiser bluetoothLeAdvertiser = null;

    private BluetoothLeAdvertiser getBluetoothLeAdvertiser() {
        MyLog.i(TAG, "getBluetoothLeAdvertiser(): Enter");
        if (bluetoothAdapter != null) {
            if (bluetoothLeAdvertiser == null) {
                bluetoothLeAdvertiser = bluetoothAdapter.getBluetoothLeAdvertiser();
            }
            MyLog.i(TAG, "getBluetoothLeAdvertiser(): Exit");
            return bluetoothLeAdvertiser;
        }
        else {
            MyLog.e(TAG, "Unable to access the Bluetooth LE Advertiser...no bluetoothAdapter");
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
    private AdvertiseData getAdvertiseData(BeaconType beaconType, int uniqueCode) {
        AdvertiseData data;
        switch (beaconType) {
            case AltBeacon:
                data = getAltBeaconAdvertiseData(uniqueCode);
                break;
            case IBeacon:
                data = getIBeaconAdvertiseData();
                break;
            case Ble1MBeacon:
            default:
                data = getBle1MPhyAdvertiseData();
        }
        return data;
    }

    @NonNull
    private AdvertiseData getBle1MPhyAdvertiseData() {
        return new AdvertiseData.Builder()
                .setIncludeDeviceName(true)
                .setIncludeTxPowerLevel(true)
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
        // Mfg Reserved
        manufacturerData.put(23, (byte)0);
        // Mfg ID - using google's company ID
        int manufacturerId = 224;

        return new AdvertiseData.Builder()
                .addManufacturerData(manufacturerId, manufacturerData.array())
                .build();
    }

    @NonNull
    private AdvertiseData getIBeaconAdvertiseData() {
        ByteBuffer manufacturerData = ByteBuffer.allocate(23);
        // Beacon Code - 0xBEAC the AltBeacon advertisement code
        manufacturerData.putShort(0, (short)0x0215); // iBeacon Identifier
        // Beacon ID - UUID
        byte[] uuid = getIdAsByte(UUID.fromString(App.resources.getString(R.string.ble_uuid)));
        for (int i=2; i<=17; i++) {
            manufacturerData.put(i, uuid[i-2]); // adding the UUID
        }
        // Major Version Number
        manufacturerData.putShort(18, (short) 0x0001);
        // Minor Version Number
        manufacturerData.putShort(20, (short) 0x0000);
        // Reference RSSI - A 1-byte value representing the average received signal strength
        // at 1m from the advertiser
        manufacturerData.put(22, (byte)0xCC); // reference RSSI (-52 dBm)

        // Mfg ID - using apple's company ID
        int manufacturerId = 76;
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
                            MyLog.i(TAG, "Advertising onStartSuccess");
                            super.onStartSuccess(settingsInEffect);
                        }

                        @Override
                        public void onStartFailure(int errorCode) {
                            MyLog.e(TAG, "Advertising onStartFailure: " + errorCode);
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
