package piuk.blockchain.android.ui.tour

import com.blockchain.notifications.analytics.AnalyticsEvent

sealed class IntroTourAnalyticsEvent(
    override val event: String,
    override val params: Map<String, String> = emptyMap()
) : AnalyticsEvent {

    object IntroOfferedAnalytics : IntroTourAnalyticsEvent(WALLET_INTRO_OFFERED)
    object IntroStartedAnalytics : IntroTourAnalyticsEvent(WALLET_INTRO_STARTED)
    object IntroDismissedAnalytics : IntroTourAnalyticsEvent(WALLET_INTRO_DISMISSED)
    object IntroPortfolioViewedAnalytics : IntroTourAnalyticsEvent(WALLET_INTRO_PORTFOLIO_VIEWED)
    object IntroSendViewedAnalytics : IntroTourAnalyticsEvent(WALLET_INTRO_SEND_VIEWED)
    object IntroRequestViewedAnalytics : IntroTourAnalyticsEvent(WALLET_INTRO_REQUEST_VIEWED)
    object IntroSwapViewedAnalytics : IntroTourAnalyticsEvent(WALLET_INTRO_SWAP_VIEWED)
    object IntroBuySellViewedAnalytics : IntroTourAnalyticsEvent(WALLET_INTRO_BUYSELL_VIEWED)

    companion object {
        private const val WALLET_INTRO_OFFERED = "wallet_intro_offered"
        private const val WALLET_INTRO_STARTED = "wallet_intro_started"
        private const val WALLET_INTRO_DISMISSED = "wallet_intro_dismissed"
        private const val WALLET_INTRO_PORTFOLIO_VIEWED = "wallet_intro_portfolio_viewed"
        private const val WALLET_INTRO_SEND_VIEWED = "wallet_intro_send_viewed"
        private const val WALLET_INTRO_REQUEST_VIEWED = "wallet_intro_request_viewed"
        private const val WALLET_INTRO_SWAP_VIEWED = "wallet_intro_swap_viewed"
        private const val WALLET_INTRO_BUYSELL_VIEWED = "wallet_intro_buysell_viewed"
    }
}
