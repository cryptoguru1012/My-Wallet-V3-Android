package piuk.blockchain.android.thepit

import android.net.Uri
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import piuk.blockchain.android.BlockchainTestApplication
import kotlin.test.assertEquals
import kotlin.test.assertNull

@Config(sdk = [23], application = BlockchainTestApplication::class)
@RunWith(RobolectricTestRunner::class)
class ThePitDeepLinkParserTest {
    private val subject = ThePitDeepLinkParser()

    @Test
    fun `Valid URI is correctly parsed`() {
        val uri = Uri.parse(VALID_TEST_PIT_URI)

        val r = subject.mapUri(uri)

        assertEquals(r, LINK_ID)
    }

    @Test
    fun `Malformed URI returns null`() {
        val uri = Uri.parse(INVALID_TEST_PIT_URI)

        val r = subject.mapUri(uri)

        assertNull(r)
    }

    @Test
    fun `empty URI returns null`() {
        val uri = Uri.parse("")

        val r = subject.mapUri(uri)

        assertNull(r)
    }

    @Test
    fun `URI with missing link id returns null`() {
        val uri = Uri.parse(MISSING_LINK_ID)

        val r = subject.mapUri(uri)

        assertNull(r)
    }

    @Test
    fun `URI with missing link id parameter returns null`() {
        val uri = Uri.parse(MISSING_LINK_ID_PARAMETER)

        val r = subject.mapUri(uri)

        assertNull(r)
    }

    companion object {
        private const val LINK_ID = "11111111-2222-3333-4444-555555556666"
        private const val VALID_TEST_PIT_URI =
            "https://wallet-frontend-v4.dev.blockchain.info/#/open/link-account?link_id=$LINK_ID"

        private const val INVALID_TEST_PIT_URI =
            "https://wallet-frontend-v4.dev.blockchain.info/#/nope/link-account?link_id=$LINK_ID"

        private const val MISSING_LINK_ID =
            "https://wallet-frontend-v4.dev.blockchain.info/#/open/link-account?link_id="

        private const val MISSING_LINK_ID_PARAMETER =
            "https://wallet-frontend-v4.dev.blockchain.info/#/open/link-account"
    }
}