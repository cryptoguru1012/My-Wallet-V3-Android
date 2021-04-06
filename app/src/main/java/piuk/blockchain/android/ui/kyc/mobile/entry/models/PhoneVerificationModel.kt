package piuk.blockchain.android.ui.kyc.mobile.entry.models

import piuk.blockchain.android.ui.kyc.mobile.validation.models.VerificationCode

data class PhoneVerificationModel(
    val sanitizedPhoneNumber: String,
    val verificationCode: VerificationCode
)