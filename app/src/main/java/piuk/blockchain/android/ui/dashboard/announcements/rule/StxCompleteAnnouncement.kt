package piuk.blockchain.android.ui.dashboard.announcements.rule

import androidx.annotation.VisibleForTesting
import io.reactivex.Single
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.dashboard.announcements.AnnouncementHost
import piuk.blockchain.android.ui.dashboard.announcements.AnnouncementQueries
import piuk.blockchain.android.ui.dashboard.announcements.AnnouncementRule
import piuk.blockchain.android.ui.dashboard.announcements.DismissRecorder
import piuk.blockchain.android.ui.dashboard.announcements.DismissRule
import piuk.blockchain.android.ui.dashboard.announcements.StandardAnnouncementCard

internal class StxCompleteAnnouncement(
    dismissRecorder: DismissRecorder,
    private val queries: AnnouncementQueries
) : AnnouncementRule(dismissRecorder) {

    override val dismissKey = DISMISS_KEY

    override fun shouldShow(): Single<Boolean> {
        if (dismissEntry.isDismissed) {
            return Single.just(false)
        }

        return queries.hasReceivedStxAirdrop()
    }

    override fun show(host: AnnouncementHost) {

        val card = StandardAnnouncementCard(
            name = name,
            titleText = R.string.stacks_airdrop_complete_card_title,
            bodyText = R.string.stacks_airdrop_complete_card_description,
            ctaText = R.string.stacks_airdrop_complete_card_button,
            iconImage = R.drawable.ic_logo_stx,
            dismissFunction = {
                host.dismissAnnouncementCard()
            },
            ctaFunction = {
                host.dismissAnnouncementCard()
                host.startStxReceivedDetail()
            },
            dismissEntry = dismissEntry,
            dismissRule = DismissRule.CardOneTime
        )
        host.showAnnouncementCard(card)
    }

    override val name = "stx_airdrop_complete"

    companion object {
        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        const val DISMISS_KEY = "STX_AIRDROP_COMPLETE_DISMISSED"
    }
}
