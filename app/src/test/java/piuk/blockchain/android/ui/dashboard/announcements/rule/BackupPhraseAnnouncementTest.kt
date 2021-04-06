package piuk.blockchain.android.ui.dashboard.announcements.rule

import com.blockchain.preferences.WalletStatus
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.whenever
import org.junit.Before
import org.junit.Test
import piuk.blockchain.android.ui.dashboard.announcements.DismissRecorder

class BackupPhraseAnnouncementTest {

    private val dismissRecorder: DismissRecorder = mock()
    private val dismissEntry: DismissRecorder.DismissEntry = mock()
    private val walletStatus: WalletStatus = mock()

    private lateinit var subject: BackupPhraseAnnouncement

    @Before
    fun setUp() {
        whenever(dismissRecorder[BackupPhraseAnnouncement.DISMISS_KEY]).thenReturn(dismissEntry)
        whenever(dismissEntry.prefsKey).thenReturn(BackupPhraseAnnouncement.DISMISS_KEY)

        subject =
            BackupPhraseAnnouncement(
                dismissRecorder = dismissRecorder,
                walletStatus = walletStatus
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
    fun `should show, when not already shown`() {
        whenever(dismissEntry.isDismissed).thenReturn(false)
        whenever(walletStatus.isWalletFunded).thenReturn(true)
        whenever(walletStatus.isWalletBackedUp).thenReturn(false)

        subject.shouldShow()
            .test()
            .assertValue { it }
            .assertValueCount(1)
            .assertComplete()
    }

    @Test
    fun `should not show, if wallet not funded`() {
        whenever(dismissEntry.isDismissed).thenReturn(false)
        whenever(walletStatus.isWalletFunded).thenReturn(false)
        whenever(walletStatus.isWalletBackedUp).thenReturn(false)

        subject.shouldShow()
            .test()
            .assertValue { !it }
            .assertValueCount(1)
            .assertComplete()
    }

    @Test
    fun `should not show, if wallet already backed up`() {
        whenever(dismissEntry.isDismissed).thenReturn(false)
        whenever(walletStatus.isWalletFunded).thenReturn(true)
        whenever(walletStatus.isWalletBackedUp).thenReturn(true)

        subject.shouldShow()
            .test()
            .assertValue { !it }
            .assertValueCount(1)
            .assertComplete()
    }
}