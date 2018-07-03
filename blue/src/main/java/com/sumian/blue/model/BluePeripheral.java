package com.sumian.blue.model;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.sumian.blue.base.BaseBlueBuilder;
import com.sumian.blue.base.BasePeripheral;
import com.sumian.blue.callback.BlueGattCallback;
import com.sumian.blue.callback.BluePeripheralCallback;
import com.sumian.blue.callback.BluePeripheralDataCallback;
import com.sumian.blue.model.bean.BlueUuidConfig;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Created by jzz
 * on 2017/11/17.
 * <p>
 * desc:
 */

@SuppressWarnings({"NonAtomicOperationOnVolatileField", "ConstantConditions"})
public class BluePeripheral extends BasePeripheral implements Serializable, BlueGattCallback, Handler.Callback {

    private static final String TAG = BluePeripheral.class.getSimpleName();

    private static final int MSG_WHAT_CONNECT = 0x01;
    private static final int MSG_WHAT_COUNTDOWN_TIMER = 0x02;
    private static final int MSG_WHAT_DISCOVER_SERVICE = 0x03;

    private static final int COUNTDOWN_DELAY_MILLIS = 15 * 1000;

    private Context mContext;
    private Handler mWorkHandler;
    private Handler mMainHandler;

    private volatile List<BluePeripheralCallback> mBluePeripheralCallbacks;
    private volatile List<BluePeripheralDataCallback> mBluePeripheralDataCallbacks;

    private volatile int mCount;

    private BluePeripheral(Context context, HandlerThread workThread) {
        this.mContext = context;
        this.mBluePeripheralCallbacks = new ArrayList<>(0);
        this.mBluePeripheralDataCallbacks = new ArrayList<>(0);
        this.mWorkHandler = new Handler(workThread.getLooper());
        this.mMainHandler = new Handler(Looper.getMainLooper(), this);

        // Log.d(TAG, "bindWorkThread: --------->" + (Looper.myLooper() == Looper.getMainLooper()));
    }

    private BluePeripheral(Context context, HandlerThread workThread, String name, BlueUuidConfig blueUuidConfig, BluetoothDevice remoteDevice) {
        this(context, workThread);
        this.mName = name;
        this.mMac = remoteDevice.getAddress();
        this.mBlueUuidConfig = blueUuidConfig;
        this.mRemoteDevice = remoteDevice;
    }

    public String getName() {
        return mName;
    }

    public String getMac() {
        return mMac;
    }

    public String getDfuMac() {
        //CD:9D:C4:08:D8:9D
        String mac = this.mMac;

        String[] split = mac.split(":");

        StringBuilder macSb = new StringBuilder();
        for (String s : split) {
            macSb.append(s);
        }

        //由于 dfu 升级需要设备 mac+1

        //uint64 x old mac;, y new mac;
        // y = (( x & 0xFF ) + 1) + ((x >> 8) << 8);
        long oldMac = Long.parseLong(macSb.toString(), 16);
        long newMac = ((oldMac & 0xff) + 1) + ((oldMac >> 8) << 8);


        macSb.delete(0, macSb.length());

        String hexString = Long.toHexString(newMac);

        for (int i = 0, len = hexString.length(); i < len; i++) {
            if (i % 2 == 0) {
                macSb.append(hexString.substring(i, i + 2));
                if (i != len - 2) {
                    macSb.append(":");
                }
            }
        }

        return macSb.toString().toUpperCase(Locale.getDefault());
    }

    public boolean isConnected() {
        return mConnectedState == BluetoothProfile.STATE_CONNECTED;
    }

    @Override
    public void connect(boolean auto) {
        synchronized (this) {
            removeMsg();
            close();
            for (BluePeripheralCallback peripheralCallback : mBluePeripheralCallbacks) {
                runOnMainThread(() -> peripheralCallback.onConnecting(this, mConnectedState = BluetoothProfile.STATE_CONNECTING));
            }
            mMainHandler.sendEmptyMessageDelayed(MSG_WHAT_CONNECT, 400);
        }
    }

