package piuk.blockchain.android.ui.kyc.invalidcountry

import piuk.blockchain.android.ui.kyc.countryselection.util.CountryDisplayModel
import piuk.blockchain.androidcoreui.ui.base.View

interface KycInvalidCountryView : View {

    val displayModel: CountryDisplayModel

    fun showProgressDialog()

    fun dismissProgressDialog()

    fun finishPage()
}