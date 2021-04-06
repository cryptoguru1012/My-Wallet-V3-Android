package piuk.blockchain.android.thepit

import android.net.Uri

class ThePitDeepLinkParser {

    fun mapUri(uri: Uri): String? {
        val fragment = uri.encodedFragment?.let { Uri.parse(it) } ?: return null

        if (fragment.path == "/open/link-account") {
            val linkId = fragment.getQueryParameter("link_id")

            if (!linkId.isNullOrEmpty()) {
                return linkId
            }
        }
        return null
    }
}
