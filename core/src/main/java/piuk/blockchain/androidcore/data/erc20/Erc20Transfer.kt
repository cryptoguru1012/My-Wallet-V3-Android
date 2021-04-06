package piuk.blockchain.androidcore.data.erc20

import info.blockchain.wallet.ethereum.data.Erc20TransferResponse
import java.math.BigInteger

data class Erc20Transfer(
    val logIndex: String,
    val transactionHash: String,
    val value: BigInteger,
    val from: String,
    val to: String,
    val blockNumber: BigInteger,
    val timestamp: Long
) {
    fun isFromAccount(accountHash: String): Boolean =
        accountHash == from

    fun isToAccount(accountHash: String): Boolean =
        accountHash == to

    companion object {
        operator fun invoke(erc20TransferResponse: Erc20TransferResponse): Erc20Transfer =
            Erc20Transfer(
                erc20TransferResponse.logIndex,
                erc20TransferResponse.transactionHash,
                erc20TransferResponse.value,
                erc20TransferResponse.from,
                erc20TransferResponse.to,
                erc20TransferResponse.blockNumber,
                erc20TransferResponse.timestamp
            )
    }
}