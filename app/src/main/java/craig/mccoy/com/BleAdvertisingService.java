package craig.mccoy.com;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

public class BleAdvertisingService extends Service {
    private static final String TAG = "BLE:BleAdvertisingService";

    public static volatile boolean isServiceAdvertising = false;
    private BluetoothReceiver bluetoothReceiver = null;

    @Override
    public void onCreate() {
        Log.i(TAG, "onCreate(): Enter");
        super.onCreate();
        bluetoothReceiver = new BluetoothReceiver(this);
        Log.i(TAG, "onCreate(): Exit");
    }

    @Override
    public int onStartCommand(@NonNull Intent intent, int flags, int startId) {
        Log.i(TAG, String.format("onStartCommand(%o, %o)", flags, startId));
        isServiceAdvertising = true;

        int uniqueCode = intent.getIntExtra(getString(R.string.unique_code), 0);
        Log.i(TAG, String.format("onStartCommand: inputExtra = %x", uniqueCode));

        Intent notificationIntent = new Intent(this, MainActivity.class);
        notificationIntent.putExtra("ACTIVITY_NAME", "From Service");
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);

        Notification notification = new NotificationCompat.Builder(this, getString(R.string.service_channel_id))
                .setContentTitle(getString(R.string.ble_advertising_service_title))
                .setContentText(String.format(getString(R.string.ble_advertising_service_text_format), uniqueCode))
                .setSmallIcon(R.drawable.ic_ble_beacon)
                .setContentIntent(pendingIntent)
                .build();

        startForeground(1, notification);

        BleAdvertisingManager.getInstance().startAdvertising(uniqueCode);

        bluetoothReceiver.registerBluetoothStateChanged(BluetoothAdapter.STATE_TURNING_OFF, () -> {
            Log.i(TAG, "The local Bluetooth adapter is turning off. Stop BLE Advertising.");
            stopAdvertising();
            stopSelf();
        });

        Log.i(TAG, "onStartCommand(): Exit");
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "onDestroy(): Enter");
        bluetoothReceiver.unregisterBluetoothStateChanged(BluetoothAdapter.STATE_TURNING_OFF);
        stopAdvertising();
        super.onDestroy();
        Log.i(TAG, "onDestroy(): Exit");
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Log.i(TAG, "onBind(): Enter/Exit");
        return null;
    }

    private void stopAdvertising() {
        Log.i(TAG, "stopAdvertising():isServiceAdvertising = " + isServiceAdvertising);
        if (isServiceAdvertising) {
            BleAdvertisingManager.getInstance().stopAdvertising();
            isServiceAdvertising = false;
        }
        Log.i(TAG, "stopAdvertising(): Exit");
    }
}
