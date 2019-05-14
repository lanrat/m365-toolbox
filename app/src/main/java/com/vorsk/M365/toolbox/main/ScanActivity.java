package com.vorsk.M365.toolbox.main;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;

import com.vorsk.M365.toolbox.util.BleUtil;
import com.vorsk.M365.toolbox.util.ScannedDevice;
import permissions.dispatcher.NeedsPermission;
import permissions.dispatcher.RuntimePermissions;

@RuntimePermissions
public class ScanActivity extends Activity implements BluetoothAdapter.LeScanCallback {
    private BluetoothAdapter mBTAdapter;
    private DeviceAdapter mDeviceAdapter;
    private boolean mIsScanning;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.activity_scan);

        init();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        stopScan();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (mIsScanning) {
            menu.findItem(R.id.action_scan).setVisible(false);
            menu.findItem(R.id.action_stop).setVisible(true);
        } else {
            menu.findItem(R.id.action_scan).setVisible(true);
            menu.findItem(R.id.action_stop).setVisible(false);
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == android.R.id.home) {
            // ignore
            return true;
        } else if (itemId == R.id.action_scan) {
            ScanActivityPermissionsDispatcher.startScanWithPermissionCheck(this);
            return true;
        } else if (itemId == R.id.action_stop) {
            stopScan();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onLeScan(final BluetoothDevice newDeivce, final int newRssi,
                         final byte[] newScanRecord) {
        runOnUiThread(() -> mDeviceAdapter.update(newDeivce, newRssi, newScanRecord));
    }

    private void init() {
        // BLE check
        if (!BleUtil.isBLESupported(this)) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // BT check
        BluetoothManager manager = BleUtil.getManager(this);
        if (manager != null) {
            mBTAdapter = manager.getAdapter();
        }
        if (mBTAdapter == null) {
            Toast.makeText(this, R.string.bt_unavailable, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        if (!mBTAdapter.isEnabled()) {
            Toast.makeText(this, R.string.bt_disabled, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // init listview
        ListView deviceListView = findViewById(R.id.list);
        mDeviceAdapter = new DeviceAdapter(this, R.layout.listitem_device,
                new ArrayList<>());
        deviceListView.setAdapter(mDeviceAdapter);
        deviceListView.setOnItemClickListener((adapterview, view, position, id) -> {
            ScannedDevice item = mDeviceAdapter.getItem(position);
            if (item != null) {
                Intent intent = new Intent(view.getContext(), DeviceActivity.class);
                BluetoothDevice selectedDevice = item.getDevice();
                //intent.putExtra(DeviceActivity.EXTRA_BLUETOOTH_DEVICE, selectedDevice);
                intent.putExtra(Scooter.EXTRAS_DEVICE_NAME, selectedDevice.getName());
                intent.putExtra(Scooter.EXTRAS_DEVICE_ADDRESS, selectedDevice.getAddress());
                startActivity(intent);

                // stop before change Activity
                stopScan();
            }
        });

        stopScan();
    }

    @NeedsPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
    void startScan() {
        if ((mBTAdapter != null) && (!mIsScanning)) {
            // clear listview
            ListView deviceListView = findViewById(R.id.list);
            deviceListView.deferNotifyDataSetChanged();
            mDeviceAdapter.clear();

            // start scan
            mBTAdapter.startLeScan(this);
            mIsScanning = true;
            setProgressBarIndeterminateVisibility(true);
            invalidateOptionsMenu();
        }
    }

    private void stopScan() {
        if (mBTAdapter != null) {
            mBTAdapter.stopLeScan(this);
        }
        mIsScanning = false;
        setProgressBarIndeterminateVisibility(false);
        invalidateOptionsMenu();
    }
}
