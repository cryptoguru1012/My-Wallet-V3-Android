package piuk.blockchain.androidcore.data.payments

import info.blockchain.api.data.UnspentOutputs
import info.blockchain.balance.CryptoCurrency
import info.blockchain.wallet.api.dust.DustService
import info.blockchain.wallet.exceptions.ApiException
import info.blockchain.wallet.exceptions.TransactionHashApiException
import info.blockchain.wallet.payment.Payment
import info.blockchain.wallet.payment.SpendableUnspentOutputs
import io.reactivex.Observable
import io.reactivex.Single
import org.apache.commons.lang3.tuple.Pair
import org.bitcoinj.core.ECKey
import org.bitcoinj.core.Transaction
import piuk.blockchain.androidcore.data.api.EnvironmentConfig
import piuk.blockchain.androidcore.utils.annotations.WebRequest
import java.math.BigInteger
import java.util.HashMap

class PaymentService(
    private val environmentSettings: EnvironmentConfig,
    private val payment: Payment,
    private val dustService: DustService
) {

    /**
     * Submits a BTC payment to a specified Bitcoin address and returns the transaction hash if
     * successful
     *
     * @param unspentOutputBundle UTXO object
     * @param keys A List of elliptic curve keys
     * @param toAddress The Bitcoin address to send the funds to
     * @param changeAddress A change address
     * @param bigIntFee The specified fee amount
     * @param bigIntAmount The actual transaction amount
     * @return An [Observable] wrapping a [String] where the String is the transaction hash
     */
    @WebRequest
    internal fun submitBtcPayment(
        unspentOutputBundle: SpendableUnspentOutputs,
        keys: List<ECKey>,
        toAddress: String,
        changeAddress: String,
        bigIntFee: BigInteger,
        bigIntAmount: BigInteger
    ): Observable<String> = Observable.fromCallable {

        val tx = signAngGetBtcTx(unspentOutputBundle, keys, toAddress, changeAddress, bigIntFee, bigIntAmount)
        val response = payment.publishSimpleTransaction(tx).execute()

        when {
            response.isSuccessful -> tx.hashAsString
            else -> throw TransactionHashApiException.fromResponse(tx.hashAsString, response)
        }
    }

    @WebRequest
    internal fun submitBtcPayment(
        signedTx: Transaction
    ): Single<String> = Single.fromCallable {
        val response = payment.publishSimpleTransaction(signedTx).execute()
        when {
            response.isSuccessful -> signedTx.hashAsString
            else -> throw TransactionHashApiException.fromResponse(signedTx.hashAsString, response)
        }
    }

    internal fun signAngGetBtcTx(
        unspentOutputBundle: SpendableUnspentOutputs,
        keys: List<ECKey>,
        toAddress: String,
        changeAddress: String,
        bigIntFee: BigInteger,
        bigIntAmount: BigInteger
    ): Transaction {
        val receivers = HashMap<String, BigInteger>()
        receivers[toAddress] = bigIntAmount

        val tx = payment.makeSimpleTransaction(
            environmentSettings.bitcoinNetworkParameters,
            unspentOutputBundle.spendableOutputs,
            receivers,
            bigIntFee,
            changeAddress
        )

        payment.signSimpleTransaction(environmentSettings.bitcoinNetworkParameters, tx, keys)
        return tx
    }

    /**
     * Submits a BCH payment to a specified Bitcoin Cash address and returns the transaction hash if
     * successful
     *
     * @param unspentOutputBundle UTXO object
     * @param keys A List of elliptic curve keys
     * @param toAddress The Bitcoin Cash address to send the funds to
     * @param changeAddress A change address
     * @param bigIntFee The specified fee amount
     * @param bigIntAmount The actual transaction amount
     * @return An [Observable] wrapping a [String] where the String is the transaction hash
     */
    @WebRequest
    internal fun submitBchPayment(
        unspentOutputBundle: SpendableUnspentOutputs,
        keys: List<ECKey>,
        toAddress: String,
        changeAddress: String,
        bigIntFee: BigInteger,
        bigIntAmount: BigInteger
    ): Observable<String> = dustService.getDust(CryptoCurrency.BCH)
        .flatMapObservable {
            val receivers = HashMap<String, BigInteger>()
            receivers[toAddress] = bigIntAmount

            val tx = payment.makeNonReplayableTransaction(
                environmentSettings.bitcoinCashNetworkParameters,
                unspentOutputBundle.spendableOutputs,
                receivers,
                bigIntFee,
                changeAddress,
                it
            )

            payment.signBchTransaction(environmentSettings.bitcoinCashNetworkParameters, tx, keys)

            return@flatMapObservable Observable.fromCallable {
                val response = payment.publishTransactionWithSecret(CryptoCurrency.BCH, tx, it.lockSecret).execute()
                when {
                    response.isSuccessful -> tx.hashAsString
                    else -> throw TransactionHashApiException.fromResponse(tx.hashAsString, response)
                }
            }
        }

    /**
     * Returns an [UnspentOutputs] object containing all the unspent outputs for a given
     * Bitcoin address.
     *
     * @param address The BTC address you wish to query, as a String
     * @return An [Observable] wrapping an [UnspentOutputs] object
     */
    @WebRequest
    internal fun getUnspentBtcOutputs(address: String): Observable<UnspentOutputs> {
        return Observable.fromCallable {
            val response = payment.getUnspentCoins(listOf(address)).execute()
            when {
                response.isSuccessful -> response.body()
                response.code() == 500 -> // If no unspent outputs available server responds with 500
                    UnspentOutputs.fromJson("{\"unspent_outputs\":[]}")
                else -> throw ApiException(response.code().toString())
            }
        }
    }

    /**
     * Returns an [UnspentOutputs] object containing all the unspent outputs for a given
     * Bitcoin Cash address. Please note that this method only accepts a valid Base58 (ie Legacy)
     * BCH address. BECH32 is not accepted by the endpoint.
     *
     * @param address The BCH address you wish to query, as a Base58 address String
     * @return An [Observable] wrapping an [UnspentOutputs] object
     */
    @WebRequest
    internal fun getUnspentBchOutputs(address: String): Observable<UnspentOutputs> {
        return Observable.fromCallable {
            val response = payment.getUnspentBchCoins(listOf(address)).execute()

            when {
                response.isSuccessful -> response.body()
                response.code() == 500 -> // If no unspent outputs available server responds with 500
                    UnspentOutputs.fromJson("{\"unspent_outputs\":[]}")
                else -> throw ApiException(response.code().toString())
            }
        }
    }

    /**
     * Returns a [SpendableUnspentOutputs] object from a given [UnspentOutputs] object,
     * given the payment amount and the current fee per kB. This method selects the minimum number
     * of inputs necessary to allow a successful payment by selecting from the largest inputs
     * first.
     *
     * @param unspentCoins The addresses' [UnspentOutputs]
     * @param paymentAmount The amount you wish to send, as a [BigInteger]
     * @param feePerKb The current fee per kB, as a [BigInteger]
     * @param includeReplayProtection Whether or not you intend on adding a dust input for replay protection. This is
     * an extra input and therefore affects the transaction fee.
     * @return An [SpendableUnspentOutputs] object, which wraps a list of spendable outputs
     * for the given inputs
     */
    internal fun getSpendableCoins(
        unspentCoins: UnspentOutputs,
        paymentAmount: BigInteger,
        feePerKb: BigInteger,
        includeReplayProtection: Boolean
    ): SpendableUnspentOutputs =
        payment.getSpendableCoins(unspentCoins, paymentAmount, feePerKb, includeReplayProtection)

    /**
     * Calculates the total amount of bitcoin that can be swept from an [UnspentOutputs]
     * object and returns the amount that can be recovered, along with the fee (in absolute terms)
     * necessary to sweep those coins.
     *
     * @param unspentCoins An [UnspentOutputs] object that you wish to sweep
     * @param feePerKb The current fee per kB on the network
     * @param includeReplayProtection Whether or not you intend on adding a dust input for replay protection. This is
     * an extra input and therefore affects the transaction fee.
     * @return A [Pair] object, where left = the sweepable amount as a [BigInteger],
     * right = the absolute fee needed to sweep those coins, also as a [BigInteger]
     */
    internal fun getMaximumAvailable(
        unspentCoins: UnspentOutputs,
        feePerKb: BigInteger,
        includeReplayProtection: Boolean
    ): Pair<BigInteger, BigInteger> = payment.getMaximumAvailable(
        unspentCoins,
        feePerKb,
        includeReplayProtection
    )

    /**
     * Returns true if the `absoluteFee` is adequate for the number of inputs/outputs in the
     * transaction.
     *
     * @param inputs The number of inputs
     * @param outputs The number of outputs
     * @param absoluteFee The absolute fee as a [BigInteger]
     * @return True if the fee is adequate, false if not
     */
    internal fun isAdequateFee(inputs: Int, outputs: Int, absoluteFee: BigInteger): Boolean =
        payment.isAdequateFee(inputs, outputs, absoluteFee)

    /**
     * Returns the estimated size of the transaction in kB.
     *
     * @param inputs The number of inputs
     * @param outputs The number of outputs
     * @return The estimated size of the transaction in kB
     */
    internal fun estimateSize(inputs: Int, outputs: Int): Int =
        payment.estimatedSize(inputs, outputs)

    /**
     * Returns an estimated absolute fee in satoshis (as a [BigInteger] for a given number of
     * inputs and outputs.
     *
     * @param inputs The number of inputs
     * @param outputs The number of outputs
     * @param feePerKb The current fee per kB om the network
     * @return A [BigInteger] representing the absolute fee
     */
    internal fun estimateFee(inputs: Int, outputs: Int, feePerKb: BigInteger): BigInteger =
        payment.estimatedFee(inputs, outputs, feePerKb)
}
