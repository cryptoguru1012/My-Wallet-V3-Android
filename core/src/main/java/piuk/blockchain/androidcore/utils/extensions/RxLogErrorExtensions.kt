package piuk.blockchain.androidcore.utils.extensions

import com.blockchain.notifications.analytics.Analytics
import com.blockchain.notifications.analytics.apiError
import com.blockchain.notifications.analytics.networkError
import io.reactivex.Observable
import io.reactivex.Single
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException

fun <T : Any> Observable<T>.logAnalyticsError(analytics: Analytics): Observable<T> =
    doOnError {
        val httpException = it as? HttpException
        httpException?.logError(analytics)
    }

fun <T : Any> Single<T>.logAnalyticsError(analytics: Analytics): Single<T> =
    doOnError {
        val httpException = it as? HttpException
        httpException?.logError(analytics)
    }

private fun HttpException.logError(analytics: Analytics) {
    val url = response()?.raw()?.request?.url ?: return
    val host = url.host
    val scheme = url.scheme ?: ""
    val path = url.toString().removePrefix("$scheme://$host")
    if (this is SocketTimeoutException || this is IOException) {
        analytics.logEvent(networkError(host,
            path, message()))
    } else {
        val rawError = response()?.errorBody()?.string()
        val requestId = response()?.headers()?.get("x-request-id")
        val errorCode = response()?.code() ?: -1
        analytics.logEvent(apiError(host, path, rawError, requestId, errorCode))
    }
}
