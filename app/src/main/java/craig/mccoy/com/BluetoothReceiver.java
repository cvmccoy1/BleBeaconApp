package craig.mccoy.com;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

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
        Log.i(TAG, "onReceive():Enter for " + context);
        String action = intent.getAction();
        if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
            int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1);
            Log.i(TAG, "onReceive():state " + state);
            for (Map.Entry<Integer, Runnable> entry : registeredCallbacks.entrySet()) {
                if (entry.getKey().equals(state)) {
                    Runnable currentCallback;
                    synchronized (this) {
                        currentCallback = entry.getValue();
                    }
                    if (currentCallback != null) {
                        Log.i(TAG, "onReceive(): Calling back on " + state );
                        currentCallback.run();
                    }
                }
            }
        }
        Log.i(TAG, "onReceive():Exit");
    }

    public synchronized  void registerBluetoothStateChanged(int state, @NonNull Runnable callback) {
        Log.i(TAG, "registerBluetoothStateChanged(" + state + "): Enter ");
        if (registeredCallbacks.size() == 0) {
            Log.i(TAG, "registerBluetoothStateChanged(): Register the Receiver");
            parentContext.registerReceiver(this, bluetoothStateChangedIntent);
        }
        if (registeredCallbacks.put(state, callback) != null)
        {
            Log.w(TAG, "registerBluetoothStateChanged(): " + state + " already registered");
        }
        Log.i(TAG, "registerBluetoothStateChanged(): Exit");
    }

    public synchronized  void unregisterBluetoothStateChanged(int state) {
        Log.i(TAG, "unregisterBluetoothStateChanged(" + state + "): Enter ");

        if (registeredCallbacks.remove(state) == null) {
            Log.w(TAG, "unregisterBluetoothStateChanged(): " + state + " is not registered");
        } else if (registeredCallbacks.size() == 0) {
            Log.i(TAG, "unregisterBluetoothStateChanged(): Unregister the Receiver");
            parentContext.unregisterReceiver(this);
        }

        Log.i(TAG, "unregisterBluetoothStateChanged(): Exit");
    }
}
