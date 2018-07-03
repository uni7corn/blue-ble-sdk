package com.sumian.blue.callback;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;

/**
 * Created by jzz
 * on 2017/11/19.
 * <p>
 * desc:
 */

public interface BlueGattCallback {

    void onGattConnectSuccess(BluetoothGatt gatt, int connectState);

    void onGattConnectFailed(int connectState);

    void onGattDisconnectSuccess(int connectState);

    void onPrepareSuccess();

    void onDiscoverServicesSuccess(BluetoothGatt gatt, BluetoothGattCharacteristic writeCha);

    void onReceiveData(BluetoothGatt gatt, byte[] data);

    void onSendDataSuccess(byte[] data);
}
