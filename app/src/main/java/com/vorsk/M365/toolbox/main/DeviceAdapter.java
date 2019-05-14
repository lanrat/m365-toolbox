package com.vorsk.M365.toolbox.main;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

import com.vorsk.M365.toolbox.util.ScannedDevice;

public class DeviceAdapter extends ArrayAdapter<ScannedDevice> {
    private List<ScannedDevice> mList;
    private LayoutInflater mInflater;
    private int mResId;

    DeviceAdapter(Context context, int resId, List<ScannedDevice> objects) {
        super(context, resId, objects);
        mResId = resId;
        mList = objects;
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        ScannedDevice item = getItem(position);

        if (convertView == null) {
            convertView = mInflater.inflate(mResId, null);
        }
        TextView name = convertView.findViewById(R.id.device_name);
        assert item != null;
        name.setText(item.getDisplayName());
        TextView address = convertView.findViewById(R.id.device_address);
        address.setText(item.getDevice().getAddress());
        TextView rssi = convertView.findViewById(R.id.device_rssi);
        rssi.setText(String.format("RSSI:%s", Integer.toString(item.getRssi())));

        return convertView;
    }

    /**
     * add or update BluetoothDevice
     */
    public void update(BluetoothDevice newDevice, int rssi, byte[] scanRecord) {
        if ((newDevice == null) || (newDevice.getAddress() == null)) {
            return;
        }

        boolean contains = false;
        for (ScannedDevice device : mList) {
            if (newDevice.getAddress().equals(device.getDevice().getAddress())) {
                contains = true;
                device.setRssi(rssi); // update
                break;
            }
        }
        if (!contains) {
            // add new BluetoothDevice
            mList.add(new ScannedDevice(newDevice, rssi));
        }
        notifyDataSetChanged();
    }
}
