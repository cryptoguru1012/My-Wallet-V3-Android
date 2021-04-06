package piuk.blockchain.android.data.coinswebsocket.models

import com.google.gson.annotations.SerializedName

sealed class SocketRequest(private val command: Command) {

    object PingRequest : SocketRequest(Command.PING)

    data class SubscribeRequest(val entity: Entity, val coin: Coin, val param: Parameters?) :
        SocketRequest(Command.SUBSCRIBE)

    data class UnSubscribeRequest(val entity: Entity, val coin: Coin, val param: Parameters?) :
        SocketRequest(Command.UNSUBSCRIBE)
}

enum class Command {
    @SerializedName("ping")
    PING,
    @SerializedName("subscribe")
    SUBSCRIBE,
    @SerializedName("unsubscribe")
    UNSUBSCRIBE
}

sealed class Parameters {
    data class SimpleAddress(val address: String) : Parameters()
    data class Guid(val guid: String) : Parameters()
    data class TokenedAddress(val address: String, @SerializedName("token_address") val tokenAddress: String) :
        Parameters()
}