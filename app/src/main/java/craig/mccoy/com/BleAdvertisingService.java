package craig.mccoy.com;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.os.IBinder;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

public class BleAdvertisingService extends Service {
    private static final String TAG = "BLE:BleAdvertisingService";

    public static volatile boolean isServiceAdvertising = false;
    private BluetoothReceiver bluetoothReceiver = null;

    @Override
    public void onCreate() {
        MyLog.i(TAG, "onCreate(): Enter");
        super.onCreate();
        bluetoothReceiver = new BluetoothReceiver(this);
        MyLog.i(TAG, "onCreate(): Exit");
    }

    @Override
    public int onStartCommand(@NonNull Intent intent, int flags, int startId) {
        MyLog.i(TAG, String.format("onStartCommand(%o, %o)", flags, startId));
        isServiceAdvertising = true;

        String beaconTypeString = intent.getStringExtra(getString(R.string.beacon_type));
        int uniqueCode = intent.getIntExtra(getString(R.string.unique_code), 0);
        MyLog.i(TAG, String.format("onStartCommand: inputExtra = %x, beaconType = %s", uniqueCode, beaconTypeString));
        BeaconType beaconType = BeaconType.valueOf(beaconTypeString);

        Intent notificationIntent = new Intent(this, MainActivity.class);
        notificationIntent.putExtra("ACTIVITY_NAME", "From Service");
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        Notification notification = new NotificationCompat.Builder(this, getString(R.string.service_channel_id))
                .setContentTitle(getString(R.string.ble_advertising_service_title))
                .setContentText(getContextText(beaconType, uniqueCode))
                .setSmallIcon(R.drawable.ic_ble_beacon)
                .setContentIntent(pendingIntent)
                .build();

        startForeground(1, notification);

        BleAdvertisingManager.getInstance().startAdvertising(beaconType, uniqueCode);

        bluetoothReceiver.registerBluetoothStateChanged(BluetoothAdapter.STATE_TURNING_OFF, () -> {
            MyLog.i(TAG, "The local Bluetooth adapter is turning off. Stop BLE Advertising.");
            stopAdvertising();
            stopSelf();
        });

        MyLog.i(TAG, "onStartCommand(): Exit");
        return START_NOT_STICKY;
    }

    private String getContextText( BeaconType beaconType, int uniqueCode) {
        String contentText;
        switch (beaconType) {
            case IBeacon:
                contentText = getString(R.string.ble_advertising_i_beacon_text);
                break;
            case AltBeacon:
            default:
                contentText = String.format(getString(R.string.ble_advertising_alt_beacon_text_format), uniqueCode);
                break;
        }
        return contentText;
    }

    @Override
    public void onDestroy() {
        MyLog.i(TAG, "onDestroy(): Enter");
        bluetoothReceiver.unregisterBluetoothStateChanged(BluetoothAdapter.STATE_TURNING_OFF);
        stopAdvertising();
        super.onDestroy();
        MyLog.i(TAG, "onDestroy(): Exit");
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        MyLog.i(TAG, "onBind(): Enter/Exit");
        return null;
    }

    private void stopAdvertising() {
        MyLog.i(TAG, "stopAdvertising():isServiceAdvertising = " + isServiceAdvertising);
        if (isServiceAdvertising) {
            BleAdvertisingManager.getInstance().stopAdvertising();
            isServiceAdvertising = false;
        }
        MyLog.i(TAG, "stopAdvertising(): Exit");
    }
}
