package piuk.blockchain.android.data.api.bitpay

import io.reactivex.Single
import piuk.blockchain.android.data.api.bitpay.models.BitPayChain
import piuk.blockchain.android.data.api.bitpay.models.BitPayPaymentResponse
import piuk.blockchain.android.data.api.bitpay.models.BitPaymentRequest
import piuk.blockchain.android.data.api.bitpay.models.RawPaymentRequest
import piuk.blockchain.android.data.api.bitpay.models.exceptions.wrapErrorMessage
import piuk.blockchain.androidcore.data.api.EnvironmentConfig
import piuk.blockchain.androidcore.data.rxjava.RxBus
import piuk.blockchain.androidcore.data.rxjava.RxPinning
import retrofit2.Retrofit

class BitPayService constructor(
    environmentConfig: EnvironmentConfig,
    retrofit: Retrofit,
    rxBus: RxBus
) {

    private val service: BitPay = retrofit.create(BitPay::class.java)
    private val rxPinning: RxPinning = RxPinning(rxBus)
    private val baseUrl: String = environmentConfig.bitpayUrl

    internal fun getRawPaymentRequest(
        path: String = "$baseUrl$PATH_BITPAY_INVOICE",
        invoiceId: String
    ): Single<RawPaymentRequest> = rxPinning.callSingle {
        service.getRawPaymentRequest("$path/$invoiceId", BitPayChain("BTC"))
            .wrapErrorMessage()
    }

    internal fun getPaymentVerificationRequest(
        path: String = "$baseUrl$PATH_BITPAY_INVOICE",
        body: BitPaymentRequest,
        invoiceId: String
    ): Single<BitPayPaymentResponse> = rxPinning.callSingle {
        service.paymentRequest(path = "$path/$invoiceId",
            body = body,
            contentType = "application/payment-verification")
            .wrapErrorMessage()
    }

    internal fun getPaymentSubmitRequest(
        path: String = "$baseUrl$PATH_BITPAY_INVOICE",
        body: BitPaymentRequest,
        invoiceId: String
    ): Single<BitPayPaymentResponse> = rxPinning.callSingle {
        service.paymentRequest(path = "$path/$invoiceId",
            body = body,
            contentType = "application/payment")
            .wrapErrorMessage()
    }
}