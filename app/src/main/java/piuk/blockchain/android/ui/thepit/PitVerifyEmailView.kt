package piuk.blockchain.android.ui.thepit

interface PitVerifyEmailView : piuk.blockchain.androidcoreui.ui.base.View {
    fun mailResendFailed()
    fun mailResentSuccessfully()
    fun emailVerified()
}