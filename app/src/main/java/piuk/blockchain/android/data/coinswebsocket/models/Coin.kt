package piuk.blockchain.android.data.coinswebsocket.models

import com.google.gson.annotations.SerializedName

enum class Coin {
    @SerializedName("eth")
    ETH,
    @SerializedName("btc")
    BTC,
    @SerializedName("bch")
    BCH,
    @SerializedName("none")
    None
}
