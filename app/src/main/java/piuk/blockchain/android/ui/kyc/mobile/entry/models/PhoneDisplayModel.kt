package piuk.blockchain.android.ui.kyc.mobile.entry.models

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class PhoneDisplayModel(
    val formattedString: String,
    val sanitizedString: String
) : Parcelable