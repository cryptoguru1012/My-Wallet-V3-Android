package com.blockchain.notifications.analytics

sealed class RequestAnalyticsEvents(override val event: String, override val params: Map<String, String> = mapOf()) :
    AnalyticsEvent {
    object TabItemClicked : RequestAnalyticsEvents("request_tab_item_click")
    object QrAddressClicked : RequestAnalyticsEvents("request_qr_address_click")
    object RequestPaymentClicked : RequestAnalyticsEvents("request_request_payment_click")
}