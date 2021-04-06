package piuk.blockchain.android.ui.dashboard.announcements.rule

import io.reactivex.Single
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.dashboard.announcements.AnnouncementHost
import piuk.blockchain.android.ui.dashboard.announcements.AnnouncementQueries
import piuk.blockchain.android.ui.dashboard.announcements.AnnouncementRule
import piuk.blockchain.android.ui.dashboard.announcements.DismissRecorder
import piuk.blockchain.android.ui.dashboard.announcements.DismissRule
import piuk.blockchain.android.ui.dashboard.announcements.MiniAnnouncementCard

class RegisteredForAirdropMiniAnnouncement(
    dismissRecorder: DismissRecorder,
    private val queries: AnnouncementQueries
) : AnnouncementRule(dismissRecorder) {
    override val dismissKey: String
        get() = ""
    override val name: String
        get() = "stx_registered_airdrop_mini"

    override fun shouldShow(): Single<Boolean> =
        queries.isRegistedForStxAirdrop()

    override fun show(host: AnnouncementHost) {
        host.showAnnouncementCard(
            card = MiniAnnouncementCard(
                name = name,
                dismissRule = DismissRule.CardPersistent,
                background = R.drawable.blockstack_announcement_mini_card_background,
                dismissEntry = dismissEntry,
                titleText = R.string.stacks_airdrop_registered_mini_card_title,
                bodyText = R.string.stacks_airdrop_registered_mini_card_body,
                iconImage = R.drawable.ic_airdrop_parachute_green,
                hasCta = false
            )
        )
    }
}