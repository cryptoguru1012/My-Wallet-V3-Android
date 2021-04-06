package piuk.blockchain.android.ui.onboarding

import piuk.blockchain.androidcoreui.ui.base.View

interface OnboardingView : View {

    fun showFingerprintPrompt()

    fun showEmailPrompt()

    fun showFingerprintDialog(pincode: String)

    fun showEnrollFingerprintsDialog()

    val showEmail: Boolean

    val showFingerprints: Boolean
}
