package piuk.blockchain.android.data.coinswebsocket.models

import com.google.gson.annotations.SerializedName

enum class Entity {
    @SerializedName("height")
    Height,
    @SerializedName("header")
    Header,
    @SerializedName("confirmed_transaction")
    ConfirmedTransaction,
    @SerializedName("pending_transaction")
    PendingTransaction,
    @SerializedName("account")
    Account,
    @SerializedName("xpub")
    Xpub,
    @SerializedName("token_transfer")
    TokenTransfer,
    @SerializedName("token_account_delta")
    TokenAccountDelta,
    @SerializedName("token_account")
    TokenAccount,
    @SerializedName("wallet")
    Wallet
}