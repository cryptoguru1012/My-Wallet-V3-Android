package piuk.blockchain.android.ui.kyc.mobile.validation.models

data class VerificationCode(private val verificationCode: String) {
    val code = verificationCode.toUpperCase()
    val isValid = verificationCode.length >= 5
}