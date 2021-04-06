package piuk.blockchain.androidcore.utils

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.provider.Settings
import com.blockchain.notifications.analytics.Analytics
import com.blockchain.notifications.analytics.AnalyticsEvent
import com.blockchain.utils.toHex
import timber.log.Timber
import java.net.NetworkInterface
import java.security.MessageDigest
import java.util.UUID

interface DeviceIdGenerator {
    fun generateId(): String
}

internal class DeviceIdGeneratorImpl(
    private val ctx: Context,
    private val analytics: Analytics
) : DeviceIdGenerator {

    @SuppressLint("HardwareIds")
    override fun generateId(): String {

        // In most cases, the android_id should do what we want. However, sometimes this has been
        // known to return null and some devices have a bug where it returns the emulator id.
        // In those cases, we'll generate an id based on the wifi interface. IF we can't find
        // a wifi interface then... um... all bets are off - without expanding the app
        // permission set, there's not so many options. So then we'll just generate a UUID and
        // have to live with it's failure to persist across app installs.
        // We'll also track these and see if it actually comes up, in which case we can re-think.

        var id = Settings.Secure.getString(ctx.contentResolver, Settings.Secure.ANDROID_ID)
        if ((id == null) || (id == BUGGY_ANDROID_ID)) {
            id = generateWifiMacId()
            if (id == null) {
                id = UUID.randomUUID().toString()
                analytics.logEvent(AnalyticsGenEvent(SOURCE_UUID_GEN))
            } else {
                analytics.logEvent(AnalyticsGenEvent(SOURCE_MAC_ADDRESS))
            }
        } else {
            analytics.logEvent(AnalyticsGenEvent(SOURCE_ANDROID_ID))
        }
        return id
    }

    private fun generateWifiMacId(): String? {
        try {
            val hwIf = NetworkInterface.getNetworkInterfaces()
                .toList()
                .firstOrNull { it.displayName == "wlan0" }

                if (hwIf != null && hwIf.hardwareAddress != null) {
                    val md = MessageDigest.getInstance("SHA-1")
                    md.update(hwIf.hardwareAddress)
                    md.update(Build.MANUFACTURER.toByteArray())
                    md.update(Build.MODEL.toByteArray())
                    md.update(Build.DEVICE.toByteArray())
                    return md.digest().toHex()
                }
            } catch (e: Throwable) {
                Timber.d("Unable to generate mac based device id")
            }
        return null
    }

    private class AnalyticsGenEvent(val source: String) : AnalyticsEvent {
        override val event: String
            get() = EVENT_NAME

        override val params: Map<String, String>
            get() = mapOf(ANALYTICS_PARAM to source)
    }

    companion object {
        const val EVENT_NAME = "generateId"
        const val ANALYTICS_PARAM = "source"
        const val SOURCE_ANDROID_ID = "android_id"
        const val SOURCE_MAC_ADDRESS = "wifi_mac"
        const val SOURCE_UUID_GEN = "uuid_gen"

        const val BUGGY_ANDROID_ID = "9774d56d682e549c"
    }
}