package info.blockchain.wallet.ethereum.data

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty

import java.math.BigInteger

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.NONE,
    getterVisibility = JsonAutoDetect.Visibility.NONE,
    setterVisibility = JsonAutoDetect.Visibility.NONE,
    creatorVisibility = JsonAutoDetect.Visibility.NONE,
    isGetterVisibility = JsonAutoDetect.Visibility.NONE)
class Erc20TransferResponse {
    @JsonProperty("logIndex")
    var logIndex: String = ""

    @JsonProperty("tokenHash")
    var tokenHash: String = ""

    @JsonProperty("from")
    var from: String = ""

    @JsonProperty("to")
    var to: String = ""

    @JsonProperty("value")
    var value: BigInteger = 0.toBigInteger()

    @JsonProperty("decimals")
    var decimals: Int = 0

    @JsonProperty("blockHash")
    var blockHash: String = ""

    @JsonProperty("transactionHash")
    var transactionHash: String = ""

    @JsonProperty("blockNumber")
    var blockNumber: BigInteger = 0.toBigInteger()

    @JsonProperty("timestamp")
    var timestamp: Long = 0
}
