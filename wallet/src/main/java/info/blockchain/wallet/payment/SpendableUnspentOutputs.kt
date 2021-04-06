package info.blockchain.wallet.payment

import info.blockchain.api.data.UnspentOutput

import java.math.BigInteger

class SpendableUnspentOutputs(
    var spendableOutputs: List<UnspentOutput> = emptyList(),
    var absoluteFee: BigInteger = BigInteger.ZERO,
    var consumedAmount: BigInteger = BigInteger.ZERO,
    var isReplayProtected: Boolean = false
) {
    val spendableBalance get() = spendableOutputs.sum() - absoluteFee
}
