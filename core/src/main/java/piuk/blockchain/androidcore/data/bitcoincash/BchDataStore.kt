package piuk.blockchain.androidcore.data.bitcoincash

import info.blockchain.wallet.BitcoinCashWallet
import info.blockchain.wallet.coin.GenericMetadataWallet
import piuk.blockchain.androidcore.data.datastores.SimpleDataStore
import java.math.BigInteger

/**
 * A simple data store class to cache the Bitcoin cash Wallet (bitcoin chain M/44H/0H)
 */
class BchDataStore : SimpleDataStore {

    var bchWallet: BitcoinCashWallet? = null
    var bchMetadata: GenericMetadataWallet? = null
    val bchBalances = hashMapOf<String, BigInteger>()

    override fun clearData() {
        bchWallet = null
        bchMetadata = null
        bchBalances.clear()
    }
}