package craig.mccoy.com;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseSettings;
import android.content.Context;
import android.content.Intent;

public class App extends Application {
    private static final String TAG = "BLE:App";

    private static App myApp;
    private static volatile AdvertiseCallback advertiseCallback = null;
    public static Intent serviceIntent = null;

    public static BluetoothManager getBluetoothManager() {
        return (BluetoothManager)myApp.getSystemService(Context.BLUETOOTH_SERVICE);
    }
    public static String getAppString(int id) {
        return myApp.getResources().getString(id);
    }

    public App() {
        MyLog.i(TAG, "Constructor(): Enter/Exit");
        myApp = this;
    }

    @Override
    public void onCreate() {
        MyLog.i(TAG, "onCreate(): Enter");
        super.onCreate();
        createNotificationChannel();
        MyLog.i(TAG, "onCreate(): Exit");
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

    /**
     * Moved this method out of the BleAdvertisingManager class because it uses a
     * static value referencing an object (an AdvertiseCallback class).  If kept there,
     * it would prevent the BleAdvertisingManager class from being garbage collected,
     * causing possibly a memory leak or at least unnecessary memory usage.
     * @param createIfNull - creates the AdvertiseCallback object if not already created.
     * @return the AdvertiseCallback object.
     */
    public static synchronized AdvertiseCallback getAdvertiseCallback(boolean createIfNull) {
        if (advertiseCallback == null || createIfNull) {
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
            MyLog.i(TAG, "createAdvertiseCallback(): advertiseCallback created");
        } else {
            MyLog.i(TAG, "createAdvertiseCallback(): advertiseCallback already created");
        }
        return advertiseCallback;
    }
}
