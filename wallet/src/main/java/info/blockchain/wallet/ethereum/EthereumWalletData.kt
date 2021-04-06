package info.blockchain.wallet.ethereum

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import java.util.ArrayList
import java.util.HashMap

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(
    fieldVisibility = Visibility.NONE,
    getterVisibility = Visibility.NONE,
    setterVisibility = Visibility.NONE,
    creatorVisibility = Visibility.NONE,
    isGetterVisibility = Visibility.NONE
)
class EthereumWalletData {

    @JsonProperty("has_seen")
    var hasSeen: Boolean = false

    @JsonProperty("default_account_idx")
    var defaultAccountIdx: Int = 0

    @JsonProperty("accounts")
    var accounts: ArrayList<EthereumAccount>? = null

    @JsonProperty("erc20")
    var erc20Tokens: HashMap<String, Erc20TokenData>? = null

    @JsonProperty("legacy_account")
    var legacyAccount: EthereumAccount? = null

    @JsonProperty("tx_notes")
    var txNotes: HashMap<String, String>? = null

    @JsonProperty("last_tx")
    var lastTx: String? = null

    @JsonProperty("last_tx_timestamp")
    var lastTxTimestamp: Long = 0
}
