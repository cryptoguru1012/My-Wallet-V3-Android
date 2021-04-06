package piuk.blockchain.android.data.coinswebsocket.service

import piuk.blockchain.androidcore.data.events.ActionEvent

interface MessagesSocketHandler {
    fun triggerNotification(title: String, marquee: String, text: String)
    fun sendBroadcast(event: ActionEvent)
    fun showToast(message: Int)
}