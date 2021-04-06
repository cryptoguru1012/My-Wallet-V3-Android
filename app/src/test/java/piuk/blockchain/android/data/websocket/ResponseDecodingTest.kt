package piuk.blockchain.android.data.websocket

import com.google.gson.Gson
import junit.framework.Assert.assertEquals
import org.junit.Test
import piuk.blockchain.android.data.coinswebsocket.models.Coin
import piuk.blockchain.android.data.coinswebsocket.models.Entity
import piuk.blockchain.android.data.coinswebsocket.models.EthBlock
import piuk.blockchain.android.data.coinswebsocket.models.SocketResponse

class ResponseDecodingTest {

    private val gson = Gson()

    @Test
    fun `pong message`() {
        val pongMessage = " {\"success\": true, \"entity\":\"none\", \"coin\":\"none\", \"message\": \"pong\"}"
        assertEquals(
            SocketResponse(
                success = true,
                message = "pong",
                coin = Coin.None
            ), gson.fromJson(pongMessage, SocketResponse::class.java))
    }

    @Test
    fun `error message`() {
        val errorMessage =
            " {\"success\": false, \"entity\":\"account\",\"coin\":\"eth\",\"message\":\"Address xxx" +
                    " is not valid Ethereum address\"}\n"
        assertEquals(
            SocketResponse(
                success = false,
                entity = Entity.Account,
                coin = Coin.ETH,
                message = "Address xxx is not valid Ethereum address"
            ), gson.fromJson(errorMessage, SocketResponse::class.java))
    }

    @Test
    fun `header transaction message`() {
        val errorMessage =
            " {\"coin\":\"eth\",\"entity\":\"header\",\"block\":{\"hash\"" +
                    ":\"0x8458a6bdfc7437fb5511171a570834f4ec851300c4fbcc545720db6cfaff78ee\"" +
                    ",\"number\":8349668,\"parentHash\":\"0xb8636f3f5bd5bdab77018c905293d66cc355e678ed82c6ba" +
                    "adcca27920c68b72\",\"uncles\":[],\"sha3Uncles\":\"0x7147366c13a1b54b\",\"transactionsR" +
                    "oot\":\"0x1dcc4de8dec75d7aab85b567b6ccd41ad312451b948a7413f0a142fd40d49347\",\"sta" +
                    "teRoot\":\"0x698c1d5007f93d87fcccbe90b18cb082d170fae8319cccb1cbc37d80463b7629\",\"nonce\":\"0x" +
                    "a339cf61f7397386dcb68d9328b5194f41aee0c2db0bb6cb37e258ceaf078006\",\"logsBloom\":\"0x1a" +
                    "04540870e6051e21cc06820ac494584140f01059a1008eb408c0015a0ec1a8380a10446c9cc01000308b100a565909" +
                    "21812424286004872489c388cd74c080d214027e82934233cc50150d40043075a7286c24904881010443e188801" +
                    "00210502d150246904c810a2a31a122500821a0581225648e4aa109109b964d41334a92c211390054a2288209049" +
                    "1800cac061941546267030451024614004a1428311a412116436660024e084b8214a0e195208c0858538" +
                    "e8334078d182038048054901b502a0005017181000002142346448e11042595590806a41130095" +
                    "252bea60451c40102042001052006b69288a00c22342a011250088d2413a61a0035a286\",\"diffi" +
                    "culty\":\"2314633248726434\",\"totalDifficulty\":\"11461988108663074596503\",\"extraData\":\"0" +
                    "x5050594520737061726b706f6f6c2d6574682d636e2d687a34\",\"size\":27068,\"miner\":\"0x5a0b54d5dc17e" +
                    "0aadc383d2db43b0a0d3e029c4c\",\"staticReward\":\"2000000000000000000\",\"blockReward\":\"214" +
                    "3124100350408882\",\"totalFees\":\"143124100350408882\",\"gasLi" +
                    "mit\":8009305,\"gasUsed\":7999426,\"transactionCount\":158,\"internalTransacti" +
                    "onCount\":10,\"timestamp\":1565799296}}"
        assertEquals(
            SocketResponse(
                success = true,
                entity = Entity.Header,
                coin = Coin.ETH,
                block = EthBlock(
                    "0x8458a6bdfc7437fb5511171a570834f4ec851300c4fbcc545720db6cfaff78ee",
                    "0xb8636f3f5bd5bdab77018c905293d66cc355e678ed82c6baadcca27920c68b72",
                    "0xa339cf61f7397386dcb68d9328b5194f41aee0c2db0bb6cb37e258ceaf078006",
                    8009305
                )
            ), gson.fromJson(errorMessage, SocketResponse::class.java))
    }
}