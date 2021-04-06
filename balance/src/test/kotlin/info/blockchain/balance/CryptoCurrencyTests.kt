package info.blockchain.balance

import org.amshove.kluent.`should be`
import org.junit.Test

class CryptoCurrencyTests {

    @Test
    fun `lowercase btc`() {
        CryptoCurrency.fromNetworkTicker("btc") `should be` CryptoCurrency.BTC
    }

    @Test
    fun `uppercase BTC`() {
        CryptoCurrency.fromNetworkTicker("BTC") `should be` CryptoCurrency.BTC
    }

    @Test
    fun `lowercase bch`() {
        CryptoCurrency.fromNetworkTicker("btc") `should be` CryptoCurrency.BTC
    }

    @Test
    fun `uppercase BCH`() {
        CryptoCurrency.fromNetworkTicker("BCH") `should be` CryptoCurrency.BCH
    }

    @Test
    fun `lowercase eth`() {
        CryptoCurrency.fromNetworkTicker("eth") `should be` CryptoCurrency.ETHER
    }

    @Test
    fun `uppercase ETH`() {
        CryptoCurrency.fromNetworkTicker("ETH") `should be` CryptoCurrency.ETHER
    }

    @Test
    fun `uppercase XLM`() {
        CryptoCurrency.fromNetworkTicker("XLM") `should be` CryptoCurrency.XLM
    }

    @Test
    fun `lowercase xlm`() {
        CryptoCurrency.fromNetworkTicker("xlm") `should be` CryptoCurrency.XLM
    }

    @Test
    fun `uppercase PAX`() {
        CryptoCurrency.fromNetworkTicker("PAX") `should be` CryptoCurrency.PAX
    }

    @Test
    fun `lowercase pax`() {
        CryptoCurrency.fromNetworkTicker("pax") `should be` CryptoCurrency.PAX
    }

    @Test
    fun `mixed case pax`() {
        CryptoCurrency.fromNetworkTicker("Pax") `should be` CryptoCurrency.PAX
    }

    @Test
    fun `null should return null`() {
        CryptoCurrency.fromNetworkTicker(null) `should be` null
    }

    @Test
    fun `empty should return null`() {
        CryptoCurrency.fromNetworkTicker("") `should be` null
    }

    @Test
    fun `not recognised should return null`() {
        CryptoCurrency.fromNetworkTicker("NONE") `should be` null
    }

    @Test
    fun `btc dp is 8`() {
        CryptoCurrency.BTC.dp `should be` 8
        CryptoCurrency.BTC.userDp `should be` 8
    }

    @Test
    fun `bch dp is 8`() {
        CryptoCurrency.BCH.dp `should be` 8
        CryptoCurrency.BCH.userDp `should be` 8
    }

    @Test
    fun `ether dp is 18 and 8 for user`() {
        CryptoCurrency.ETHER.dp `should be` 18
        CryptoCurrency.ETHER.userDp `should be` 8
    }

    @Test
    fun `XLM dp is 7`() {
        CryptoCurrency.XLM.dp `should be` 7
        CryptoCurrency.XLM.userDp `should be` 7
    }

    @Test
    fun `btc required confirmations is 3`() {
        CryptoCurrency.BTC.requiredConfirmations `should be` 3
    }

    @Test
    fun `bch required confirmations is 3`() {
        CryptoCurrency.BCH.requiredConfirmations `should be` 3
    }

    @Test
    fun `ether required confirmations is 12`() {
        CryptoCurrency.ETHER.requiredConfirmations `should be` 12
    }
}