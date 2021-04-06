package com.blockchain.notifications.analytics

import com.google.firebase.analytics.FirebaseAnalytics

class UserAnalyticsImpl(
    private val firebaseAnalytics: FirebaseAnalytics
) : UserAnalytics {
    override fun logUserId(userId: String) {
        firebaseAnalytics.setUserId(userId)
    }

    override fun logUserProperty(userPropery: UserProperty) {
        firebaseAnalytics.setUserProperty(userPropery.property, userPropery.value)
    }
}