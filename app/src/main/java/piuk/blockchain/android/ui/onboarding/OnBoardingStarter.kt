package piuk.blockchain.android.ui.onboarding

import android.content.Context
import com.blockchain.activities.StartOnboarding

@Suppress("unused")
class OnBoardingStarter : StartOnboarding {
    override fun startEmailOnboarding(context: Any) {
        (context as? Context)?.let {
            OnboardingActivity.launchForEmail(it)
        }
    }
}