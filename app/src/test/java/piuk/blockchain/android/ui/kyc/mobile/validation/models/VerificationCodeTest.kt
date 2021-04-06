package piuk.blockchain.android.ui.kyc.mobile.validation.models

import org.amshove.kluent.`should equal to`
import org.junit.Test

class VerificationCodeTest {

    @Test
    fun `code too short, should return invalid`() {
        VerificationCode("123").isValid `should equal to` false
    }

    @Test
    fun `code correct length, should return invalid`() {
        VerificationCode("12345").isValid `should equal to` true
    }

    @Test
    fun `code should always return upper case`() {
        VerificationCode("abcd1").code `should equal to` "ABCD1"
    }
}