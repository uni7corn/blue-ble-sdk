package com.sumian.blue.callback;

import com.sumian.blue.model.BluePeripheral;

/**
 * Created by jzz
 * on 2017/12/3.
 * <p>
 * desc:
 */

public interface BluePeripheralDataCallback {

    void onSendSuccess(BluePeripheral bluePeripheral, byte[] data);

    void onReceiveSuccess(BluePeripheral bluePeripheral, byte[] data);

}
