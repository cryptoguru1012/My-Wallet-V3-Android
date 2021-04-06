package info.blockchain.balance

import org.amshove.kluent.`should equal`
import org.junit.Test
import java.util.Locale

class CryptoValuePartsTest {

    @Test
    fun `extract BTC parts in UK`() {
        Locale.setDefault(Locale.UK)

        1.2.bitcoin()
            .toStringParts().apply {
                symbol `should equal` "BTC"
                major `should equal` "1"
                minor `should equal` "2"
                majorAndMinor`should equal` "1.2"
            }
    }

    @Test
    fun `extract ETH parts in US`() {
        Locale.setDefault(Locale.US)

        9.89.ether()
            .toStringParts().apply {
                symbol `should equal` "ETH"
                major `should equal` "9"
                minor `should equal` "89"
                majorAndMinor`should equal` "9.89"
            }
    }

    @Test
    fun `extract max DP ETHER parts in UK`() {
        Locale.setDefault(Locale.UK)

        5.12345678.ether()
            .toStringParts().apply {
                symbol `should equal` "ETH"
                major `should equal` "5"
                minor `should equal` "12345678"
                majorAndMinor`should equal` "5.12345678"
            }
    }

    @Test
    fun `extract parts from large number in UK`() {
        Locale.setDefault(Locale.UK)

        5345678.ether()
            .toStringParts().apply {
                symbol `should equal` "ETH"
                major `should equal` "5,345,678"
                minor `should equal` "0"
                majorAndMinor`should equal` "5,345,678.0"
            }
    }

    @Test
    fun `extract parts from large number in France`() {
        Locale.setDefault(Locale.FRANCE)

        5345678.987.ether()
            .toStringParts().apply {
                symbol `should equal` "ETH"
                major `should equal` "5 345 678"
                minor `should equal` "987"
                majorAndMinor`should equal` "5 345 678,987"
            }
    }

    @Test
    fun `extract parts from large number in Italy`() {
        Locale.setDefault(Locale.ITALY)

        9345678.987.ether()
            .toStringParts().apply {
                symbol `should equal` "ETH"
                major `should equal` "9.345.678"
                minor `should equal` "987"
                majorAndMinor`should equal` "9.345.678,987"
            }
    }
}
