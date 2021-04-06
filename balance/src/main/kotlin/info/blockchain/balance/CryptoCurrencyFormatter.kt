package info.blockchain.balance

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.max

enum class FormatPrecision {
    /**
     * Some currencies will be displayed at a shorter length
     */
    Short,
    /**
     * Full decimal place precision is used for the display string
     */
    Full
}

internal fun CryptoValue.format(
    locale: Locale,
    precision: FormatPrecision = FormatPrecision.Short
): String =
    getFormatter(locale).format(this, precision)

internal fun CryptoValue.formatWithUnit(
    locale: Locale,
    precision: FormatPrecision = FormatPrecision.Short
) =
    getFormatter(locale).formatWithUnit(this, precision)

private val formatterMap: MutableMap<Locale, CryptoCurrencyFormatter> = ConcurrentHashMap()

private fun getFormatter(locale: Locale) =
    formatterMap.getOrPut(locale) { CryptoCurrencyFormatter(locale) }

internal class CryptoCurrencyFormatter(locale: Locale) {
    private val btcFormat = createCryptoDecimalFormat(locale, CryptoCurrency.BTC.dp)
    private val bchFormat = createCryptoDecimalFormat(locale, CryptoCurrency.BCH.dp)
    private val ethFormat = createCryptoDecimalFormat(locale, CryptoCurrency.ETHER.dp)
    private val ethShortFormat = createCryptoDecimalFormat(locale, CryptoCurrency.ETHER.userDp)
    private val xlmFormat = createCryptoDecimalFormat(locale, CryptoCurrency.XLM.dp)
    private val paxShortFormat = createCryptoDecimalFormat(locale, CryptoCurrency.PAX.userDp)
    private val paxFormat = createCryptoDecimalFormat(locale, CryptoCurrency.PAX.dp)
    private val stxFormat = createCryptoDecimalFormat(locale, CryptoCurrency.STX.dp)
    private val algFormat = createCryptoDecimalFormat(locale, CryptoCurrency.ALGO.dp)
    private val usdtFormat = createCryptoDecimalFormat(locale, CryptoCurrency.USDT.dp)
    private val dgldFormat = createCryptoDecimalFormat(locale, CryptoCurrency.DGLD.dp)

    fun format(
        cryptoValue: CryptoValue,
        precision: FormatPrecision = FormatPrecision.Short
    ): String =
        cryptoValue.currency.decimalFormat(precision).formatWithoutUnit(cryptoValue.toBigDecimal())

    fun formatWithUnit(
        cryptoValue: CryptoValue,
        precision: FormatPrecision = FormatPrecision.Short
    ) =
        cryptoValue.currency.decimalFormat(precision).formatWithUnit(
            cryptoValue.toBigDecimal(),
            cryptoValue.currency.displayTicker
        )

    private fun CryptoCurrency.decimalFormat(displayMode: FormatPrecision) = when (this) {
        CryptoCurrency.BTC -> btcFormat
        CryptoCurrency.BCH -> bchFormat
        CryptoCurrency.ETHER -> when (displayMode) {
            FormatPrecision.Short -> ethShortFormat
            FormatPrecision.Full -> ethFormat
        }
        CryptoCurrency.XLM -> xlmFormat
        CryptoCurrency.PAX -> when (displayMode) {
            FormatPrecision.Short -> paxShortFormat
            FormatPrecision.Full -> paxFormat
        }
        CryptoCurrency.STX -> stxFormat
        CryptoCurrency.ALGO -> algFormat
        CryptoCurrency.USDT -> usdtFormat
        CryptoCurrency.DGLD -> dgldFormat
    }

    private fun DecimalFormat.formatWithUnit(value: BigDecimal, symbol: String) =
        "${formatWithoutUnit(value)} $symbol"

    private fun DecimalFormat.formatWithoutUnit(value: BigDecimal) =
        format(value.toPositiveDouble()).toWebZero()
}

private fun BigDecimal.toPositiveDouble() = this.toDouble().toPositiveDouble()

private fun Double.toPositiveDouble() = max(this, 0.0)

/**
 * Replace 0.0 with 0 to match web
 */
private fun String.toWebZero() = if (this == "0.0" || this == "0,0" || this == "0.00") "0" else this

private fun createCryptoDecimalFormat(locale: Locale, maxDigits: Int) =
    (NumberFormat.getInstance(locale) as DecimalFormat).apply {
        minimumFractionDigits = 1
        maximumFractionDigits = maxDigits
        roundingMode = RoundingMode.DOWN
    }
