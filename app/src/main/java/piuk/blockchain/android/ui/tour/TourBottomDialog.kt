package piuk.blockchain.android.ui.tour

import android.os.Bundle
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.blockchain.notifications.analytics.Analytics
import kotlinx.android.synthetic.main.dialog_tour_bottom_sheet.*
import org.koin.android.ext.android.inject
import piuk.blockchain.android.R

open class TourBottomDialog : BottomSheetDialogFragment() {

    data class Content(
        @DrawableRes val iconId: Int,
        @StringRes val titleText: Int,
        @StringRes val bodyText: Int,
        @StringRes val btnText: Int,
        val onBtnClick: () -> Unit
    )

    protected val analytics: Analytics by inject()
    protected open val layout = R.layout.dialog_tour_bottom_sheet

    private var content: Content? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val contextThemeWrapper = ContextThemeWrapper(activity, R.style.AppTheme)
        val themedInflater = inflater.cloneInContext(contextThemeWrapper)
        return themedInflater.inflate(layout, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        if (!init()) dismiss()
    }

    protected fun setContent(content: Content) {
        this.content = content
    }

    private fun init(): Boolean {
        val c = content ?: return false

        with(c) {
            icon.setImageResource(iconId)
            txt_title.setText(titleText)
            txt_body.setText(bodyText)
            button.setText(btnText)
            button.setOnClickListener {
                dismiss()
                onBtnClick.invoke()
            }
        }
        return true
    }

    companion object {
        fun newInstance(content: Content): TourBottomDialog {
            return TourBottomDialog().apply {
                isCancelable = false
                setContent(content)
            }
        }
    }
}
