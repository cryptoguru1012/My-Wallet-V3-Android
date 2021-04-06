package piuk.blockchain.android.deeplink

import android.net.Uri
import org.amshove.kluent.`should be`
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import piuk.blockchain.android.BlockchainTestApplication

@Config(sdk = [23], application = BlockchainTestApplication::class)
@RunWith(RobolectricTestRunner::class)
class EmailVerificationDeepLinkHelperTest {
    private lateinit var subject: EmailVerificationDeepLinkHelper

    @Before
    fun setup() {
        subject = EmailVerificationDeepLinkHelper()
    }

    @Test
    fun `test that 'PIT_SIGNUP' context is parsed`() {
        val uri = Uri.parse(
            "https://login.blockchain.com/login?deep_link_path=email_verified&context=PIT_SIGNUP"
        )
        subject.mapUri(uri) `should be` EmailVerifiedLinkState.FromPitLinking
    }

    @Test
    fun `test that 'PIT_SIGNUP' case insensitive context is parsed`() {
        val uri = Uri.parse(
            "https://login.blockchain.com/login?deep_link_path=email_verified&context=pit_signup"
        )
        subject.mapUri(uri) `should be` EmailVerifiedLinkState.FromPitLinking
    }

    @Test
    fun `test invalid deep_link_path`() {
        val uri = Uri.parse(
            "https://login.blockchain.com/login?deep_link_path=not_valid&context=PIT_SIGNUP"
        )
        subject.mapUri(uri) `should be` EmailVerifiedLinkState.NoUri
    }

    @Test
    fun `test valid deep_link_path but unknown context`() {
        val uri = Uri.parse(
            "https://login.blockchain.com/login?deep_link_path=email_verified&context=not_known"
        )
        subject.mapUri(uri) `should be` EmailVerifiedLinkState.NoUri
    }
}