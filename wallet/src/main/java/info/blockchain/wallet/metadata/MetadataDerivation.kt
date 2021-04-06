package info.blockchain.wallet.metadata

import org.bitcoinj.core.ECKey
import org.bitcoinj.core.NetworkParameters
import org.bitcoinj.core.Utils
import org.bitcoinj.crypto.ChildNumber
import org.bitcoinj.crypto.DeterministicKey
import org.bitcoinj.crypto.HDKeyDerivation
import java.io.UnsupportedEncodingException
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.Arrays

class MetadataDerivation(private val bitcoinParams: NetworkParameters) {

    @Throws(UnsupportedEncodingException::class, NoSuchAlgorithmException::class)
    fun deriveMetadataNode(node: DeterministicKey): String {
        return HDKeyDerivation.deriveChildKey(
            node,
            getPurpose("metadata") or ChildNumber.HARDENED_BIT
        ).serializePrivB58(bitcoinParams)
    }

    @Throws(UnsupportedEncodingException::class, NoSuchAlgorithmException::class)
    fun deriveSharedMetadataNode(node: DeterministicKey?): String {
        return HDKeyDerivation.deriveChildKey(node, getPurpose("mdid") or ChildNumber.HARDENED_BIT)
            .serializePrivB58(bitcoinParams)
    }

    fun deriveAddress(key: ECKey) = key.toAddress(bitcoinParams).toString()

    /**
     * BIP 43 purpose needs to be 31 bit or less. For lack of a BIP number we take the first 31 bits
     * of the SHA256 hash of a reverse domain.
     */
    @Throws(NoSuchAlgorithmException::class, UnsupportedEncodingException::class)
    private fun getPurpose(sub: String): Int {

        val md = MessageDigest.getInstance("SHA-256")
        val text = "info.blockchain.$sub"
        md.update(text.toByteArray(charset("UTF-8")))
        val hash = md.digest()
        val slice = Arrays.copyOfRange(hash, 0, 4)

        return (Utils.readUint32BE(slice, 0) and 0x7FFFFFFF).toInt() // 510742
    }

    fun deserializeMetadataNode(node: String): DeterministicKey =
        DeterministicKey.deserializeB58(
            node,
            bitcoinParams
        )
}