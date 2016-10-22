package com.example.ricardomartins.ble.BLE;

import android.bluetooth.BluetoothDevice;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


public class LeDeviceListAdapter {

    private static final String TAG = "LeDeviceListAdapter";

    // Adapter for holding devices found through scanning.
    private ArrayList<BluetoothDevice> mLeDevices = new ArrayList<BluetoothDevice>();
    private int[] rssiList = new int[25];

    public void addDevice(BluetoothDevice device, int rssi) {
        if (!mLeDevices.contains(device)) {
            rssiList[getCount()]= rssi;
            mLeDevices.add(device);
            Log.d(TAG, "Added " + device.getName() + " with " + rssi);
        }
    }

    public BluetoothDevice getDevice(int position) {
        return mLeDevices.get(position);
    }

    public int getRSSI(int position){
        return rssiList[position];
    }

    public void clear() {
        mLeDevices.clear();
    }

    public int getCount() {
        return mLeDevices.size();
    }
}
