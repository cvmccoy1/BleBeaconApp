package craig.mccoy.com;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.res.Resources;
import android.util.Log;

public class App extends Application {
    private static final String TAG = "App";

    public static BluetoothManager bluetoothManager;
    public static Resources resources;

    @Override
    public void onCreate() {
        super.onCreate();

        Log.i(TAG, "onCreate(): Enter");
        bluetoothManager = (BluetoothManager) getApplicationContext().getSystemService(Context.BLUETOOTH_SERVICE);
        resources = getResources();
        createNotificationChannel();
        Log.i(TAG, "onCreate(): Exit");
    }

    private void createNotificationChannel() {
        NotificationChannel serviceChannel = new NotificationChannel(
                getString(R.string.service_channel_id),
                getString(R.string.service_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT
        );
        NotificationManager manager = getSystemService(NotificationManager.class);
        manager.createNotificationChannel(serviceChannel);
    }
}
