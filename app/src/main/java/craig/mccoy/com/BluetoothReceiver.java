package craig.mccoy.com;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import androidx.annotation.NonNull;
import java.util.function.Consumer;

public class BluetoothReceiver extends BroadcastReceiver {
    private static final String TAG = "BluetoothReceiver";

    private final Context parentContext;
    private volatile boolean isRegistered = false;
    private volatile Consumer<Integer> parentCallback = null;
    //private Map<Integer, Runnable> registeredCallbacks = new HashMap<Integer, Runnable>();

    public  BluetoothReceiver(Context context) {
        parentContext = context;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i(TAG, "onReceive():Enter");
        String action = intent.getAction();

         if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
             int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1);
             Log.i(TAG, "onReceive():state" + state);
             //receiverCallback.onBluetoothAdapterStateChange(state);
             Consumer<Integer> currentCallback = parentCallback;
             if (currentCallback != null) {
                 Log.i(TAG, "onReceive(): Calling back to " + currentCallback);
                 currentCallback.accept(state);
             }
        }
        Log.i(TAG, "onReceive():Exit");
    }

    public synchronized  void registerBluetoothReceiver(@NonNull Consumer<Integer>callback) {
        Log.i(TAG, "registerBluetoothReceiver(): Enter " + callback);
        parentCallback = callback;
        if (!isRegistered) {
            parentContext.registerReceiver(this, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
            isRegistered = true;
        } else {
            Log.w(TAG, "registerBluetoothReceiver(): bluetoothReceiver already registered");
        }
        Log.i(TAG, "registerBluetoothReceiver(): Exit");
    }

    public synchronized  void unregisterBluetoothReceiver() {
        Log.i(TAG, "unregisterBluetoothReceiver(): Enter");
        if (isRegistered) {
            parentContext.unregisterReceiver(this);
            parentCallback = null;
            isRegistered = false;
        } else {
            Log.w(TAG, "unregisterBluetoothReceiver(): bluetoothReceiver already unregistered");
        }
        Log.i(TAG, "unregisterBluetoothReceiver(): Exit");
    }
}
