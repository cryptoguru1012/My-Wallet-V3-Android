package piuk.blockchain.android.ui.kyc.navhost

import androidx.appcompat.app.AppCompatActivity
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AlertDialog
import android.view.View
import piuk.blockchain.android.R

internal fun showHelpDialog(activity: AppCompatActivity) {
    AlertDialog.Builder(activity, R.style.AlertDialogStyle)
        .setView(View.inflate(activity, R.layout.dialog_help, null))
        .setNeutralButton(R.string.kyc_help_dialog_read_now) { _, _ ->
            activity.startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse(activity.getString(R.string.kyc_help_dialog_read_now_url))
                )
            )
        }
        .setPositiveButton(R.string.kyc_help_dialog_contact_support) { _, _ ->
            activity.startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse(activity.getString(R.string.kyc_help_dialog_contact_support_url))
                )
            )
        }
        .show()
}
