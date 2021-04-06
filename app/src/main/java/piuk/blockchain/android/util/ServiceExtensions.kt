package piuk.blockchain.android.util

import android.app.Service
import android.content.Context
import android.content.Intent

fun <T : Service> Class<T>.start(context: Context, osUtil: OSUtil) {
    val intent = Intent(context, this)

    if (!osUtil.isServiceRunning(this)) {
        context.applicationContext.startService(intent)
    } else {
        // Restarting this here ensures re-subscription after app restart - the service may remain
        // running, but the subscription to the WebSocket won't be restarted unless onCreate called
        context.applicationContext.stopService(intent)
        context.applicationContext.startService(intent)
    }
}