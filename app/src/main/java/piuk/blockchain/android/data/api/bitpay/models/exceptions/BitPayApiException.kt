package piuk.blockchain.android.data.api.bitpay.models.exceptions

import android.annotation.SuppressLint
import retrofit2.Response

class BitPayApiException private constructor(message: String) : Throwable(message) {

    var _httpErrorCode: Int = -1
    lateinit var _error: String

    companion object {

        @SuppressLint("SyntheticAccessor")
        fun fromResponseBody(response: Response<*>?): BitPayApiException {
            val bitpayErrorResponse = response?.errorBody()!!.string()
            val httpErrorCode = response.code()

            return BitPayApiException("$httpErrorCode: $bitpayErrorResponse")
                .apply {
                    _httpErrorCode = httpErrorCode
                    _error = bitpayErrorResponse
                }
        }
    }
}