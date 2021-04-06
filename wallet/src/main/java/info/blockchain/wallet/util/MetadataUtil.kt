package info.blockchain.wallet.util

import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.UnsupportedEncodingException
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.Arrays
import org.bitcoinj.core.Sha256Hash
import org.bitcoinj.core.Utils
import org.bitcoinj.crypto.ChildNumber
import org.bitcoinj.crypto.DeterministicKey
import org.bitcoinj.crypto.HDKeyDerivation

object MetadataUtil {

    @Throws(UnsupportedEncodingException::class, NoSuchAlgorithmException::class)
    fun deriveMetadataNode(node: DeterministicKey): DeterministicKey {
        return HDKeyDerivation.deriveChildKey(
            node,
            getPurpose("metadata") or ChildNumber.HARDENED_BIT
        )
    }

    @Throws(IOException::class)
    fun message(payload: ByteArray, prevMagicHash: ByteArray?): ByteArray {
        return if (prevMagicHash == null)
            payload
        else {
            val payloadHash = Sha256Hash.hash(payload)

            val outputStream = ByteArrayOutputStream()
            outputStream.write(prevMagicHash)
            outputStream.write(payloadHash)

            outputStream.toByteArray()
        }
    }

    @Throws(IOException::class)
    fun magic(payload: ByteArray, prevMagicHash: ByteArray?): ByteArray {
        val msg = message(payload, prevMagicHash)
        return magicHash(msg)
    }

    private fun magicHash(message: ByteArray): ByteArray {
        val messageBytes = Utils.formatMessageForSigning(Base64Util.encodeBase64String(message))
        return Sha256Hash.hashTwice(messageBytes)
    }

    fun deriveHardened(node: DeterministicKey, type: Int): DeterministicKey {
        return HDKeyDerivation.deriveChildKey(node, type or ChildNumber.HARDENED_BIT)
    }

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
}
