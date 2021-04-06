package com.blockchain.nabu.models.responses.nabu

import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.ToJson
import java.util.Locale

internal class KycTierStateAdapter {

    @FromJson
    fun fromJson(input: String): KycTierState =
        when (input.toUpperCase(Locale.US)) {
            NONE -> KycTierState.None
            REJECTED -> KycTierState.Rejected
            PENDING -> KycTierState.Pending
            VERIFIED -> KycTierState.Verified
            UNDER_REVIEW -> KycTierState.UnderReview
            EXPIRED -> KycTierState.Expired
            else -> throw JsonDataException("Unknown KYC Tier State: $input, unsupported data type")
        }

    @ToJson
    fun toJson(kycTierState: KycTierState): String =
        when (kycTierState) {
            KycTierState.None -> NONE
            KycTierState.Rejected -> REJECTED
            KycTierState.Pending -> PENDING
            KycTierState.UnderReview -> UNDER_REVIEW
            KycTierState.Verified -> VERIFIED
            KycTierState.Expired -> EXPIRED
        }

    private companion object {
        private const val NONE = "NONE"
        private const val REJECTED = "REJECTED"
        private const val PENDING = "PENDING"
        private const val VERIFIED = "VERIFIED"
        private const val UNDER_REVIEW = "UNDER_REVIEW"
        private const val EXPIRED = "EXPIRED"
    }
}
