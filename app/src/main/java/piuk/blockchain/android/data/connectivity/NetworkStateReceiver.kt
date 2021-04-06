package piuk.blockchain.android.data.connectivity

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import piuk.blockchain.androidcore.data.events.ActionEvent
import piuk.blockchain.androidcore.data.events.SpottyNetworkConnectionEvent
import piuk.blockchain.androidcore.data.rxjava.RxBus

internal class NetworkStateReceiver(private val rxBus: RxBus) : BroadcastReceiver() {
    private var lastBroadcastTime: Long = 0

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.extras != null) {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val networkInfo = connectivityManager.activeNetworkInfo

            if (networkInfo != null && networkInfo.isConnected) {
                // Sends max of one broadcast every 30s if network connection is spotty
                if (System.currentTimeMillis() - lastBroadcastTime > COOL_DOWN_INTERVAL) {
                    rxBus.emitEvent(ActionEvent::class.java, SpottyNetworkConnectionEvent())
                    lastBroadcastTime = System.currentTimeMillis()
                }
            }
        }
    }

    companion object {

        private const val COOL_DOWN_INTERVAL = 1000 * 30L
    }
}