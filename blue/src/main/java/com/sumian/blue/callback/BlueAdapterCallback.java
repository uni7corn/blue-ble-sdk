package com.sumian.blue.callback;

/**
 * Created by jzz
 * on 2017/11/20.
 * <p>
 * desc:
 */

public interface BlueAdapterCallback {

    void onAdapterEnable();

    void onAdapterDisable();

    default void onAdapterStateCallback(int state) {
    }

}
