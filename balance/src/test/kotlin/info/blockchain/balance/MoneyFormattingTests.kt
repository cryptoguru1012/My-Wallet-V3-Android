package info.blockchain.balance

import org.amshove.kluent.`should equal`
import org.junit.Test
import java.util.Locale

class MoneyFormattingTests {

    @Test
    fun `FiatValue formatted as Money`() {
        Locale.setDefault(Locale.CANADA)

        val money: Money = 1.cad()
        money.symbol `should equal` "$"
        money.toStringWithSymbol() `should equal` "$1.00"
        money.toStringWithoutSymbol() `should equal` "1.00"
    }

    @Test
    fun `FiatValue formatted as Money with rounding`() {
        Locale.setDefault(Locale.CANADA)
        val money: Money = 1.695.cad()

        money.toStringWithSymbol() `should equal` "$1.69"
        money.toStringWithoutSymbol() `should equal` "1.69"
    }

    @Test
    fun `FiatValue JPY formatted as Money`() {
        Locale.setDefault(Locale.US)

        val money: Money = 123.jpy()
        money.symbol `should equal` "JPY"
        money.toStringWithSymbol() `should equal` "JPY123"
        money.toStringWithoutSymbol() `should equal` "123"
    }

    @Test
    fun `CryptoValue formatted as Money`() {
        Locale.setDefault(Locale.US)

        val money: Money = 1.23.bitcoin()
        money.symbol `should equal` "BTC"
        money.toStringWithSymbol() `should equal` "1.23 BTC"
        money.toStringWithoutSymbol() `should equal` "1.23"
    }

    @Test
    fun `CryptoValue Ether formatted as Money`() {
        Locale.setDefault(Locale.FRANCE)

        val money: Money = 1.23.ether()
        money.symbol `should equal` "ETH"
        money.toStringWithSymbol() `should equal` "1,23 ETH"
        money.toStringWithoutSymbol() `should equal` "1,23"
    }
}
