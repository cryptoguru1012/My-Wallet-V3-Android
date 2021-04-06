package piuk.blockchain.android.data.api.bitpay.models

import java.math.BigInteger

data class RawPaymentRequest(
    val instructions: List<BitPaymentInstructions>,
    val memo: String,
    val expires: String,
    val paymentUrl: String
)

data class BitPaymentInstructions(
    val outputs: List<BitPayPaymentRequestOutput>
)

data class BitPayPaymentRequestOutput(
    val amount: BigInteger,
    val address: String
)