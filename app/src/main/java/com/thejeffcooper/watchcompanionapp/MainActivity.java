package com.thejeffcooper.watchcompanionapp;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.ParcelUuid;
import android.util.Log;
import android.view.View;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private static String CHANNEL_ID = "channel";
    private BluetoothManager bluetoothManager;
    BluetoothAdapter bluetoothAdapter = null;

    private BluetoothLeScanner bluetoothLeScanner;
    private boolean scanning;
    private Handler handler = new Handler();
    private static final long SCAN_PERIOD = 100000;

    ParcelUuid HRM_UUID = new ParcelUuid(UUID.fromString("0000180D-0000-1000-8000-00805f9b34fb"));

    private void createNotificationChannel() {
        CharSequence name = getString(R.string.channel_name);
        String description = getString(R.string.channel_desc);
        int importance = NotificationManager.IMPORTANCE_DEFAULT;
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
        NotificationManager notMan = getSystemService(NotificationManager.class);
        notMan.createNotificationChannel(channel);
    }

    public void onButton(View v) {
        /*
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Notification Title")
                .setSmallIcon(R.drawable.ic_launcher_foreground);
        NotificationManagerCompat notMan = NotificationManagerCompat.from(this);
        notMan.notify(1, builder.build());
        */
        int hasPermission = ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION);
        if (hasPermission != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 0);
        }

        scanLeDevice();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        createNotificationChannel();

        bluetoothManager = getSystemService(BluetoothManager.class);
        if (bluetoothManager != null) {
            bluetoothAdapter = bluetoothManager.getAdapter();
        }
        // Ensures Bluetooth is available on the device and it is enabled. If not,
        // displays a dialog requesting user permission to enable Bluetooth.
        if (bluetoothAdapter != null && !bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, 4);
        }

        bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
    }

    private void scanLeDevice() {
        Log.i("BT", "called");

        if (bluetoothLeScanner != null) {
            if (!scanning) {
                // Stops scanning after a pre-defined scan period.
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        scanning = false;
                        Log.i("BT", "stopped scan");
                        bluetoothLeScanner.stopScan(leScanCallback);
                    }
                }, SCAN_PERIOD);
                Log.i("BT", "started scan");
                scanning = true;
                List<ScanFilter> filters = new ArrayList<ScanFilter>();
                filters.add(new ScanFilter.Builder().setServiceUuid(HRM_UUID).build());
                bluetoothLeScanner.startScan(filters, new ScanSettings.Builder().build(), leScanCallback);
            } else {
                scanning = false;
                bluetoothLeScanner.stopScan(leScanCallback);
            }
        }
    }

    private ScanCallback leScanCallback =
            new ScanCallback() {
                @Override
                public void onScanResult(int callbackType, ScanResult result) {
                    super.onScanResult(callbackType, result);
                    BluetoothDevice dev = result.getDevice();
                    if (dev.getName() == null) {
                        //Log.i("DEVICE_FOUND", dev.getAddress());
                    } else {
                        Log.i("DEVICE_FOUND", dev.getName());
                        ScanRecord rec = result.getScanRecord();
                        for (int i = 0; i < rec.getManufacturerSpecificData().size(); i++) {
                            int key = rec.getManufacturerSpecificData().keyAt(i);
                            byte[] val = rec.getManufacturerSpecificData().valueAt(key);
                            Log.i("DEVICE_DATA", String.format("0x%x\t'%s'", key, new String(val)));
                        }

                    }
                }
            };

}