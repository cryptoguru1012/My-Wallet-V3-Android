package piuk.blockchain.android.ui.dashboard.announcements.rule

import androidx.annotation.VisibleForTesting
import io.reactivex.Single
import io.reactivex.rxkotlin.Singles
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.dashboard.announcements.AnnouncementHost
import piuk.blockchain.android.ui.dashboard.announcements.AnnouncementQueries
import piuk.blockchain.android.ui.dashboard.announcements.AnnouncementRule
import piuk.blockchain.android.ui.dashboard.announcements.DismissRecorder
import piuk.blockchain.android.ui.dashboard.announcements.DismissRule
import piuk.blockchain.android.campaign.CampaignType
import piuk.blockchain.android.ui.dashboard.announcements.StandardAnnouncementCard

class KycForAirdropsAnnouncement(
    dismissRecorder: DismissRecorder,
    private val queries: AnnouncementQueries
) : AnnouncementRule(dismissRecorder) {

    override val dismissKey = DISMISS_KEY

    override fun shouldShow(): Single<Boolean> {
        if (dismissEntry.isDismissed) {
            return Single.just(false)
        }

        return Singles.zip(
            queries.canKyc(),
            queries.isKycGoldStartedOrComplete()
        ).map { (kyc, isGold) -> kyc && !isGold }
    }

    override fun show(host: AnnouncementHost) {
        host.showAnnouncementCard(
            card = StandardAnnouncementCard(
                name = name,
                dismissRule = DismissRule.CardPeriodic,
                dismissEntry = dismissEntry,
                titleText = R.string.kyc_airdrop_card_title,
                bodyText = R.string.kyc_airdrop_card_body,
                ctaText = R.string.kyc_airdrop_card_cta,
                iconImage = R.drawable.ic_announce_kyc_airdrop,
                dismissFunction = {
                    host.dismissAnnouncementCard()
                },
                ctaFunction = {
                    host.dismissAnnouncementCard()
                    host.startKyc(CampaignType.Sunriver)
                }
            )
        )
    }

    override val name = "kyc_airdrop"

    companion object {
        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        const val DISMISS_KEY = "KycAirdropAnnouncement_DISMISSED"
    }
}
