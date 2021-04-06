package piuk.blockchain.android.ui.dashboard.announcements.rule

import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.whenever
import io.reactivex.Single
import org.junit.Before
import org.junit.Test
import piuk.blockchain.android.ui.dashboard.announcements.AnnouncementQueries
import piuk.blockchain.android.ui.dashboard.announcements.DismissRecorder

class StxCompleteAnnouncementTest {

    private val dismissRecorder: DismissRecorder = mock()
    private val dismissEntry: DismissRecorder.DismissEntry = mock()
    private val queries: AnnouncementQueries = mock()

    private lateinit var subject: StxCompleteAnnouncement

    @Before
    fun setUp() {
        whenever(dismissRecorder[StxCompleteAnnouncement.DISMISS_KEY]).thenReturn(dismissEntry)
        whenever(dismissEntry.prefsKey).thenReturn(StxCompleteAnnouncement.DISMISS_KEY)

        subject =
            StxCompleteAnnouncement(
                dismissRecorder = dismissRecorder,
                queries = queries
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
    fun `should show, when not already shown, and the blockstack airdrop has been completed`() {
        whenever(dismissEntry.isDismissed).thenReturn(false)
        whenever(queries.hasReceivedStxAirdrop()).thenReturn(Single.just(true))

        subject.shouldShow()
            .test()
            .assertValue { it }
            .assertValueCount(1)
            .assertComplete()
    }

    @Test
    fun `should not show, when not already shown, and the blockstack airdrop has not been completed`() {
        whenever(dismissEntry.isDismissed).thenReturn(false)
        whenever(queries.hasReceivedStxAirdrop()).thenReturn(Single.just(true))

        subject.shouldShow()
            .test()
            .assertValue { it }
            .assertValueCount(1)
            .assertComplete()
    }
}
