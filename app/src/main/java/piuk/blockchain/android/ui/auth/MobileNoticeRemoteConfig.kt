package piuk.blockchain.android.ui.auth

import io.reactivex.Single

interface MobileNoticeRemoteConfig {
    fun mobileNoticeDialog(): Single<MobileNoticeDialog>
}