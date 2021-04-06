package info.blockchain.balance

import org.amshove.kluent.`should be`
import org.amshove.kluent.`should equal`
import org.amshove.kluent.`should not equal`
import org.amshove.kluent.`should throw the Exception`
import org.amshove.kluent.`with message`
import org.junit.Test
import java.util.Locale

class FiatValueTests {

    @Test
    fun `can create and read properties`() {
        FiatValue.fromMajor("USD", 123.45.toBigDecimal())
            .apply {
                currencyCode `should equal` "USD"
                toBigDecimal() `should equal` 123.45.toBigDecimal()
            }
    }

    @Test
    fun `can create and read alternative properties`() {
        FiatValue.fromMajor("GBP", 99.99.toBigDecimal())
            .apply {
                currencyCode `should equal` "GBP"
                toBigDecimal() `should equal` 99.99.toBigDecimal()
            }
    }

    @Test
    fun `can equate`() {
        FiatValue.fromMajor("CAD", 123.toBigDecimal()) `should equal` FiatValue.fromMajor("CAD", 123.toBigDecimal())
    }

    @Test
    fun `can not equate by symbol`() {
        FiatValue.fromMajor("CAD", 123.toBigDecimal()) `should not equal` FiatValue.fromMajor("USD", 123.toBigDecimal())
    }

    @Test
    fun `can not equate by value`() {
        FiatValue.fromMajor("CAD", 123.toBigDecimal()) `should not equal` FiatValue.fromMajor("CAD", 456.toBigDecimal())
    }

    @Test
    fun `can format GBP in UK`() {
        Locale.setDefault(Locale.UK)
        FiatValue.fromMajor("GBP", 99.99.toBigDecimal())
            .toStringWithSymbol() `should equal` "£99.99"
    }

    @Test
    fun `can format GBP in UK - trailing digits`() {
        Locale.setDefault(Locale.UK)
        FiatValue.fromMajor("GBP", 99.toBigDecimal())
            .toStringWithSymbol() `should equal` "£99.00"
    }

    @Test
    fun `can format GBP in UK - maximum 2 digits`() {
        Locale.setDefault(Locale.UK)
        FiatValue.fromMajor("GBP", 99.123.toBigDecimal())
            .toStringWithSymbol() `should equal` "£99.12"
    }

    @Test
    fun `can format USD in US`() {
        Locale.setDefault(Locale.US)
        FiatValue.fromMajor("USD", 1.23.toBigDecimal())
            .toStringWithSymbol() `should equal` "$1.23"
    }

    @Test
    fun `can format USD in Canada`() {
        Locale.setDefault(Locale.CANADA)
        FiatValue.fromMajor("USD", 3.45.toBigDecimal())
            .toStringWithSymbol() `should equal` "US$3.45"
    }

    @Test
    fun `can format CAD in US`() {
        Locale.setDefault(Locale.US)
        FiatValue.fromMajor("CAD", 4.56.toBigDecimal())
            .toStringWithSymbol() `should equal` "CAD4.56"
    }

    @Test
    fun `can format CAD in Canada`() {
        Locale.setDefault(Locale.CANADA)
        FiatValue.fromMajor("CAD", 6.78.toBigDecimal())
            .toStringWithSymbol() `should equal` "$6.78"
    }

    @Test
    fun `can format YEN in US`() {
        Locale.setDefault(Locale.US)
        FiatValue.fromMajor("JPY", 456.toBigDecimal())
            .toStringWithSymbol() `should equal` "JPY456"
    }

    @Test
    fun `can format YEN in Japan`() {
        Locale.setDefault(Locale.JAPAN)
        FiatValue.fromMajor("JPY", 678.toBigDecimal())
            .toStringWithSymbol() `should equal` "￥678"
    }

    @Test
    fun `can format YEN in Japan - maximum 0 digits`() {
        Locale.setDefault(Locale.JAPAN)
        FiatValue.fromMajor("JPY", 99.123.toBigDecimal())
            .toStringWithSymbol() `should equal` "￥99"
    }

    @Test
    fun `can format GBP without symbol`() {
        Locale.setDefault(Locale.UK)
        FiatValue.fromMajor("GBP", 1.1.toBigDecimal())
            .toStringWithoutSymbol() `should equal` "1.10"
    }

    @Test
    fun `can format YEN without symbol`() {
        Locale.setDefault(Locale.JAPAN)
        FiatValue.fromMajor("JPY", 678.toBigDecimal())
            .toStringWithoutSymbol() `should equal` "678"
    }

    @Test
    fun `can format USD without symbol in ES`() {
        Locale.setDefault(Locale("es_ES"))
        FiatValue.fromMajor("USD", 0.07.toBigDecimal())
            .toStringWithoutSymbol() `should equal` "0.07"
    }

    @Test
    fun `is Zero`() {
        FiatValue.fromMajor("GBP", 0.toBigDecimal()).isZero `should be` true
    }

    @Test
    fun `isZero with decimal places`() {
        FiatValue.fromMajor("GBP", 0.0.toBigDecimal()).isZero `should be` true
    }

    @Test
    fun `isZero negative`() {
        FiatValue.fromMajor("GBP", (-1).toBigDecimal()).isZero `should be` false
    }

    @Test
    fun `isZero positive`() {
        FiatValue.fromMajor("GBP", 0.1.toBigDecimal()).isZero `should be` false
    }

    @Test
    fun `zero is not Positive`() {
        FiatValue.fromMajor("GBP", 0.toBigDecimal()).isPositive `should be` false
    }

    @Test
    fun `about zero is positive`() {
        FiatValue.fromMajor("GBP", 0.1.toBigDecimal()).isPositive `should be` true
    }

    @Test
    fun `below zero is not positive`() {
        FiatValue.fromMajor("GBP", (-1).toBigDecimal()).isZero `should be` false
    }

    @Test
    fun `can add`() {
        1.2.gbp() + 2.3.gbp() `should equal` 3.5.gbp()
    }

    @Test
    fun `can add with alternative currency and values`() {
        10.usd() + 20.usd() `should equal` 30.usd()
    }

    @Test
    fun `can't add if the currency codes don't match`() {
        {
            1.2.gbp() + 2.3.usd()
        } `should throw the Exception` ValueTypeMismatchException::class `with message` "Can't add GBP and USD"
    }
}
