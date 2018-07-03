package com.sumian.blue.callback;

import android.bluetooth.BluetoothDevice;

/**
 * Created by sm
 * on 2018/3/22.
 * desc:
 */

public interface BlueScanCallback {

    default void onBeginScanCallback() {
    }

    void onLeScanCallback(BluetoothDevice device, int rssi, byte[] scanRecord);

    default void onFinishScanCallback() {
    }
}
