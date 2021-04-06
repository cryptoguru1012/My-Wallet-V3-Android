package piuk.blockchain.android.ui.kyc.navhost

import androidx.annotation.StringRes
import androidx.navigation.NavDirections
import piuk.blockchain.android.campaign.CampaignType
import piuk.blockchain.androidcoreui.ui.base.View

interface KycNavHostView : View {

    val campaignType: CampaignType

    val showTiersLimitsSplash: Boolean

    fun displayLoading(loading: Boolean)

    fun showErrorToastAndFinish(@StringRes message: Int)

    fun navigate(directions: NavDirections)

    fun navigateToKycSplash()

    fun navigateToResubmissionSplash()
}
