package piuk.blockchain.android.ui.kyc.reentry

enum class ReentryPoint(val entryPoint: String) {
    EmailEntry("Email Entry"),
    CountrySelection("Country Selection"),
    Profile("Profile Entry"),
    Address("Address Entry"),
    MobileEntry("Mobile Entry"),
    Veriff("Veriff Splash")
}
