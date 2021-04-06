package piuk.blockchain.android.data.connectivity;

import android.content.Context;

import piuk.blockchain.androidcore.data.rxjava.RxBus;

public enum ConnectivityManager {

    INSTANCE;

    ConnectivityManager() {
        // No-op
    }

    public static ConnectivityManager getInstance() {
        return INSTANCE;
    }

    /**
     * Listens for network connection events using whatever is best practice for the current API level,
     * ie a {@link android.content.BroadcastReceiver} for pre-21, and the {@link
     * android.net.ConnectivityManager.NetworkCallback} for Lollipop and above
     */
    public void registerNetworkListener(Context context, RxBus rxBus) {
        new ConnectionStateMonitor(context, rxBus).enable();
    }
}
