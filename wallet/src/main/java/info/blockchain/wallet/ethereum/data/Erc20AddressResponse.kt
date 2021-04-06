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
class Erc20AddressResponse {
    @JsonProperty("accountHash")
    var accountHash: String = ""
    @JsonProperty("tokenHash")
    var tokenHash: String = ""
    @JsonProperty("balance")
    var balance: BigInteger = 0.toBigInteger()
    @JsonProperty("decimals")
    var decimals: Int = 0
    @JsonProperty("transfers")
    var transfers: List<Erc20TransferResponse> = emptyList()
}
