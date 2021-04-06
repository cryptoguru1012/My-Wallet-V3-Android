package piuk.blockchain.android.ui.dashboard.announcements

import com.nhaarman.mockito_kotlin.argumentCaptor
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.times
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.verifyNoMoreInteractions
import com.nhaarman.mockito_kotlin.whenever
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import piuk.blockchain.androidcore.utils.PersistentPrefs
import java.lang.ClassCastException

class DismissRecorderTest {

    private val prefs: PersistentPrefs = mock()
    private val clock: DismissClock = mock()

    private lateinit var subject: DismissRecorder

    @Before
    fun setup() {
        subject = DismissRecorder(
            prefs = prefs,
            clock = clock
        )
    }

    @Test
    fun `entry is undismissed by default`() {
        whenever(prefs.getValue(DISMISS_KEY, 0)).thenReturn(0)
        whenever(clock.now()).thenReturn(BASE_TIME)

        val entry = subject[DISMISS_KEY]

        assertFalse(entry.isDismissed)
    }

    @Test
    fun `persistent cards write to persistent prefs`() {

        whenever(clock.now()).thenReturn(BASE_TIME)

        val entry = subject[DISMISS_KEY]

        entry.dismiss(DismissRule.CardPersistent)

        // Check that any historical value is removed:
        verify(prefs).removeValue(DISMISS_KEY)

        val captorValue = argumentCaptor<Long>()
        val captorString = argumentCaptor<String>()

        verify(prefs).setValue(captorString.capture(), captorValue.capture())

        assertEquals(captorString.firstValue, DISMISS_KEY)
        assertEquals(captorValue.firstValue, Long.MAX_VALUE)

        verifyNoMoreInteractions(prefs)
    }

    @Test
    fun `periodic cards write to persistent prefs`() {

        whenever(clock.now()).thenReturn(BASE_TIME)

        val entry = subject[DISMISS_KEY]

        entry.dismiss(DismissRule.CardPeriodic)

        // Check that any historical value is removed:
        verify(prefs).removeValue(DISMISS_KEY)

        val captorValue = argumentCaptor<Long>()
        val captorString = argumentCaptor<String>()

        verify(prefs).setValue(captorString.capture(), captorValue.capture())

        assertEquals(captorString.firstValue, DISMISS_KEY)
        assertEquals(captorValue.firstValue, BASE_TIME + ONE_WEEK)

        verifyNoMoreInteractions(prefs)
    }

    @Test
    fun `one time cards write to persistent prefs`() {

        whenever(clock.now()).thenReturn(BASE_TIME)

        val entry = subject[DISMISS_KEY]

        entry.dismiss(DismissRule.CardOneTime)

        // Check that any historical value is removed:
        verify(prefs).removeValue(DISMISS_KEY)

        val captorValue = argumentCaptor<Long>()
        val captorString = argumentCaptor<String>()

        verify(prefs).setValue(captorString.capture(), captorValue.capture())

        assertEquals(captorString.firstValue, DISMISS_KEY)
        assertEquals(captorValue.firstValue, Long.MAX_VALUE)

        verifyNoMoreInteractions(prefs)
    }

    @Test
    fun `one time cards, once dismissed stay dismissed`() {

        whenever(clock.now()).thenReturn(BASE_TIME).thenReturn(BASE_TIME + ONE_WEEK)
        whenever(prefs.getValue(DISMISS_KEY, 0L)).thenReturn(BASE_TIME)

        val entry = subject[DISMISS_KEY]

        entry.dismiss(DismissRule.CardOneTime)

        val result = entry.isDismissed

        assertTrue(result)
    }

    @Test
    fun `periodic cards undismiss after defined interval`() {
        whenever(clock.now()).thenReturn(BASE_TIME + 1)
        whenever(prefs.getValue(DISMISS_KEY, 0L)).thenReturn(BASE_TIME)

        val entry = subject[DISMISS_KEY]

        val result = entry.isDismissed

        assertFalse(result)
    }

    @Test
    fun `boolean style dismissed is dismissed with new time-based call`() {
        whenever(prefs.getValue(DISMISS_KEY, 0L)).thenThrow(ClassCastException("It's a boolean"))
        whenever(prefs.getValue(DISMISS_KEY, false)).thenReturn(true)
        whenever(clock.now()).thenReturn(BASE_TIME)

        val entry = subject[DISMISS_KEY]

        val result = entry.isDismissed

        assertTrue(result)
    }

    @Test
    fun `undismiss clears all dismiss flags`() {
        val announcementList: AnnouncementList = mock()
        whenever(announcementList.dismissKeys()).thenReturn(listOf(DISMISS_KEY, DISMISS_KEY_2, DISMISS_KEY_3))

        subject[DISMISS_KEY].dismiss(DismissRule.CardPersistent)
        subject[DISMISS_KEY_2].dismiss(DismissRule.CardPeriodic)
        subject[DISMISS_KEY_3].dismiss(DismissRule.CardOneTime)

        subject.undismissAll(announcementList)

        val captorString = argumentCaptor<String>()
        verify(prefs, times(6)).removeValue(captorString.capture())

        assertEquals(captorString.firstValue, DISMISS_KEY)
        assertEquals(captorString.secondValue, DISMISS_KEY_2)
        assertEquals(captorString.thirdValue, DISMISS_KEY_3)
    }

    companion object {
        private const val DISMISS_KEY = "wibble"
        private const val DISMISS_KEY_2 = "wobble"
        private const val DISMISS_KEY_3 = "wubble"

        private const val BASE_TIME = 10000000L
        private const val MINUTE = 60L * 1000L
        private const val ONE_WEEK = 7L * 24L * 60L * MINUTE
    }
}