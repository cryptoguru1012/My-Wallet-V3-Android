package piuk.blockchain.android.ui.dashboard.announcements.rule

import com.blockchain.preferences.WalletStatus
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.whenever
import info.blockchain.wallet.api.data.Settings
import io.reactivex.Observable
import org.junit.Before
import org.junit.Test
import piuk.blockchain.android.ui.dashboard.announcements.DismissRecorder
import piuk.blockchain.androidcore.data.settings.SettingsDataManager

class TwoFAAnnouncementTest {

    private val dismissRecorder: DismissRecorder = mock()
    private val dismissEntry: DismissRecorder.DismissEntry = mock()
    private val walletStatus: WalletStatus = mock()
    private val walletSettings: SettingsDataManager = mock()
    private val settings: Settings = mock()

    private lateinit var subject: TwoFAAnnouncement

    @Before
    fun setUp() {
        whenever(dismissRecorder[TwoFAAnnouncement.DISMISS_KEY]).thenReturn(dismissEntry)
        whenever(dismissEntry.prefsKey).thenReturn(TwoFAAnnouncement.DISMISS_KEY)

        whenever(walletSettings.getSettings()).thenReturn(Observable.just(settings))

        subject =
            TwoFAAnnouncement(
                dismissRecorder = dismissRecorder,
                walletStatus = walletStatus,
                walletSettings = walletSettings
            )
    }

    @Test
    fun `should not show, when already shown`() {
        whenever(dismissEntry.isDismissed).thenReturn(true)

        subject.shouldShow()
            .test()
            .assertValue { !it }
            .assertValueCount(1)
            .assertComplete()
    }

    @Test
    fun `should show, when not already shown, wallet is funded and 2 fa is not enabled`() {
        whenever(dismissEntry.isDismissed).thenReturn(false)
        whenever(walletStatus.isWalletFunded).thenReturn(true)

        whenever(settings.isSmsVerified).thenReturn(false)
        whenever(settings.authType).thenReturn(Settings.AUTH_TYPE_OFF)

        subject.shouldShow()
            .test()
            .assertValue { it }
            .assertValueCount(1)
            .assertComplete()
    }

    @Test
    fun `should not show, when not already shown, wallet is not funded`() {
        whenever(dismissEntry.isDismissed).thenReturn(false)
        whenever(walletStatus.isWalletFunded).thenReturn(false)

        whenever(settings.isSmsVerified).thenReturn(false)
        whenever(settings.authType).thenReturn(Settings.AUTH_TYPE_OFF)

        subject.shouldShow()
            .test()
            .assertValue { !it }
            .assertValueCount(1)
            .assertComplete()
    }

    @Test
    fun `should not show, when 2fa is enabled - yubi key`() {
        whenever(dismissEntry.isDismissed).thenReturn(false)
        whenever(walletStatus.isWalletFunded).thenReturn(false)

        whenever(settings.isSmsVerified).thenReturn(false)
        whenever(settings.authType).thenReturn(Settings.AUTH_TYPE_YUBI_KEY)

        subject.shouldShow()
            .test()
            .assertValue { !it }
            .assertValueCount(1)
            .assertComplete()
    }

    @Test
    fun `should not show, when 2fa is enabled - SMS`() {
        whenever(dismissEntry.isDismissed).thenReturn(false)
        whenever(walletStatus.isWalletFunded).thenReturn(false)

        whenever(settings.isSmsVerified).thenReturn(true)
        whenever(settings.authType).thenReturn(Settings.AUTH_TYPE_SMS)

        subject.shouldShow()
            .test()
            .assertValue { !it }
            .assertValueCount(1)
            .assertComplete()
    }
}
