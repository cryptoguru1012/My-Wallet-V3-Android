package piuk.blockchain.android.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AlertDialog
import piuk.blockchain.android.R

fun calloutToExternalSupportLinkDlg(ctx: Context, supportUrl: String) {

    AlertDialog.Builder(ctx, R.style.AlertDialogStyle)
        .setTitle(R.string.app_name)
        .setMessage(R.string.support_leaving_app_warning)
        .setPositiveButton(android.R.string.ok) { _, _ ->
            ctx.startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse(supportUrl)
                )
            )
        }
        .setNegativeButton(android.R.string.cancel, null)
        .show()
}

fun Context.launchUrlInBrowser(uri: String) {
    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(uri)))
}