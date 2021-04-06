package piuk.blockchain.android.ui.thepit

import android.os.Bundle
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.customviews.ErrorBottomDialog

class PitEmailVerifiedBottomDialog : ErrorBottomDialog() {
    override val layout: Int
        get() = R.layout.pit_email_verified_bottom_dialog

    companion object {
        private const val ARG_CONTENT = "arg_content"
        fun newInstance(content: Content): PitEmailVerifiedBottomDialog {
            return PitEmailVerifiedBottomDialog().apply {
                arguments = Bundle().apply {
                    putParcelable(ARG_CONTENT, content)
                }
            }
        }
    }
}