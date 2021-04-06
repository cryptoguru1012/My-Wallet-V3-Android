package piuk.blockchain.android.ui.home

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.blockchain.annotations.CommonCode
import kotlinx.android.synthetic.main.mobile_notice_dialog.*
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.auth.MobileNoticeDialog

@CommonCode("One of many almost identical bottom dialogs")
class MobileNoticeDialogFragment : BottomSheetDialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val contextThemeWrapper = ContextThemeWrapper(activity, R.style.AppTheme)
        val themedInflater = inflater.cloneInContext(contextThemeWrapper)
        return themedInflater.inflate(R.layout.mobile_notice_dialog, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dialog_title.text = arguments?.getString(KEY_TITLE)
        dialog_body.text = arguments?.getString(KEY_MESSAGE)
        button_cta.text = arguments?.getString(KEY_CTA_BUTTON_TEXT)
        button_cta.setOnClickListener {
            arguments?.getString(KEY_CTA_LINK)?.let {
                context?.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(it)))
            }
            dismiss()
        }
    }

    companion object {
        val TAG = MobileNoticeDialogFragment::class.java.simpleName!!

        private const val KEY_TITLE = "title"
        private const val KEY_MESSAGE = "message"
        private const val KEY_CTA_BUTTON_TEXT = "cta_button_text"
        private const val KEY_CTA_LINK = "cta_link"

        fun newInstance(
            mobileNoticeDialog: MobileNoticeDialog
        ): MobileNoticeDialogFragment {
            val fragment = MobileNoticeDialogFragment()

            fragment.arguments = Bundle().apply {
                putString(KEY_TITLE, mobileNoticeDialog.title)
                putString(KEY_MESSAGE, mobileNoticeDialog.body)
                putString(KEY_CTA_BUTTON_TEXT, mobileNoticeDialog.ctaText)
                putString(KEY_CTA_LINK, mobileNoticeDialog.ctaLink)
            }
            return fragment
        }
    }
}
