package piuk.blockchain.android.ui.settings.preferences

import android.content.Context
import androidx.core.content.ContextCompat
import android.util.AttributeSet
import android.widget.TextView
import piuk.blockchain.android.R

class ThePitStatusPreference @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = R.attr.preferenceStyle,
    defStyleRes: Int = 0
) : BaseStatusPreference<Boolean>(context, attrs, defStyleAttr, defStyleRes) {

    override val defaultValue = false

    override fun doUpdateValue(value: Boolean, view: TextView) {
        view.apply {
            text = when (value) {
                false -> context.getString(R.string.the_exchange_setting_connect)
                true -> context.getString(R.string.the_exchange_setting_connected)
            }

            val background = when (value) {
                false -> R.drawable.pref_status_bkgrd_blue
                true -> R.drawable.pref_status_bkgrd_green
            }

            setBackgroundResource(background)
            setTextColor(ContextCompat.getColor(context, R.color.white))
        }
    }
}
