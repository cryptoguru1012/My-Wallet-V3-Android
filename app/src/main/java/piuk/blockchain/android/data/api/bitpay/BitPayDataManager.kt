package piuk.blockchain.android.data.api.bitpay

import piuk.blockchain.android.data.api.bitpay.models.RawPaymentRequest
import io.reactivex.Single
import piuk.blockchain.android.data.api.bitpay.models.BitPayPaymentResponse
import piuk.blockchain.android.data.api.bitpay.models.BitPaymentRequest
import piuk.blockchain.androidcore.utils.extensions.applySchedulers

class BitPayDataManager constructor(
    private val bitPayService: BitPayService
) {

    /**
     * Returns a [RawPaymentRequest] object containing details about the BitPay invoice payment
     * request
     *
     * @param invoiceId The id of the BitPay invoice
     *
     * @return A [RawPaymentRequest] object wrapped in a [Single].
     */
    fun getRawPaymentRequest(invoiceId: String): Single<RawPaymentRequest> =
        bitPayService.getRawPaymentRequest(invoiceId = invoiceId).applySchedulers()

    fun paymentVerificationRequest(invoiceId: String, paymentRequest: BitPaymentRequest):
            Single<BitPayPaymentResponse> =
        bitPayService.getPaymentVerificationRequest(
            invoiceId = invoiceId,
            body = paymentRequest
        ).applySchedulers()

    fun paymentSubmitRequest(invoiceId: String, paymentRequest: BitPaymentRequest):
            Single<BitPayPaymentResponse> =
        bitPayService.getPaymentSubmitRequest(
            invoiceId = invoiceId,
            body = paymentRequest
        ).applySchedulers()
}