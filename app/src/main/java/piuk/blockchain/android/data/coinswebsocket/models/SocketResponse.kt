package piuk.blockchain.android.data.coinswebsocket.models

import com.google.gson.annotations.SerializedName
import java.math.BigDecimal
import java.math.BigInteger

data class SocketResponse(
    val success: Boolean? = true,
    val entity: Entity? = null,
    val coin: Coin? = null,
    val block: EthBlock? = null,
    val message: String? = null,
    val checksum: String? = null,
    val op: String? = null
)

data class BtcBchResponse(
    val transaction: BtcTransaction?
)

data class EthResponse(
    val transaction: EthTransaction?,
    val account: EthAccount? = null,
    val entity: Entity? = null,
    val tokenTransfer: TokenTransfer? = null,
    val tokenAccount: TokenAccount? = null,
    val tokenAccountKey: TokenAccountKey? = null,
    val param: TokenParams? = null
)

data class TokenParams(
    val tokenAddress: String? = null,
    val accountAddress: String? = null
)

data class BtcTransaction(
    val inputs: List<Input> = emptyList(),
    @SerializedName("out") val outputs: List<Output> = emptyList(),
    val hash: String?
)

data class Output(
    val spent: Boolean,
    val addr: String?,
    val xpub: String?,
    val value: BigDecimal?
)

data class Input(
    val address: String,
    val value: BigInteger,
    @SerializedName("prev_out") val prevOut: Output?
)

data class EthBlock(val hash: String, val parentHash: String, val nonce: String, val gasLimit: Long)
data class EthAccount(val address: String, val txHash: String, val tx: EthTransaction)

data class EthTransaction(
    val hash: String,
    val blockHash: String,
    val blockNumber: Long,
    val from: String,
    val to: String,
    val value: BigInteger,
    val state: TransactionState
)

data class TokenAccount(
    val accountHash: String,
    val tokenHash: String,
    val balance: String,
    val totalSent: String
)

data class TokenTransfer(
    val blockHash: String,
    val transactionHash: String,
    val blockNumber: String,
    val tokenHash: String,
    val logIndex: String,
    val from: String,
    val to: String,
    val value: BigInteger,
    val timeStamp: Long
)

data class TokenAccountKey(
    val accountHash: String,
    val tokenHash: String
)

enum class TransactionState {
    @SerializedName("pending")
    PENDING,
    @SerializedName("replaced")
    REPLACED,
    @SerializedName("confirmed")
    CONFIRMED
}