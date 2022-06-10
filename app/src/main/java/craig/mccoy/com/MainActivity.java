package craig.mccoy.com;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "BLE:MainActivity";
    private static final int REQUEST_CODE = 1234;

    private Intent serviceIntent = null;
    private BluetoothReceiver bluetoothReceiver = null;
    private SharedPreferences sharedPreferences = null;

    private String activityName;
    private EditText editTextInput;
    private RadioGroup beaconTypeRadioGroup;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        TextView text = findViewById(R.id.activity_name_text);
        activityName = getIntent().getStringExtra("ACTIVITY_NAME");
        MyLog.i(TAG, "onCreate() activityName = " + activityName);
        if (activityName == null) {
            activityName = text.getText().toString();
        } else {
            text.setText(activityName);
        }

        MyLog.i(TAG, "onCreate(): Enter " + activityName);

        serviceIntent = new Intent(this, BleAdvertisingService.class);
        bluetoothReceiver = new BluetoothReceiver(this);
        sharedPreferences = getPreferences(MODE_PRIVATE);

        beaconTypeRadioGroup = findViewById(R.id.beaconTypeGroupRadio);
        editTextInput = findViewById(R.id.edit_text_input);

        MyLog.i(TAG, "onCreate(): Exit");
    }

    @Override
    protected void onStart() {
        MyLog.i(TAG, "onStart(): Enter" + activityName);
        super.onStart();

        String uniqueCodeString = sharedPreferences.getString(getString(R.string.unique_code_string), "");
        editTextInput.setText(uniqueCodeString);

        int checkedBeaconTypeId = sharedPreferences.getInt(getString(R.string.checked_beacon_type_id), R.id.altBeaconRadioButton);
        beaconTypeRadioGroup.clearCheck();
        beaconTypeRadioGroup.check(checkedBeaconTypeId);

        findViewById(R.id.stop_advertising_button).setEnabled(isBleAdvertisingServiceRunning());

        setTextInputEnabled();
        MyLog.i(TAG, "onStart(): Exit");
    }

    @Override
    protected void onResume() {
        MyLog.i(TAG, "onResume(): Enter/Exit " + activityName);
        super.onResume();
    }

    @Override
    protected void onPause() {
        MyLog.i(TAG, "onPause(): Enter/Exit " + activityName);
        super.onPause();
    }

    @Override
    protected void onStop() {
        MyLog.i(TAG, "onStop(): Enter/Exit " + activityName);
        super.onStop();
    }

    @Override
    protected void onRestart() {
        MyLog.i(TAG, "onRestart(): Enter/Exit " + activityName);
        super.onRestart();
    }

    @Override
    protected void onDestroy() {
        MyLog.i(TAG, "onDestroy(): Enter " + activityName);
        super.onDestroy();
        bluetoothReceiver.unregisterBluetoothStateChanged(BluetoothAdapter.STATE_ON);
        if (!isBleAdvertisingServiceRunning()) {
            MyLog.i(TAG, "onDestroy(): destroying Activity while the Service is not running...kill application");
            finishAndRemoveTask();
            System.exit(0);
        }
        MyLog.i(TAG, "onDestroy(): Exit");
    }

    public void onRadioButtonClicked(@SuppressWarnings("unused") View view) {
        MyLog.i(TAG, "onRadioButtonClicked(): Enter");
        setTextInputEnabled();
        MyLog.i(TAG, "onRadioButtonClicked(): Exit");
    }

    public void onClickStartAdvertising(@SuppressWarnings("unused") View view) {
        MyLog.i(TAG, "startService(): Enter "  + activityName);
        startBleAdvertising();
        MyLog.i(TAG, "startService(): Exit");
    }

    public void onClickStopAdvertising(@SuppressWarnings("unused") View view) {
        MyLog.i(TAG, "stopAdvertising(): Enter"  + activityName);
        stopBleAdvertising();
        MyLog.i(TAG, "stopAdvertising(): Exit");
    }

    public void startBleAdvertising() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(getString(R.string.unique_code_string), editTextInput.getText().toString());
        editor.putInt(getString(R.string.checked_beacon_type_id), beaconTypeRadioGroup.getCheckedRadioButtonId());
        editor.apply();
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
        boolean hasAdvertisePermission = ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADVERTISE) == PackageManager.PERMISSION_GRANTED;

        List<String> permissions = new ArrayList<>();
        if (!hasConnectPermission) {
            permissions.add( Manifest.permission.BLUETOOTH_CONNECT );
        }
        if (!hasAdvertisePermission) {
            permissions.add( Manifest.permission.BLUETOOTH_ADVERTISE );
        }
        if (!permissions.isEmpty()) {
            MyLog.i(TAG, "checkPermissions(): Requesting permissions for " + permissions);
            // The following will result in the 'onRequestPermissionsResult' callback being executed
            requestPermissions(permissions.toArray(new String[0]), REQUEST_CODE);
        }
        else {
            MyLog.i(TAG, "checkPermissions(): App already has the required permissions...moving on");
            okToAttemptAdvertising();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        MyLog.i(TAG, "onRequestPermissionsResult(): Enter " + activityName);
        if (requestCode == REQUEST_CODE) {
            boolean hasPermission = true;
            for (int i = 0; i < permissions.length; i++) {
                if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    MyLog.i(TAG, "Permission Granted: " + permissions[i]);
                } else if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
                    MyLog.i(TAG, "Permission Denied: " + permissions[i]);
                    hasPermission = false;
                }
            }
            MyLog.i(TAG, "onRequestPermissionsResult(): Has Permission " + hasPermission);
            if (hasPermission) {
                okToAttemptAdvertising();
            } else {
                Toast.makeText(this, R.string.required_permissions_not_granted, Toast.LENGTH_LONG).show();
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
        MyLog.i(TAG, "onRequestPermissionsResult(): Exit");
    }

    private void okToAttemptAdvertising()
    {
        MyLog.i(TAG, "okToStartAdvertising(): Enter");
        if (BleAdvertisingManager.getInstance().isBluetoothEnabled()) {
            checkForBleAdvertisingSupported();
        } else {
            // Bluetooth not enabled, so attempt to enable it and wait for the BT adapter to signal is it ready for use
            bluetoothReceiver.registerBluetoothStateChanged(BluetoothAdapter.STATE_ON, () -> {
                MyLog.i(TAG, "BluetoothReceiver Callback: The local Bluetooth adapter is on and ready for use.");
                bluetoothReceiver.unregisterBluetoothStateChanged(BluetoothAdapter.STATE_ON);
                checkForBleAdvertisingSupported();
            });
            // The following should cause the callback from the previous statement to be executed when the BT adapter is ready
            BleAdvertisingManager.getInstance().enableBluetooth();
            MyLog.w(TAG, "okToStartAdvertising(): Attempting to enable the bluetooth adapter...wait for it to finish enabling");
            Toast.makeText(this, R.string.waiting_for_bluetooth_enable, Toast.LENGTH_SHORT).show();
        }
        MyLog.i(TAG, "okToStartAdvertising(): Exit");
    }

    private void checkForBleAdvertisingSupported() {
        MyLog.i(TAG, "checkBleAdvertisingSupported(): Enter");

        if (BleAdvertisingManager.getInstance().isBleAdvertisingSupported()) {
            startAdvertisingService();
        } else {
            MyLog.w(TAG, "BLE Advertisement is not supported...disable buttons");
            Toast.makeText(this, R.string.advertisement_not_supported, Toast.LENGTH_SHORT).show();
            disableUiButtons();
        }
        MyLog.i(TAG, "checkBleAdvertisingSupported(): Exit");
    }

    private void startAdvertisingService() {
        MyLog.i(TAG, "startAdvertisingService(): Enter");

        // Stop any previously started BLE Advertising Service
        stopAdvertisingService();
        findViewById(R.id.stop_advertising_button).setEnabled(true);

        // Send the beaconType and uniqueCode to the service
        String beaconType = (beaconTypeRadioGroup.getCheckedRadioButtonId() == R.id.altBeaconRadioButton ? BeaconType.AltBeacon : BeaconType.IBeacon).name();
        String input = editTextInput.getText().toString();
        int uniqueCode = input.isEmpty() ? 0 : Integer.parseUnsignedInt(input, 16);
        serviceIntent.putExtra(getString(R.string.beacon_type), beaconType);
        serviceIntent.putExtra(getString(R.string.unique_code), uniqueCode);
        // Start the service as a Foreground Service so the system won't kill it after 15 minutes
        ContextCompat.startForegroundService(this, serviceIntent);
        Toast.makeText(this, getString(R.string.advertising_started), Toast.LENGTH_LONG).show();
        MyLog.i(TAG, "startAdvertisingService(): Exit");
    }

    private void stopAdvertisingService() {
        if (isBleAdvertisingServiceRunning()) {
            stopService(serviceIntent);
            findViewById(R.id.stop_advertising_button).setEnabled(false);
            Toast.makeText(this, getString(R.string.advertising_stopped), Toast.LENGTH_LONG).show();
        }
    }

    private boolean isBleAdvertisingServiceRunning() {
        boolean isRunning = BleAdvertisingService.isServiceAdvertising;
        MyLog.i(TAG, "isBleAdvertisingServiceRunning(): " + isRunning);
        return isRunning;
    }

    private void setTextInputEnabled() {
        boolean isTextInputEnabled= beaconTypeRadioGroup.getCheckedRadioButtonId() == R.id.altBeaconRadioButton;
        MyLog.i(TAG, "setTextInputEnabled(): isTextInputEnabled = " + isTextInputEnabled);
        editTextInput.setEnabled(isTextInputEnabled);
    }

    private void disableUiButtons() {
        findViewById(R.id.start_advertising_button).setEnabled(false);
        findViewById(R.id.stop_advertising_button).setEnabled(false);
    }
}