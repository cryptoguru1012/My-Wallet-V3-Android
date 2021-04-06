package piuk.blockchain.android.identity

import io.reactivex.Single

interface UserIdentity {
    fun isEligibleFor(feature: Feature): Single<Boolean>
    fun isVerifiedFor(feature: Feature): Single<Boolean>
}

sealed class Feature {
    class TierLevel(val tier: Tier) : Feature()
    object SimplifiedDueDiligence : Feature()
}

enum class Tier {
    BRONZE, SILVER, GOLD
}