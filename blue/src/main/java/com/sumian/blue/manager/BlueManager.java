package com.sumian.blue.manager;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;

import com.sumian.blue.callback.BlueAdapterCallback;
import com.sumian.blue.callback.BlueScanCallback;
import com.sumian.blue.model.BluePeripheral;
import com.sumian.blue.util.BlueUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by jzz
 * on 2017/11/19.
 * <p>
 * desc:
 */

@SuppressWarnings({"deprecation", "ResultOfMethodCallIgnored"})
public class BlueManager implements BlueContract.Presenter, BluetoothAdapter.LeScanCallback, Handler.Callback {

    private static final String TAG = BlueManager.class.getSimpleName();

    private static final int MSG_WHAT_START_SCAN = 0x01;
    private static final int MSG_WHAT_START_SCAN_AND_DELAY_STOP = 0x0f;
    private static final int MSG_WHAT_STOP_SCAN = 0x011;

    private static final int STOP_SCAN_DELAY_MILLIS = 5 * 1000;

    private static volatile BlueManager INSTANCE = null;

    private BluetoothAdapter mBluetoothAdapter;

    private HandlerThread mHandlerThread;

    private Handler mWorkHandler;

    private Handler mMainHandler = new Handler(Looper.getMainLooper());

    private boolean mIsScanning;
    private volatile List<BlueAdapterCallback> mBlueAdapterCallbacks = new ArrayList<>(0);
    private volatile List<BlueScanCallback> mBlueScanCallbacks = new ArrayList<>(0);

    private BluePeripheral mBluePeripheral;

    private BlueManager() {
        mHandlerThread = new HandlerThread("blueManagerThread") {
            @Override
            protected void onLooperPrepared() {
                super.onLooperPrepared();
                Log.e(TAG, "onLooperPrepared: --------blueManager looper------>");
                mWorkHandler = new Handler(mHandlerThread.getLooper(), BlueManager.this);
            }
        };
        mHandlerThread.start();
    }

    public static BlueManager init() {
        if (INSTANCE == null) {
            synchronized (BlueManager.class) {
                if (INSTANCE == null) {
                    INSTANCE = new BlueManager();
                }
            }
        }
        return INSTANCE;
    }

    public void with(Context context) {
        registerBluetoothReceiver(context);
        BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        this.mBluetoothAdapter = bluetoothManager != null ? bluetoothManager.getAdapter() : null;
    }

    @Override
    public void doScan() {
        removeMsg();
        mWorkHandler.sendEmptyMessage(MSG_WHAT_START_SCAN);
    }

    @Override
    public void doScanDelay() {
        removeMsg();
        mWorkHandler.sendEmptyMessage(MSG_WHAT_START_SCAN_AND_DELAY_STOP);
    }

    @Override
    public void doStopScan() {
        removeMsg();
        mBluetoothAdapter.stopLeScan(this);
        mIsScanning = false;
        for (BlueScanCallback blueScanCallback : mBlueScanCallbacks) {
            runOnUiThread(blueScanCallback::onFinishScanCallback);
        }
    }

    @Override
    public BluetoothAdapter getBluetoothAdapter() {
        return mBluetoothAdapter;
    }

    @Override
    public void enable() {
        if (!isEnable()) {
            mBluetoothAdapter.enable();
        }
    }

    @Override
    public void disable() {
        removeMsg();
        mIsScanning = false;
        mBluetoothAdapter.disable();
    }

    @Override
    public void refresh() {
        if (mBluePeripheral != null) {
            BlueUtil.refresh(mBluePeripheral.getGatt());
        }
    }

    @Override
    public boolean isEnable() {
        return mBluetoothAdapter.isEnabled();
    }

    @Override
    public boolean isLeScanning() {
        return mIsScanning;
    }

    @Override
    public void saveBluePeripheral(BluePeripheral bluePeripheral) {
        this.mBluePeripheral = bluePeripheral;
        refresh();
    }

    @Override
    public BluePeripheral getBluePeripheral() {
        return mBluePeripheral;
    }

    @Override
    public void clearBluePeripheral() {
        if (mBluePeripheral != null) {
            mBluePeripheral.close();
            mBluePeripheral = null;
        }
    }

    @Override
    public void addBlueAdapterCallback(BlueAdapterCallback blueAdapterCallback) {
        synchronized (this) {
            boolean contains = mBlueAdapterCallbacks.contains(blueAdapterCallback);
            if (contains) return;
            mBlueAdapterCallbacks.add(blueAdapterCallback);
        }
    }

    @Override
    public void removeBlueAdapterCallback(BlueAdapterCallback blueAdapterCallback) {
        synchronized (this) {
            if (mBlueAdapterCallbacks == null || mBlueAdapterCallbacks.isEmpty()) return;
            mBlueAdapterCallbacks.remove(blueAdapterCallback);
        }
    }

    @Override
    public void addBlueScanCallback(BlueScanCallback blueScanCallback) {
        synchronized (this) {
            boolean contains = mBlueScanCallbacks.contains(blueScanCallback);
            if (contains) return;
            mBlueScanCallbacks.add(blueScanCallback);
        }
    }

