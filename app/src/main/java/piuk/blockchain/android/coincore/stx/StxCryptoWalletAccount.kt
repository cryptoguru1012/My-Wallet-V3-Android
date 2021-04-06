package piuk.blockchain.android.coincore.stx

import com.blockchain.nabu.datamanagers.CustodialWalletManager
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.Money
import io.reactivex.Single
import piuk.blockchain.android.coincore.ActivitySummaryList
import piuk.blockchain.android.coincore.ReceiveAddress
import piuk.blockchain.android.coincore.TxEngine
import piuk.blockchain.android.coincore.impl.CryptoNonCustodialAccount
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateDataManager
import piuk.blockchain.androidcore.data.payload.PayloadDataManager

class StxCryptoWalletAccount(
    payloadManager: PayloadDataManager,
    custodialWalletManager: CustodialWalletManager,
    override val label: String,
    private val address: String,
    override val exchangeRates: ExchangeRateDataManager
) : CryptoNonCustodialAccount(payloadManager, CryptoCurrency.STX, custodialWalletManager) {

    override val isFunded: Boolean
        get() = false

    override val isDefault: Boolean = true // Only one account ever, so always default

    override val accountBalance: Single<Money>
        get() = throw NotImplementedError("STX Not Implemented")

    override val actionableBalance: Single<Money>
        get() = accountBalance

    override val receiveAddress: Single<ReceiveAddress>
        get() = throw NotImplementedError("STX Not Implemented")

    override val activity: Single<ActivitySummaryList>
        get() = Single.just(emptyList())

    override fun createTxEngine(): TxEngine {
        throw NotImplementedError("STX Not Implemented")
    }
}