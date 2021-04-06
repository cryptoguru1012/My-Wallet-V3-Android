package piuk.blockchain.android.ui.kyc.search

import io.reactivex.Observable
import org.junit.Test
import piuk.blockchain.android.ui.kyc.countryselection.util.CountryDisplayModel

class CountrySelectionFilterKtTest {

    @Test
    fun `returns entire list on initial subscription`() {
        countryList.just()
            .filterCountries(Observable.empty())
            .test()
            .assertValue(countryList)
    }

    @Test
    fun `filters out all countries if string doesn't match`() {
        countryList.just()
            .filterCountries("Denmark".just())
            .skip(1)
            .test()
            .assertValue(emptyList())
    }

    @Test
    fun `returns exact match for country code`() {
        countryList.just()
            .filterCountries("GB".just())
            .skip(1)
            .test()
            .assertValue(listOf(countryList[2]))
    }

    @Test
    fun `returns two matches but prioritises acronym`() {
        countryList.just()
            .filterCountries("UK".just())
            .skip(1)
            .test()
            .assertValue(listOf(countryList[2], countryList[0]))
    }

    private val countryList = listOf(
        CountryDisplayModel(
            name = "Ukraine",
            countryCode = "UKR"
        ),
        CountryDisplayModel(
            name = "United States",
            countryCode = "US"
        ),
        CountryDisplayModel(
            name = "United Kingdom",
            countryCode = "GB"
        ),
        CountryDisplayModel(
            name = "Germany",
            countryCode = "DE"
        ),
        CountryDisplayModel(
            name = "France",
            countryCode = "FR"
        )
    )

    private fun <T> T.just(): Observable<T> = Observable.just(this)
}