package piuk.blockchain.android.ui.customviews.dialogs

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.widget.AdapterView
import android.widget.ArrayAdapter
import com.jakewharton.rxbinding2.support.v7.widget.queryTextChanges
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.dialog_select_country.*
import piuk.blockchain.android.R
import piuk.blockchain.android.util.gone
import timber.log.Timber
import java.util.SortedMap
import java.util.concurrent.TimeUnit

class CountryDialog(
    context: Context,
    private val countryListSource: Single<SortedMap<String, String>>,
    private val listener: CountryCodeSelectionListener
) : Dialog(context) {

    private val compositeDisposable = CompositeDisposable()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dialog_select_country)

        compositeDisposable +=
            countryListSource
                .subscribeBy(
                    onSuccess = { renderCountryMap(it) },
                    onError = {
                        Timber.e(it)
                        cancel()
                    }
                )
    }

    private fun renderCountryMap(countryMap: SortedMap<String, String>) {
        val arrayAdapter = ArrayAdapter<String>(
            context,
            android.R.layout.simple_list_item_1,
            android.R.id.text1,
            countryMap.keys.toTypedArray()
        )
        list_view_countries.adapter = arrayAdapter
        progress_bar_select_country_dialog.gone()

        list_view_countries.onItemClickListener =
            AdapterView.OnItemClickListener { parent, _, position, _ ->
                val item = parent!!.getItemAtPosition(position).toString()
                val code = countryMap[item]!!
                listener.onCountrySelected(code, item)
                dismiss()
            }

        search_view_country.apply {
            queryHint = context.getString(R.string.search_country)

            this.queryTextChanges()
                .skipInitialValue()
                .debounce(300, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(AndroidSchedulers.mainThread())
                .doOnNext { arrayAdapter.filter.filter(it) }
                .subscribe()
        }
    }

    override fun cancel() {
        super.cancel()
        compositeDisposable.clear()
    }

    interface CountryCodeSelectionListener {

        fun onCountrySelected(code: String, name: String)
    }
}