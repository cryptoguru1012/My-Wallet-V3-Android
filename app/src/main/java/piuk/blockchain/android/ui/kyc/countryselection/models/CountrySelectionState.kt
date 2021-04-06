package piuk.blockchain.android.ui.kyc.countryselection.models

import androidx.annotation.StringRes
import piuk.blockchain.android.ui.kyc.countryselection.util.CountryDisplayModel

sealed class CountrySelectionState {
    object Loading : CountrySelectionState()
    data class Error(@StringRes val errorMessage: Int) : CountrySelectionState()
    data class Data(val countriesList: List<CountryDisplayModel>) : CountrySelectionState()
}