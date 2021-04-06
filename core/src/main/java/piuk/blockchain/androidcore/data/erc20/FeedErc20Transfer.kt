package piuk.blockchain.androidcore.data.erc20

import io.reactivex.Observable
import java.math.BigInteger

data class FeedErc20Transfer(val transfer: Erc20Transfer, val feeObservable: Observable<BigInteger>)
