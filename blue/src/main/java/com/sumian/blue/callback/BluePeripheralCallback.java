package com.sumian.blue.callback;

import com.sumian.blue.model.BluePeripheral;

/**
 * Created by jzz
 * on 2017/11/19.
 * <p>
 * desc:
 */

public interface BluePeripheralCallback {

    void onConnecting(BluePeripheral peripheral, int connectState);

    void onConnectSuccess(BluePeripheral peripheral, int connectState);

    void onConnectFailed(BluePeripheral peripheral, int connectState);

    void onDisconnecting(BluePeripheral peripheral, int connectState);

    void onDisconnectSuccess(BluePeripheral peripheral, int connectState);

    void onTransportChannelReady(BluePeripheral peripheral);
}
