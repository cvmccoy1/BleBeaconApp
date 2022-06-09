package craig.mccoy.com;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import androidx.annotation.NonNull;

import java.util.HashMap;
import java.util.Map;

public class BluetoothReceiver extends BroadcastReceiver {
    private static final String TAG = "BLE:BluetoothReceiver";

    private final Context parentContext;
    private final Map<Integer, Runnable> registeredCallbacks = new HashMap<>();
    private final IntentFilter bluetoothStateChangedIntent = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);

    public  BluetoothReceiver(Context context) {
        parentContext = context;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        MyLog.i(TAG, "onReceive():Enter for " + context);
        String action = intent.getAction();
        if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
            int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1);
            MyLog.i(TAG, "onReceive():state " + state);
            Runnable currentCallback = null;
            synchronized (this) {
                for (Map.Entry<Integer, Runnable> entry : registeredCallbacks.entrySet()) {
                    if (entry.getKey().equals(state)) {
                        currentCallback = entry.getValue();
                        break;
                    }
                }
            }
            if (currentCallback != null) {
                MyLog.i(TAG, "onReceive(): Calling back on " + state);
                currentCallback.run();
            }
        }
        MyLog.i(TAG, "onReceive():Exit");
    }

    public synchronized  void registerBluetoothStateChanged(int state, @NonNull Runnable callback) {
        MyLog.i(TAG, "registerBluetoothStateChanged(" + state + "): Enter ");
        if (registeredCallbacks.size() == 0) {
            MyLog.i(TAG, "registerBluetoothStateChanged(): Register the Receiver");
            parentContext.registerReceiver(this, bluetoothStateChangedIntent);
        }
        if (registeredCallbacks.put(state, callback) != null)
        {
            MyLog.w(TAG, "registerBluetoothStateChanged(): " + state + " already registered");
        }
        MyLog.i(TAG, "registerBluetoothStateChanged(): Exit");
    }

    public synchronized  void unregisterBluetoothStateChanged(int state) {
        MyLog.i(TAG, "unregisterBluetoothStateChanged(" + state + "): Enter ");

        if (registeredCallbacks.remove(state) == null) {
            MyLog.w(TAG, "unregisterBluetoothStateChanged(): " + state + " is not registered");
        } else if (registeredCallbacks.size() == 0) {
            MyLog.i(TAG, "unregisterBluetoothStateChanged(): Unregister the Receiver");
            parentContext.unregisterReceiver(this);
        }

        MyLog.i(TAG, "unregisterBluetoothStateChanged(): Exit");
    }
}
