package piuk.blockchain.android.ui.dashboard.announcements.rule

import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.whenever
import info.blockchain.wallet.api.data.Settings
import io.reactivex.Observable
import org.junit.Before
import org.junit.Test
import piuk.blockchain.android.ui.dashboard.announcements.DismissRecorder
import piuk.blockchain.androidcore.data.settings.SettingsDataManager

class VerifyEmailAnnouncementTest {

    private val dismissRecorder: DismissRecorder = mock()
    private val dismissEntry: DismissRecorder.DismissEntry = mock()
    private val walletSettings: SettingsDataManager = mock()
    private val settings: Settings = mock()

    private lateinit var subject: VerifyEmailAnnouncement

    @Before
    fun setUp() {
        whenever(dismissRecorder[VerifyEmailAnnouncement.DISMISS_KEY]).thenReturn(dismissEntry)
        whenever(dismissEntry.prefsKey).thenReturn(VerifyEmailAnnouncement.DISMISS_KEY)

        whenever(walletSettings.getSettings()).thenReturn(Observable.just(settings))

        subject =
            VerifyEmailAnnouncement(
                dismissRecorder = dismissRecorder,
                walletSettings = walletSettings
            )
    }

    @Test
    fun `should show, when not already shown, email address is valid and unverified`() {
        whenever(dismissEntry.isDismissed).thenReturn(false)

        whenever(settings.isEmailVerified).thenReturn(false)
        whenever(settings.email).thenReturn(VALID_EMAIL_ADDRESS)

        subject.shouldShow()
            .test()
            .assertValue { it }
            .assertValueCount(1)
            .assertComplete()
    }

    @Test
    fun `should not show, when not already shown, email address is valid and verified`() {
        whenever(dismissEntry.isDismissed).thenReturn(false)

        whenever(settings.isEmailVerified).thenReturn(true)
        whenever(settings.email).thenReturn(VALID_EMAIL_ADDRESS)

        subject.shouldShow()
            .test()
            .assertValue { !it }
            .assertValueCount(1)
            .assertComplete()
    }

    @Test
    fun `should not show, when not already shown, email address is empty and unverified`() {
        whenever(dismissEntry.isDismissed).thenReturn(false)

        whenever(settings.isEmailVerified).thenReturn(false)
        whenever(settings.email).thenReturn(INVALID_EMAIL_ADDRESS)

        subject.shouldShow()
            .test()
            .assertValue { !it }
            .assertValueCount(1)
            .assertComplete()
    }

    companion object {
        private const val VALID_EMAIL_ADDRESS = "smaug@thedestroyer.me"
        private const val INVALID_EMAIL_ADDRESS = ""
    }
}
