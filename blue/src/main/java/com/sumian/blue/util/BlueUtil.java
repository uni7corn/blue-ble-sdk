package com.sumian.blue.util;

import android.bluetooth.BluetoothGatt;
import android.util.Log;

import com.sumian.blue.BuildConfig;

import java.lang.reflect.Method;

public final class BlueUtil {

    private static final String TAG = BlueUtil.class.getSimpleName();

    @SuppressWarnings("JavaReflectionMemberAccess")
    public static boolean refresh(BluetoothGatt gatt) {
        boolean success = false;
        if (gatt == null) {
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "refresh: refreshDeviceCache gatt is null: " + false);
            }
            return false;
        }
        try {
            Method refresh = BluetoothGatt.class.getMethod("refresh");
            if (refresh != null) {
                refresh.setAccessible(true);
                success = (boolean) refresh.invoke(gatt);
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "refresh  refreshDeviceCache, is success:  " + success);
                }
                return success;
            }
        } catch (Exception e) {
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "exception occur while refreshing device: " + e.getMessage() + "  " + success);
            }
            e.printStackTrace();
        }
        return success;
    }
}
