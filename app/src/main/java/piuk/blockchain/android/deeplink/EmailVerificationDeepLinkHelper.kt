package piuk.blockchain.android.deeplink

import android.net.Uri
import piuk.blockchain.android.kyc.ignoreFragment

class EmailVerificationDeepLinkHelper {
    fun mapUri(uri: Uri): EmailVerifiedLinkState {
        val uriWithoutFragment = uri.ignoreFragment()
        val name = uriWithoutFragment.getQueryParameter("deep_link_path")
        if (name != "email_verified") {
            return EmailVerifiedLinkState.NoUri
        }

        return when (uriWithoutFragment.getQueryParameter("context")?.toLowerCase()) {
            PIT_SIGNUP -> EmailVerifiedLinkState.FromPitLinking
            else -> EmailVerifiedLinkState.NoUri
        }
    }

    companion object {
        const val PIT_SIGNUP = "pit_signup"
    }
}

enum class EmailVerifiedLinkState {

    /**
     * A deep link into the app when the user has just verified their email due
     * to trying to link their Wallet to the Pit.
     */
    FromPitLinking,

    /**
     * Not a valid email verified deep link URI
     */
    NoUri
}