    private void removeMsg() {
        mMainHandler.removeMessages(MSG_WHAT_CONNECT);
        mMainHandler.removeMessages(MSG_WHAT_COUNTDOWN_TIMER);
        mMainHandler.removeMessages(MSG_WHAT_DISCOVER_SERVICE);
    }

    @Override
    public void disconnect() {
        super.disconnect();
        synchronized (this) {
            mCount = 0;
            removeMsg();
            for (BluePeripheralCallback bluePeripheralCallback : mBluePeripheralCallbacks) {
                runOnMainThread(() -> bluePeripheralCallback.onDisconnecting(this, mConnectedState = BluetoothProfile.STATE_DISCONNECTING));
            }
        }
    }

    @Override
    public void writeDelay(byte[] command, long delayMills) {
        runWorkThread(() -> super.write(command), delayMills);
    }

    @Override
    public void addPeripheralCallback(BluePeripheralCallback peripheralCallback) {
        synchronized (this) {
            if (mBluePeripheralCallbacks.contains(peripheralCallback)) return;
            mBluePeripheralCallbacks.add(peripheralCallback);
        }
    }

    @Override
    public void removePeripheralCallback(BluePeripheralCallback peripheralCallback) {
        synchronized (this) {
            if (mBluePeripheralCallbacks.isEmpty()) return;
            mBluePeripheralCallbacks.remove(peripheralCallback);
        }
    }

    @Override
    public void addPeripheralDataCallback(BluePeripheralDataCallback peripheralDataCallback) {
        synchronized (this) {
            if (mBluePeripheralDataCallbacks.contains(peripheralDataCallback)) return;
            mBluePeripheralDataCallbacks.add(peripheralDataCallback);
        }
    }

    @Override
    public void removePeripheralDataCallback(BluePeripheralDataCallback peripheralDataCallback) {
        synchronized (this) {
            if (mBluePeripheralDataCallbacks.isEmpty()) return;
            mBluePeripheralDataCallbacks.remove(peripheralDataCallback);
        }
    }

    @Override
    public void release() {
        removeMsg();
        close();
        this.mBluePeripheralDataCallbacks.clear();
        this.mBluePeripheralCallbacks.clear();
    }

    @Override
    public void onGattConnectSuccess(BluetoothGatt gatt, int connectState) {
        synchronized (this) {
            removeMsg();
            updateConnectState(connectState);
            for (BluePeripheralCallback peripheralCallback : mBluePeripheralCallbacks) {
                runOnMainThread(() -> peripheralCallback.onConnectSuccess(this, connectState));
            }
            //延迟主线程去发现 service
            mMainHandler.sendEmptyMessageDelayed(MSG_WHAT_DISCOVER_SERVICE, 800);
        }
    }

    @Override
    public void onGattConnectFailed(int connectState) {
        removeMsg();
        close();
        updateConnectState(connectState);
        for (BluePeripheralCallback peripheralCallback : mBluePeripheralCallbacks) {
            runOnMainThread(() -> peripheralCallback.onConnectFailed(this, connectState));
        }
    }

    private void onGattConnectTimeOut(int connectState) {
        if (mCount > 5) {
            mCount = 0;
            onGattConnectFailed(connectState);
        } else {
            connect();
        }
    }

    @Override
    public void onGattDisconnectSuccess(int connectState) {
        synchronized (this) {
            removeMsg();
            close();
            updateConnectState(connectState);
            for (BluePeripheralCallback peripheralCallback : mBluePeripheralCallbacks) {
                runOnMainThread(() -> peripheralCallback.onDisconnectSuccess(this, connectState));
            }
        }
    }

    @Override
    public void onPrepareSuccess() {
        for (BluePeripheralCallback peripheralCallback : mBluePeripheralCallbacks) {
            runOnMainThread(() -> peripheralCallback.onTransportChannelReady(BluePeripheral.this));
        }
        mCount = 0;
    }

