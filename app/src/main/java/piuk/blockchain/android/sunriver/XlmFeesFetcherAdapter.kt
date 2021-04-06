package piuk.blockchain.android.sunriver

import com.blockchain.datamanagers.fees.XlmFees
import com.blockchain.datamanagers.fees.feeForType
import com.blockchain.datamanagers.fees.getFeeOptions
import com.blockchain.fees.FeeType
import com.blockchain.sunriver.XlmFeesFetcher
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import io.reactivex.Single
import piuk.blockchain.androidcore.data.fees.FeeDataManager

internal class XlmFeesFetcherAdapter(
    private val feesDataManager: FeeDataManager
) : XlmFeesFetcher {
    override fun operationFee(feeType: FeeType): Single<CryptoValue> =
        feesDataManager.getFeeOptions(CryptoCurrency.XLM)
            .map { it as XlmFees }
            .map { it.feeForType(feeType) }
}