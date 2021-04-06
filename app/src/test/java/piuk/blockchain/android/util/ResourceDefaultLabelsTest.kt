package piuk.blockchain.android.util

import android.content.res.Resources
import com.blockchain.wallet.DefaultLabels
import com.nhaarman.mockito_kotlin.mock
import info.blockchain.balance.CryptoCurrency
import org.amshove.kluent.`it returns`
import org.amshove.kluent.`should equal`
import org.junit.Test
import piuk.blockchain.android.R

class ResourceDefaultLabelsTest {

    private val resources: Resources = mock {
        on { getString(R.string.btc_default_wallet_name) } `it returns` "A - BTC"
        on { getString(R.string.eth_default_account_label) } `it returns` "B - ETH"
        on { getString(R.string.bch_default_account_label) } `it returns` "C - BCH"
        on { getString(R.string.xlm_default_account_label) } `it returns` "D - XLM"
    }

    private val defaultLabels: DefaultLabels =
        ResourceDefaultLabels(resources)

    @Test
    fun `btc default label`() {
        defaultLabels.getDefaultNonCustodialWalletLabel(CryptoCurrency.BTC) `should equal` "A - BTC"
    }

    @Test
    fun `ether default label`() {
        defaultLabels.getDefaultNonCustodialWalletLabel(CryptoCurrency.ETHER) `should equal` "B - ETH"
    }

    @Test
    fun `bch default label`() {
        defaultLabels.getDefaultNonCustodialWalletLabel(CryptoCurrency.BCH) `should equal` "C - BCH"
    }

    @Test
    fun `xlm default label`() {
        defaultLabels.getDefaultNonCustodialWalletLabel(CryptoCurrency.XLM) `should equal` "D - XLM"
    }
}