    @Override
    public void onDiscoverServicesSuccess(BluetoothGatt gatt, BluetoothGattCharacteristic writeCha) {
        removeMsg();
        this.mWriteCha = writeCha;
    }

    @Override
    public void onReceiveData(BluetoothGatt gatt, byte[] data) {
        for (BluePeripheralDataCallback peripheralDataCallback : mBluePeripheralDataCallbacks) {
            runOnMainThread(() -> peripheralDataCallback.onReceiveSuccess(BluePeripheral.this, data));
        }
    }

    @Override
    public void onSendDataSuccess(byte[] data) {
        for (BluePeripheralDataCallback peripheralDataCallback : mBluePeripheralDataCallbacks) {
            runOnMainThread(() -> peripheralDataCallback.onSendSuccess(BluePeripheral.this, data));
        }
    }

    private void runWorkThread(Runnable run, long delayMillis) {
        mWorkHandler.postDelayed(run, delayMillis);
    }

    private void runOnMainThread(Runnable run) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            run.run();
        } else {
            mMainHandler.post(run);
        }
    }

    private void updateConnectState(int connectState) {
        this.mConnectedState = connectState;
    }

    @Override
    public boolean handleMessage(Message msg) {
        switch (msg.what) {
            case MSG_WHAT_COUNTDOWN_TIMER:
                onGattConnectTimeOut(mConnectedState);
                break;
            case MSG_WHAT_CONNECT:
                if (mConnectedState != BluetoothProfile.STATE_CONNECTED) {

                    BlueGattCallbackDelegate blueGattCallbackDelegate = new BlueGattCallbackDelegate().setBlueGattCallback(this).setBlueUuidConfig(mBlueUuidConfig);

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        mGatt = mRemoteDevice.connectGatt(mContext, false, blueGattCallbackDelegate, BluetoothDevice.TRANSPORT_LE);
                    } else {
                        mGatt = mRemoteDevice.connectGatt(mContext, false, blueGattCallbackDelegate);
                    }

                    mCount++;
                    mMainHandler.sendEmptyMessageDelayed(MSG_WHAT_COUNTDOWN_TIMER, COUNTDOWN_DELAY_MILLIS);
                    Log.e(TAG, "connect: ------------->count=" + mCount);
                } else {
                    onGattConnectFailed(mConnectedState);
                }
                break;
            case MSG_WHAT_DISCOVER_SERVICE:
                //1.连接成功,发现服务
                boolean discoverServices = mGatt.discoverServices();
                if (!discoverServices) {
                    runOnMainThread(() -> onGattConnectFailed(mConnectedState = BluetoothProfile.STATE_DISCONNECTED));
                }
                break;
            default:
                break;
        }

        return false;
    }

    public static class PeripheralBlueBuilder implements BaseBlueBuilder<BluePeripheral> {

        private Context mContext;
        private String mName;
        private HandlerThread mWorkThread;
        private BlueUuidConfig mBlueUuidConfig;
        private BluetoothDevice mRemoteDevice;

        public PeripheralBlueBuilder setContext(Context context) {
            mContext = context;
            return this;
        }

        public PeripheralBlueBuilder bindWorkThread(HandlerThread workThread) {
            mWorkThread = workThread;
            return this;
        }

        public PeripheralBlueBuilder setName(String name) {
            mName = name;
            return this;
        }

        public PeripheralBlueBuilder setBlueUuidConfig(BlueUuidConfig blueUuidConfig) {
            mBlueUuidConfig = blueUuidConfig;
            return this;
        }

        public PeripheralBlueBuilder setRemoteDevice(BluetoothDevice remoteDevice) {
            mRemoteDevice = remoteDevice;
            return this;
        }

        @Override
        public BluePeripheral build() {
            return new BluePeripheral(mContext, mWorkThread, mName, mBlueUuidConfig, mRemoteDevice);
        }
    }
}
