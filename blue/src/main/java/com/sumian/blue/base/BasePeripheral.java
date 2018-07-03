package com.sumian.blue.base;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothProfile;
import android.util.Log;

import com.sumian.blue.callback.BluePeripheralCallback;
import com.sumian.blue.callback.BluePeripheralDataCallback;
import com.sumian.blue.model.bean.BlueUuidConfig;

import java.lang.reflect.Method;

/**
 * Created by jzz
 * on 2017/11/17.
 * <p>
 * desc:
 */

public abstract class BasePeripheral {

    private static final String TAG = BasePeripheral.class.getSimpleName();

    protected String mName;
    protected String mMac;
    protected BlueUuidConfig mBlueUuidConfig;
    protected BluetoothGatt mGatt;

    protected BluetoothDevice mRemoteDevice;

    // private BluetoothGattCharacteristic mReceiveCha;
    protected BluetoothGattCharacteristic mWriteCha;

    protected int mConnectedState = BluetoothProfile.STATE_DISCONNECTED;//直接对应 Android 系统 gatt 协议的状态,切勿自己定义连接状态

    public void connect() {
        connect(false);
    }

    public abstract void connect(boolean auto);

    public void disconnect() {
        synchronized (this) {
            if (mGatt != null) {
                mGatt.abortReliableWrite();
                mGatt.disconnect();
                refresh();
                mConnectedState = BluetoothProfile.STATE_DISCONNECTED;
                mWriteCha = null;
            }
        }
    }

    public void close() {
        if (mGatt != null) {
            disconnect();
            mGatt.close();
        }
    }

    @SuppressWarnings("JavaReflectionMemberAccess")
    public boolean refresh() {
        boolean success = false;
        try {
            Method refresh = BluetoothGatt.class.getMethod("refresh");
            if (refresh != null) {
                BluetoothGatt gatt = mGatt;
                if (gatt != null) {
                    refresh.setAccessible(true);
                    success = (boolean) refresh.invoke(gatt);
                    Log.e(TAG, "refresh  refreshDeviceCache, is success:  " + success);
                    return success;
                } else {
                    Log.e(TAG, "refresh: refreshDeviceCache gatt is null: " + false);
                    return false;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "exception occur while refreshing device: " + e.getMessage() + "  " + success);
            e.printStackTrace();
        }
        return success;
    }

    public void write(byte[] command) {
        if (mGatt == null || mWriteCha == null || mConnectedState != BluetoothProfile.STATE_CONNECTED) {
            return;
        }
        mWriteCha.setValue(command);
        mGatt.writeCharacteristic(mWriteCha);
    }

    public void writeDelay(byte[] command, long delayMills) {
        write(command);
    }

    public void read(byte[] command) {
        write(command);
    }

    public BluetoothGatt getGatt() {
        return mGatt;
    }

    public abstract void addPeripheralCallback(BluePeripheralCallback peripheralCallback);

    public abstract void removePeripheralCallback(BluePeripheralCallback peripheralCallback);

    public abstract void addPeripheralDataCallback(BluePeripheralDataCallback peripheralDataCallback);

    public abstract void removePeripheralDataCallback(BluePeripheralDataCallback peripheralDataCallback);

    public abstract void release();

}
