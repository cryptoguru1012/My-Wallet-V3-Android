package piuk.blockchain.android.ui.dashboard.announcements.rule

import com.blockchain.preferences.WalletStatus
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.whenever
import org.junit.Before
import org.junit.Test
import piuk.blockchain.android.ui.dashboard.announcements.DismissRecorder

class BitpayAnnouncementTest {

    private val dismissRecorder: DismissRecorder = mock()
    private val dismissEntry: DismissRecorder.DismissEntry = mock()
    private val walletStatus: WalletStatus = mock()

    private lateinit var subject: BitpayAnnouncement

    @Before
    fun setUp() {
        whenever(dismissRecorder[BitpayAnnouncement.DISMISS_KEY]).thenReturn(dismissEntry)
        whenever(dismissEntry.prefsKey).thenReturn(BitpayAnnouncement.DISMISS_KEY)

        subject =
            BitpayAnnouncement(
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
    fun `should show, when not already shown and no bitpay transactions have been made`() {
        whenever(dismissEntry.isDismissed).thenReturn(false)
        whenever(walletStatus.isWalletFunded).thenReturn(true)

        whenever(walletStatus.hasMadeBitPayTransaction).thenReturn(false)

        subject.shouldShow()
            .test()
            .assertValue { it }
            .assertValueCount(1)
            .assertComplete()
    }

    @Test
    fun `should not show, when not already shown and a bitpay transactions has been made`() {
        whenever(dismissEntry.isDismissed).thenReturn(false)
        whenever(walletStatus.isWalletFunded).thenReturn(true)

        whenever(walletStatus.hasMadeBitPayTransaction).thenReturn(true)

        subject.shouldShow()
            .test()
            .assertValue { !it }
            .assertValueCount(1)
            .assertComplete()
    }
}
