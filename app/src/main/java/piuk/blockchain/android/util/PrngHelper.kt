package piuk.blockchain.android.util

import android.content.Context
import piuk.blockchain.android.R
import piuk.blockchain.androidcore.data.access.AccessState
import piuk.blockchain.androidcore.utils.PRNGFixes
import piuk.blockchain.androidcore.utils.PrngFixer
import piuk.blockchain.android.ui.customviews.ToastCustom
import piuk.blockchain.android.ui.customviews.toast
import timber.log.Timber
import java.security.Security

class PrngHelper(
    private val context: Context,
    private val accessState: AccessState
) : PrngFixer {

    override fun applyPRNGFixes() {
        try {
            PRNGFixes.apply()
        } catch (recoverable: Exception) {
            //
            // some Android 4.0 devices throw an exception when PRNGFixes is re-applied
            // removing provider before apply() is a workaround
            //
            Security.removeProvider("LinuxPRNG")
            try {
                PRNGFixes.apply()
            } catch (fatal: Exception) {
                Timber.wtf(fatal)
                context.toast(R.string.cannot_launch_app, ToastCustom.TYPE_ERROR)
                accessState.logout()
            }
        }
    }
}
