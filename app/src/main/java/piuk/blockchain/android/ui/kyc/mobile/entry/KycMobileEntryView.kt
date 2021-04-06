package piuk.blockchain.android.ui.kyc.mobile.entry

import androidx.annotation.StringRes
import piuk.blockchain.android.ui.kyc.mobile.entry.models.PhoneDisplayModel
import piuk.blockchain.androidcore.data.settings.PhoneNumber
import io.reactivex.Observable
import piuk.blockchain.androidcoreui.ui.base.View

interface KycMobileEntryView : View {

    val uiStateObservable: Observable<Pair<PhoneNumber, Unit>>

    fun preFillPhoneNumber(phoneNumber: String)

    fun showErrorToast(@StringRes message: Int)

    fun dismissProgressDialog()

    fun showProgressDialog()

    fun continueSignUp(displayModel: PhoneDisplayModel)
}
