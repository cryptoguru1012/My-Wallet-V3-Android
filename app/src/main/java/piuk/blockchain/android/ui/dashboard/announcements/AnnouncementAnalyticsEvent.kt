package piuk.blockchain.android.ui.dashboard.announcements

import com.blockchain.notifications.analytics.AnalyticsEvent

sealed class AnnouncementAnalyticsEvent(
    cardName: String,
    override val event: String,
    override val params: Map<String, String> = mapOf(CARD_TITLE to cardName)
) : AnalyticsEvent {

    class CardShown(name: String) : AnnouncementAnalyticsEvent(
        cardName = name,
        event = CARD_SHOWN
    )

    class CardActioned(name: String) : AnnouncementAnalyticsEvent(
        cardName = name,
        event = CARD_ACTIONED
    )

    class CardDismissed(name: String) : AnnouncementAnalyticsEvent(
        cardName = name,
        event = CARD_DISMISSED
    )

    companion object {
        private const val CARD_SHOWN = "announcement_offered"
        private const val CARD_ACTIONED = "announcement_started"
        private const val CARD_DISMISSED = "announcement_dismissed"

        private const val CARD_TITLE = "card_title"
    }
}
