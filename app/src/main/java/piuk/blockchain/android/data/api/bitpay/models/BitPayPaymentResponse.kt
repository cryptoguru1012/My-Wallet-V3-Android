package piuk.blockchain.android.data.api.bitpay.models

data class BitPayPaymentResponse(val payment: BitPayVerification)

data class BitPayVerification(val chain: String, val transactions: List<BitPayTransaction>)