package piuk.blockchain.android.ui.auth

import com.blockchain.remoteconfig.RemoteConfig
import io.reactivex.Single
import com.squareup.moshi.Moshi

class FirebaseMobileNoticeRemoteConfig(private val remoteConfig: RemoteConfig) : MobileNoticeRemoteConfig {

    private val moshi = Moshi.Builder().build()

    override fun mobileNoticeDialog(): Single<MobileNoticeDialog> =
        remoteConfig.getRawJson(key)
            .filter { it.isNotEmpty() }
            .map {
                moshi.adapter(MobileNoticeDialog::class.java).fromJson(it) ?: MobileNoticeDialog()
            }
            .toSingle()

    companion object {
        private const val key = "mobile_notice"
    }
}