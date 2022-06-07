package craig.mccoy.com;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

//public class MainActivity extends AppCompatActivity implements IBluetoothReceiverCallback {
public class MainActivity extends AppCompatActivity {
    private static final String TAG = "BLE:MainActivity";

    private static final int REQUEST_CODE = 1234;
    private EditText editTextInput;
    private BluetoothReceiver bluetoothReceiver = null;
    private String activityName;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        TextView text = findViewById(R.id.activity_name_text);
        activityName = getIntent().getStringExtra("ACTIVITY_NAME");
        if (activityName == null) {
            activityName = text.getText().toString();
        } else {
            text.setText(activityName);
        }

        Log.i(TAG, "onCreate(): Enter " + activityName);
        editTextInput = findViewById(R.id.edit_text_input);
        bluetoothReceiver = new BluetoothReceiver(this);
        Log.i(TAG, "onCreate(): Exit");
    }

    @Override
    protected void onStart() {
        Log.i(TAG, "onStart(): Enter/Exit " + activityName);
        super.onStart();
    }

    @Override
    protected void onResume() {
        Log.i(TAG, "onResume(): Enter/Exit " + activityName);
        super.onResume();
    }

    @Override
    protected void onPause() {
        Log.i(TAG, "onPause(): Enter/Exit " + activityName);
        super.onPause();
    }

    @Override
    protected void onStop() {
        Log.i(TAG, "onStop(): Enter/Exit " + activityName);
        super.onStop();
    }

    @Override
    protected void onRestart() {
        Log.i(TAG, "onRestart(): Enter/Exit " + activityName);
        super.onRestart();
    }

    @Override
    protected void onDestroy() {
        Log.i(TAG, "onDestroy(): Enter " + activityName);
        super.onDestroy();
        bluetoothReceiver.unregisterBluetoothStateChanged(BluetoothAdapter.STATE_ON);
        if (!isBleAdvertisingServiceRunning()) {
            Log.i(TAG, "onDestroy(): destroying Activity while the Service is not running...kill application");
            finishAndRemoveTask();
            System.exit(0);
        }
        Log.i(TAG, "onDestroy(): Exit");
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        Log.i(TAG, "onRequestPermissionsResult(): Enter " + activityName);
        Log.i(TAG, "onRequestPermissionsResult(): requestCode =" + requestCode);
        Log.i(TAG, "onRequestPermissionsResult(): permissions =" + Arrays.toString(permissions));
        Log.i(TAG, "onRequestPermissionsResult(): grantResults =" + Arrays.toString(grantResults));
        if (requestCode == REQUEST_CODE) {
            boolean hasPermission = true;
            for (int i = 0; i < permissions.length; i++) {
                if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    Log.i(TAG, "Permission Granted: " + permissions[i]);
                } else if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
                    Log.i(TAG, "Permission Denied: " + permissions[i]);
                    hasPermission = false;
                }
            }
            Log.i(TAG, "onRequestPermissionsResult(): Has Permission " + hasPermission);
            if (hasPermission) {
                checkForBleAdvertisingSupported();
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
        Log.i(TAG, "onRequestPermissionsResult(): Exit");
    }

    public void onClickStartAdvertising(@SuppressWarnings("unused") View view) {
        Log.i(TAG, "startService(): Enter "  + activityName);
        startBleAdvertising();
        Log.i(TAG, "startService(): Exit");
    }

    public void onClickStopAdvertising(@SuppressWarnings("unused") View view) {
        Log.i(TAG, "stopAdvertising(): Enter"  + activityName);
        stopBleAdvertising();
        Log.i(TAG, "stopAdvertising(): Exit");
    }

    public void startBleAdvertising() {
        checkForBleSupported();
    }

    public void stopBleAdvertising() {
        stopAdvertisingService();
    }

    private void checkForBleSupported() {
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_LONG).show();
            disableUiButtons();
        } else {
            checkForPermissions();
        }
    }

    private void checkForPermissions() {
        boolean hasConnectPermission = ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED;
        Log.i(TAG, "checkPermissions(): hasConnectPermission = " + hasConnectPermission);

        boolean hasAdvertisePermission = ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADVERTISE) == PackageManager.PERMISSION_GRANTED;
        Log.i(TAG, "checkPermissions(): hasAdvertisePermission = " + hasAdvertisePermission);

        List<String> permissions = new ArrayList<>();
        if (!hasConnectPermission) {
            permissions.add( Manifest.permission.BLUETOOTH_CONNECT );
        }
        if (!hasAdvertisePermission) {
            permissions.add( Manifest.permission.BLUETOOTH_ADVERTISE );
        }
        if (!permissions.isEmpty()) {
            Log.i(TAG, "checkPermissions(): Checking for permissions - " + permissions);
            // The following will result in the 'onRequestPermissionsResult' callback being executed
            requestPermissions(permissions.toArray(new String[0]), REQUEST_CODE);
        }
        else {
            okToAttemptAdvertising();
        }
    }

    private void okToAttemptAdvertising()
    {
        Log.i(TAG, "okToStartAdvertising(): Enter");
        if (BleAdvertisingManager.getInstance().isBluetoothEnabled()) {
            checkForBleAdvertisingSupported();
        } else {
            bluetoothReceiver.registerBluetoothStateChanged(BluetoothAdapter.STATE_ON, () -> {
                Log.i(TAG, "BluetoothReceiver Callback: The local Bluetooth adapter is on and ready for use.");
                bluetoothReceiver.unregisterBluetoothStateChanged(BluetoothAdapter.STATE_ON);
                checkForBleAdvertisingSupported();
            });
            // The following should cause the callback from the previous statement to be executed when Bluetooth has been enabled
            BleAdvertisingManager.getInstance().enableBluetooth();
            Log.w(TAG, "okToStartAdvertising(): Attempting to enable the bluetooth adapter...wait for it to finish enabling");
            Toast.makeText(this, R.string.waiting_for_bluetooth_enable, Toast.LENGTH_SHORT).show();
        }
        Log.i(TAG, "okToStartAdvertising(): Exit");
    }

    private void checkForBleAdvertisingSupported() {
        Log.i(TAG, "checkBleAdvertisingSupported(): Enter");

        if (BleAdvertisingManager.getInstance().isBleAdvertisingSupported()) {
            startAdvertisingService();
        } else {
            Log.w(TAG, "BLE Advertisement is not supported...disable buttons");
            Toast.makeText(this, R.string.advertisement_not_supported, Toast.LENGTH_SHORT).show();
            disableUiButtons();
        }
        Log.i(TAG, "checkBleAdvertisingSupported(): Exit");
    }

    private void startAdvertisingService() {
        Log.i(TAG, "startAdvertisingService(): Enter");

        stopAdvertisingService();

        String input = editTextInput.getText().toString();
        int uniqueCode = input.isEmpty() ? 0 : Integer.parseUnsignedInt(input, 16);
        Intent serviceIntent = new Intent(this, BleAdvertisingService.class);
        serviceIntent.putExtra(getString(R.string.unique_code), uniqueCode);
        ContextCompat.startForegroundService(this, serviceIntent);
        Toast.makeText(this, getString(R.string.advertising_started), Toast.LENGTH_LONG).show();
        Log.i(TAG, "startAdvertisingService(): Exit");
    }

    private void stopAdvertisingService() {
        if (isBleAdvertisingServiceRunning()) {
            Intent serviceIntent = new Intent(this, BleAdvertisingService.class);
            stopService(serviceIntent);
            Toast.makeText(this, getString(R.string.advertising_stopped), Toast.LENGTH_LONG).show();
        }
    }

    private boolean isBleAdvertisingServiceRunning() {
        boolean isRunning = BleAdvertisingService.isServiceAdvertising;
        Log.i(TAG, "isBleAdvertisingServiceRunning(): " + isRunning);
        return isRunning;
    }

    private void disableUiButtons() {
        Button startAdvertisingButton = findViewById(R.id.start_advertising_button);
        Button stopAdvertisingButton = findViewById(R.id.stop_advertising_button);
        startAdvertisingButton.setEnabled(false);
        stopAdvertisingButton.setEnabled(false);
    }
}