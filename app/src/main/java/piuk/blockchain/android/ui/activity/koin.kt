package piuk.blockchain.android.ui.activity

import com.blockchain.koin.payloadScopeQualifier
import io.reactivex.android.schedulers.AndroidSchedulers
import org.koin.dsl.module
import piuk.blockchain.android.ui.activity.detail.ActivityDetailState
import piuk.blockchain.android.ui.activity.detail.ActivityDetailsInteractor
import piuk.blockchain.android.ui.activity.detail.ActivityDetailsModel
import piuk.blockchain.android.ui.activity.detail.TransactionHelper
import piuk.blockchain.android.ui.activity.detail.TransactionInOutMapper

val activitiesModule = module {

    scope(payloadScopeQualifier) {

        factory {
            ActivitiesModel(
                initialState = ActivitiesState(),
                mainScheduler = AndroidSchedulers.mainThread(),
                interactor = get(),
                environmentConfig = get(),
                crashLogger = get()
            )
        }

        factory {
            ActivitiesInteractor(
                coincore = get(),
                activityRepository = get(),
                custodialWalletManager = get(),
                simpleBuyPrefs = get(),
                analytics = get()
            )
        }

        factory {
            ActivityDetailsModel(
                initialState = ActivityDetailState(),
                mainScheduler = AndroidSchedulers.mainThread(),
                interactor = get(),
                environmentConfig = get(),
                crashLogger = get()
            )
        }

        factory {
            ActivityDetailsInteractor(
                currencyPrefs = get(),
                transactionInputOutputMapper = get(),
                assetActivityRepository = get(),
                custodialWalletManager = get(),
                stringUtils = get(),
                coincore = get()
            )
        }

        factory {
            TransactionInOutMapper(
                transactionHelper = get(),
                payloadDataManager = get(),
                stringUtils = get(),
                bchDataManager = get(),
                xlmDataManager = get(),
                environmentSettings = get(),
                coincore = get()
            )
        }

        factory {
            TransactionHelper(
                payloadDataManager = get(),
                bchDataManager = get()
            )
        }
    }
}
