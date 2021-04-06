package info.blockchain.wallet.api.data

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import java.util.ArrayList

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
class Coinify {
    @JsonProperty("countries")
    val countries: List<String> = ArrayList()
    @JsonProperty("partnerId")
    val partnerId: Int? = null
    @JsonProperty("iSignThisDomain")
    val iSignThisDomain: String? = null
}
