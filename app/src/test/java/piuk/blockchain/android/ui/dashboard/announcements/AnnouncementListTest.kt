package piuk.blockchain.android.ui.dashboard.announcements

import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.never
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.verifyZeroInteractions
import com.nhaarman.mockito_kotlin.whenever
import io.reactivex.Scheduler
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import io.reactivex.schedulers.TestScheduler
import org.amshove.kluent.`it returns`
import org.amshove.kluent.`it throws`
import org.junit.Test
import kotlin.test.assertEquals

class AnnouncementListTest {

    private val host: AnnouncementHost = mock()
    private val orderAdapter: AnnouncementConfigAdapter = mock()
    private val dismissRecorder: DismissRecorder = mock()

    private fun createAnnouncementList(
        availableAnnouncements: List<AnnouncementRule>,
        scheduler: Scheduler = Schedulers.trampoline()
    ) =
        AnnouncementList(
            mainScheduler = scheduler,
            orderAdapter = orderAdapter,
            availableAnnouncements = availableAnnouncements,
            dismissRecorder = dismissRecorder
        )

    @Test
    fun `announcement list is correctly mapped`() {

        val order = listOf("three", "two", "one")

        val available = listOf(
            announcement("one"),
            announcement("two"),
            announcement("three")
        )

        val result = createAnnouncementList(available)
            .buildAnnouncementList(order)

        assertEquals(3, result.size)
        assertEquals("three", result[0].name)
        assertEquals("two", result[1].name)
        assertEquals("one", result[2].name)
    }

    @Test
    fun `announcement list is correctly mapped, when order contains unknown announcements`() {

        val order = listOf("three", "two_point_five", "two", "one")

        val available = listOf(
            announcement("one"),
            announcement("two"),
            announcement("three")
        )

        val result = createAnnouncementList(available)
            .buildAnnouncementList(order)

        assertEquals(3, result.size)
        assertEquals("three", result[0].name)
        assertEquals("two", result[1].name)
        assertEquals("one", result[2].name)
    }

    @Test
    fun `announcement list is correctly mapped, when order skips announcement`() {

        val order = listOf("three", "two", "one")

        val available = listOf(
            announcement("one"),
            announcement("two"),
            announcement("three"),
            announcement("four")
        )

        val result = createAnnouncementList(available)
            .buildAnnouncementList(order)

        assertEquals(3, result.size)
        assertEquals("three", result[0].name)
        assertEquals("two", result[1].name)
        assertEquals("one", result[2].name)
    }

    @Test
    fun `calls no announcements until subscribed`() {

        val order = listOf("three", "two", "one")
        whenever(orderAdapter.announcementConfig).thenReturn(Single.just(AnnounceConfig(order, INTERVAL)))

        val available = listOf(
            announcement("one"),
            announcement("two"),
            announcement("three"),
            announcement("four")
        )

        createAnnouncementList(available)
            .showNextAnnouncement(host)

        verifyZeroInteractions(host)
    }

    @Test
    fun `calls first announcement that says it should show`() {

        val order = listOf("one", "two", "three")
        whenever(orderAdapter.announcementConfig).thenReturn(Single.just(AnnounceConfig(order, INTERVAL)))

        val available = listOf(
            dontShowAnnouncement("one"),
            announcement("two"),
            announcement("three"),
            announcement("four")
        )

        createAnnouncementList(available)
            .showNextAnnouncement(host)
            .test()
            .assertValue(available[1])
            .assertComplete()
            .assertNoErrors()

        verify(available[1]).show(host)
    }

    @Test
    fun `nothing available`() {
        val order = listOf("one", "two", "three")
        whenever(orderAdapter.announcementConfig).thenReturn(Single.just(AnnounceConfig(order, INTERVAL)))

        val available = listOf(
            dontShowAnnouncement("one"),
            dontShowAnnouncement("two"),
            dontShowAnnouncement("three"),
            dontShowAnnouncement("four")
        )

        createAnnouncementList(available)
            .showNextAnnouncement(host)
            .test()
            .assertValues()
            .assertComplete()
            .assertNoErrors()
    }

    @Test
    fun `does not check announcements beyond one that says it should show`() {

        val order = listOf("one", "two", "three")
        whenever(orderAdapter.announcementConfig).thenReturn(Single.just(AnnounceConfig(order, INTERVAL)))

        val available = listOf(
            announcement("one"),
            dontCheckAnnouncement("two"),
            dontCheckAnnouncement("three"),
            dontCheckAnnouncement("four")
        )

        createAnnouncementList(available)
            .showNextAnnouncement(host)
            .test()
            .assertValue(available[0])
            .assertComplete()
            .assertNoErrors()

        verify(available[0]).show(host)
    }

    @Test
    fun `calls first announcement that says it should show - alternative scheduler`() {

        val order = listOf("one", "two", "three")
        whenever(orderAdapter.announcementConfig).thenReturn(Single.just(AnnounceConfig(order, INTERVAL)))

        val available = listOf(
            dontShowAnnouncement("one"),
            dontShowAnnouncement("two"),
            announcement("three"),
            dontShowAnnouncement("four")
        )

        val scheduler = TestScheduler()

        val test = createAnnouncementList(available, scheduler)
            .showNextAnnouncement(host)
            .test()
            .assertValues()
            .assertNoErrors()
            .assertNotComplete()

        verify(available[2], never()).show(host)

        scheduler.triggerActions()

        verify(available[2]).show(host)

        test.assertValue(available[2])
            .assertComplete()
            .assertNoErrors()
    }

    private fun announcement(announcementName: String): AnnouncementRule =
        mock {
            on { shouldShow() } `it returns` Single.just(true)
            on { name } `it returns` announcementName
        }

    private fun dontShowAnnouncement(announcementName: String): AnnouncementRule =
        mock {
            on { shouldShow() } `it returns` Single.just(false)
            on { show(host) } `it throws` RuntimeException("Not expected")
            on { name } `it returns` announcementName
        }

    private fun dontCheckAnnouncement(announcementName: String): AnnouncementRule =
        mock {
            on { shouldShow() } `it throws` RuntimeException("Not expected")
            on { show(host) } `it throws` RuntimeException("Not expected")
            on { name } `it returns` announcementName
        }

    companion object {
        private const val INTERVAL = 7L
    }
}