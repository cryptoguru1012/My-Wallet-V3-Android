package piuk.blockchain.android.thepit

import com.blockchain.notifications.analytics.AnalyticsEvent

sealed class PitAnalyticsEvent(
    override val event: String,
    override val params: Map<String, String>
) : AnalyticsEvent {
    object ConnectNowEvent :
        PitAnalyticsEvent("pit_connect_now_tapped", emptyMap())

    object LearnMoreEvent :
        PitAnalyticsEvent("pit_learn_more_tapped", emptyMap())

    object AnnouncementTappedEvent :
        PitAnalyticsEvent("pit_announcement_tapped", emptyMap())
}