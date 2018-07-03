package com.sumian.blue.manager;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.os.Handler;
import android.os.HandlerThread;

import com.sumian.blue.callback.BlueAdapterCallback;
import com.sumian.blue.callback.BlueScanCallback;
import com.sumian.blue.model.BluePeripheral;

/**
 * Created by jzz
 * on 2017/11/19.
 * <p>
 * desc:
 */

public interface BlueContract {


    interface Presenter {

        void doScan();

        void doScanDelay();

        void doStopScan();

        BluetoothAdapter getBluetoothAdapter();

        void enable();

        void disable();

        void refresh();

        boolean isEnable();

        boolean isLeScanning();

        void saveBluePeripheral(BluePeripheral bluePeripheral);

        BluePeripheral getBluePeripheral();

        void clearBluePeripheral();

        void addBlueAdapterCallback(BlueAdapterCallback blueAdapterCallback);

        void removeBlueAdapterCallback(BlueAdapterCallback blueAdapterCallback);

        void addBlueScanCallback(BlueScanCallback blueScanCallback);

        void removeBlueScanCallback(BlueScanCallback blueScanCallback);

        HandlerThread getWorkThread();

        Handler getWorkHandler();

        BluetoothDevice getBluetoothDeviceFromMac(String mac);

        boolean checkBluetoothAddress(String address);

        void release();

    }
}
