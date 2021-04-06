package piuk.blockchain.android.ui.dashboard.assetdetails

import android.annotation.SuppressLint
import android.content.Context
import android.widget.TextView
import com.github.mikephil.charting.components.MarkerView
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.utils.MPPointF
import piuk.blockchain.android.R
import java.text.NumberFormat
import java.util.Locale

class ValueMarker(
    context: Context,
    layoutResource: Int,
    private val fiatSymbol: String,
    private val decimalPlaces: Int
) : MarkerView(context, layoutResource) {

    private val price = findViewById<TextView>(R.id.price)

    @SuppressLint("SetTextI18n")
    override fun refreshContent(e: Entry, highlight: Highlight) {
        price.text = "$fiatSymbol${NumberFormat.getNumberInstance(Locale.getDefault())
            .apply {
                maximumFractionDigits = decimalPlaces
                minimumFractionDigits = decimalPlaces
            }
            .format(e.y)}"

        super.refreshContent(e, highlight)
    }

    override fun getOffset(): MPPointF {
        return MPPointF((-width * 0.74f), (-height).toFloat())
    }
}