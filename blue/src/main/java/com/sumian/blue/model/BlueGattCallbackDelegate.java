package com.sumian.blue.model;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;

import com.sumian.blue.callback.BlueGattCallback;
import com.sumian.blue.model.bean.BlueUuidConfig;

/**
 * Created by jzz
 * on 2017/11/17.
 * <p>
 * desc:
 */

public class BlueGattCallbackDelegate extends BluetoothGattCallback {

    private static final String TAG = BlueGattCallbackDelegate.class.getSimpleName();

    private BlueGattCallback mBlueGattCallback;
    private BlueUuidConfig mBlueUuidConfig;

    BlueGattCallbackDelegate setBlueGattCallback(BlueGattCallback blueGattCallback) {
        mBlueGattCallback = blueGattCallback;
        return this;
    }

    BlueGattCallbackDelegate setBlueUuidConfig(BlueUuidConfig blueUuidConfig) {
        mBlueUuidConfig = blueUuidConfig;
        return this;
    }

    @Override
    public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
        super.onConnectionStateChange(gatt, status, newState);
        switch (newState) {
            case BluetoothProfile.STATE_CONNECTED://gatt 协议连接成功
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    this.mBlueGattCallback.onGattConnectSuccess(gatt, newState);
                } else {
                    this.mBlueGattCallback.onGattConnectFailed(BluetoothProfile.STATE_DISCONNECTED);
                }
                break;
            case BluetoothProfile.STATE_DISCONNECTED:// gatt 协议断开成功
                switch (status) {
                    case BluetoothGatt.GATT_SUCCESS://gatt 从连接成功状态,变为断开状态
                        this.mBlueGattCallback.onGattDisconnectSuccess(newState);
                        break;
                    case 8://异常断开码,包含无法连接,意外断开,连接失败,gatt 协议繁忙等状态码
                    case 22:
                    case 59:
                    case 133:
                    case BluetoothGatt.GATT_CONNECTION_CONGESTED:
                    default:
                        this.mBlueGattCallback.onGattConnectFailed(BluetoothProfile.STATE_DISCONNECTED);
                        break;
                }
                break;
            default://连接失败
                this.mBlueGattCallback.onGattConnectFailed(newState);
                break;
        }
    }

    @Override
    public void onServicesDiscovered(BluetoothGatt gatt, int status) {
        super.onServicesDiscovered(gatt, status);
        if (status == BluetoothGatt.GATT_SUCCESS) {
            BlueUuidConfig blueUuidConfig = this.mBlueUuidConfig;
            BluetoothGattService gattService = gatt.getService(blueUuidConfig.serviceUuid);
            if (gattService == null) {
                this.mBlueGattCallback.onGattConnectFailed(BluetoothProfile.STATE_DISCONNECTED);
                return;
            }

            BluetoothGattCharacteristic receiveCha = gattService.getCharacteristic(blueUuidConfig.notifyUuid);
            if (receiveCha == null) {
                this.mBlueGattCallback.onGattConnectFailed(BluetoothProfile.STATE_DISCONNECTED);
                return;
            }

            BluetoothGattDescriptor receiveChaDescriptor = receiveCha.getDescriptor(blueUuidConfig.descUuid);
            gatt.setCharacteristicNotification(receiveCha, true);
            receiveChaDescriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            gatt.writeDescriptor(receiveChaDescriptor);

            BluetoothGattCharacteristic writeCha = gattService.getCharacteristic(blueUuidConfig.writeUuid);

            this.mBlueGattCallback.onDiscoverServicesSuccess(gatt, writeCha);
        } else {
            this.mBlueGattCallback.onGattConnectFailed(BluetoothProfile.STATE_DISCONNECTED);
        }
    }

    @Override
    public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        super.onCharacteristicWrite(gatt, characteristic, status);
        if (status == BluetoothGatt.GATT_SUCCESS) {
            this.mBlueGattCallback.onSendDataSuccess(characteristic.getValue());
        } else {
            this.mBlueGattCallback.onGattConnectFailed(BluetoothProfile.STATE_DISCONNECTED);
        }
    }

    @Override
    public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        super.onCharacteristicChanged(gatt, characteristic);
        this.mBlueGattCallback.onReceiveData(gatt, characteristic.getValue());
    }

    @Override
    public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
        super.onDescriptorWrite(gatt, descriptor, status);
        if (status == BluetoothGatt.GATT_SUCCESS) {
            this.mBlueGattCallback.onPrepareSuccess();
        } else {
            this.mBlueGattCallback.onGattConnectFailed(BluetoothProfile.STATE_DISCONNECTED);
        }
    }

    @Override
    public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
        super.onReadRemoteRssi(gatt, rssi, status);
        if (status != BluetoothGatt.GATT_SUCCESS) {
            this.mBlueGattCallback.onGattConnectFailed(BluetoothProfile.STATE_DISCONNECTED);
        }
    }
}