    @Override
    public void removeBlueScanCallback(BlueScanCallback blueScanCallback) {
        synchronized (this) {
            if (mBlueScanCallbacks == null || mBlueScanCallbacks.isEmpty()) return;
            mBlueScanCallbacks.remove(blueScanCallback);
        }
    }

    @Override
    public HandlerThread getWorkThread() {
        return this.mHandlerThread;
    }

    @Override
    public Handler getWorkHandler() {
        return mWorkHandler;
    }

    @Override
    public BluetoothDevice getBluetoothDeviceFromMac(String mac) {
        if (mBluetoothAdapter != null && checkBluetoothAddress(mac)) {
            return mBluetoothAdapter.getRemoteDevice(mac);
        } else {
            return null;
        }
    }

    @Override
    public boolean checkBluetoothAddress(String address) {
        return BluetoothAdapter.checkBluetoothAddress(address);
    }

    @Override
    public void release() {
        removeMsg();
        //if (mBlueReceiver != null) {
        // mContext.unregisterReceiver(mBlueReceiver);
        // mBlueReceiver = null;
        // }
        if (mBluePeripheral != null) {
            mBluePeripheral.close();
        }
        for (int i = 0; i < mBlueAdapterCallbacks.size(); i++) {
            mBlueAdapterCallbacks.remove(i);
        }

        for (int i = 0; i < mBlueScanCallbacks.size(); i++) {
            mBlueScanCallbacks.remove(i);
        }

        //mHandlerThread.quitSafely();
    }

    @Override
    public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
        if (device == null || TextUtils.isEmpty(device.getName()) || !device.getName().startsWith("M-SUMIAN")) {
            return;
        }

        if (mBlueScanCallbacks == null || mBlueScanCallbacks.isEmpty()) return;
        for (int i = 0; i < mBlueScanCallbacks.size(); i++) {
            BlueScanCallback blueScanCallback = mBlueScanCallbacks.get(i);
            runOnUiThread(() -> blueScanCallback.onLeScanCallback(device, rssi, scanRecord));
        }
    }

    private void registerBluetoothReceiver(Context context) {
        BroadcastReceiver blueReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (TextUtils.isEmpty(action)) return;
                switch (action) {
                    case BluetoothAdapter.ACTION_STATE_CHANGED:

                        int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.STATE_OFF);
                        for (BlueAdapterCallback blueAdapterCallback : mBlueAdapterCallbacks) {
                            blueAdapterCallback.onAdapterStateCallback(state);
                        }

                        switch (state) {
                            case BluetoothAdapter.STATE_OFF:
                                Log.e(TAG, "onReceive: ------state off--->");

                                removeMsg();
                                if (mIsScanning) {
                                    doStopScan();
                                }
                                closeBluePeripheral();

                                for (BlueAdapterCallback blueAdapterCallback : mBlueAdapterCallbacks) {
                                    blueAdapterCallback.onAdapterDisable();
                                }

                                break;
                            case BluetoothAdapter.STATE_ON:
                                removeMsg();
                                Log.e(TAG, "onReceive: ------state on--->");

                                for (BlueAdapterCallback blueAdapterCallback : mBlueAdapterCallbacks) {
                                    blueAdapterCallback.onAdapterEnable();
                                }

                                break;
                            default:
                                break;
                        }
                        break;
                    case BluetoothDevice.ACTION_ACL_DISCONNECTED:
                        closeBluePeripheral();
                        break;
                    case BluetoothDevice.ACTION_ACL_CONNECTED:
                        refresh();
                        break;
                    default:
                        break;
                }
            }
        };

        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        filter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        context.registerReceiver(blueReceiver, filter);
    }

    private void closeBluePeripheral() {
        if (mBluePeripheral != null) {
            mBluePeripheral.close();
        }
    }

    private void runOnUiThread(Runnable run) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            run.run();
        } else {
            mMainHandler.post(run);
        }
    }

    private void removeMsg() {
        mWorkHandler.removeMessages(MSG_WHAT_START_SCAN);
        mWorkHandler.removeMessages(MSG_WHAT_START_SCAN_AND_DELAY_STOP);
        mWorkHandler.removeMessages(MSG_WHAT_STOP_SCAN);
    }

    @Override
    public boolean handleMessage(Message msg) {
        switch (msg.what) {
            case MSG_WHAT_START_SCAN:
                startScan();
                break;
            case MSG_WHAT_START_SCAN_AND_DELAY_STOP:
                startScan();
                mWorkHandler.sendEmptyMessageDelayed(MSG_WHAT_STOP_SCAN, STOP_SCAN_DELAY_MILLIS);
                break;
            case MSG_WHAT_STOP_SCAN:
                doStopScan();
                break;
            default:
                break;
        }
        return false;
    }

    private void startScan() {
        if (isEnable()) {
            mIsScanning = mBluetoothAdapter.startLeScan(this);
            if (mIsScanning) {
                for (BlueScanCallback blueScanCallback : mBlueScanCallbacks) {
                    runOnUiThread(blueScanCallback::onBeginScanCallback);
                }
            }
        }
    }
}
