package piuk.blockchain.android.ui.launcher

import android.net.Uri
import piuk.blockchain.androidcore.utils.PersistentPrefs
import timber.log.Timber

private const val KEY_DEEP_LINK_URI = "deeplink_uri"

class DeepLinkPersistence(private val prefs: PersistentPrefs) {

    fun pushDeepLink(data: Uri?) {
        Timber.d("DeepLink: Saving uri: $data")
        prefs.setValue(KEY_DEEP_LINK_URI, data.toString())
    }

    fun popUriFromSharedPrefs(): Uri? {
        val uri = prefs.getValue(KEY_DEEP_LINK_URI)?.let { Uri.parse(it) }
        Timber.d("DeepLink: Read uri: $uri")
        prefs.removeValue(KEY_DEEP_LINK_URI)
        return uri
    }
}
