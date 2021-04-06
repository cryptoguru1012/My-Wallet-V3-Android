package piuk.blockchain.androidcore.data.payments

import com.blockchain.android.testutils.rxInit
import com.blockchain.testutils.`should be assignable from`
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.whenever
import info.blockchain.api.data.UnspentOutput
import info.blockchain.api.data.UnspentOutputs
import info.blockchain.balance.CryptoCurrency
import info.blockchain.wallet.api.dust.DustService
import info.blockchain.wallet.api.dust.data.DustInput
import info.blockchain.wallet.exceptions.ApiException
import info.blockchain.wallet.exceptions.TransactionHashApiException
import info.blockchain.wallet.payment.InsufficientMoneyException
import info.blockchain.wallet.payment.Payment
import info.blockchain.wallet.payment.SpendableUnspentOutputs
import io.reactivex.Single
import junit.framework.Assert.assertEquals
import junit.framework.Assert.assertTrue
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.ResponseBody
import org.amshove.kluent.`should be instance of`
import org.amshove.kluent.`should equal`
import org.apache.commons.lang3.tuple.Pair
import org.bitcoinj.core.ECKey
import org.bitcoinj.core.NetworkParameters
import org.bitcoinj.core.Transaction
import org.bitcoinj.params.BitcoinCashMainNetParams
import org.bitcoinj.params.BitcoinMainNetParams
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mockito.atLeastOnce
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoMoreInteractions
import piuk.blockchain.androidcore.data.api.EnvironmentConfig
import retrofit2.Call
import retrofit2.Response
import java.math.BigInteger

class PaymentServiceTest {

    private lateinit var subject: PaymentService
    private val payment: Payment = mock()
    private val environmentSettings: EnvironmentConfig = mock()
    private val dustService: DustService = mock()

    @get:Rule
    val rxSchedulers = rxInit {
        mainTrampoline()
        ioTrampoline()
    }

    @Before
    fun setUp() {
        subject = PaymentService(environmentSettings, payment, dustService)

        whenever(environmentSettings.bitcoinNetworkParameters)
            .thenReturn(BitcoinMainNetParams.get())
        whenever(environmentSettings.bitcoinCashNetworkParameters)
            .thenReturn(BitcoinCashMainNetParams.get())
    }

    @Test
    fun submitPaymentSuccess() {
        // Arrange
        val txHash = "TX_HASH"
        val mockOutputBundle = mock(SpendableUnspentOutputs::class.java)
        val mockOutputs = listOf(mock(UnspentOutput::class.java))
        whenever(mockOutputBundle.spendableOutputs).thenReturn(mockOutputs)
        val mockEcKeys = listOf(mock(ECKey::class.java))
        val toAddress = "TO_ADDRESS"
        val changeAddress = "CHANGE_ADDRESS"
        val mockFee = mock(BigInteger::class.java)
        val mockAmount = mock(BigInteger::class.java)
        val mockTx = mock(Transaction::class.java)
        whenever(mockTx.hashAsString).thenReturn(txHash)
        whenever(
            payment.makeSimpleTransaction(
                eq<NetworkParameters>(environmentSettings.bitcoinNetworkParameters),
                eq(mockOutputs),
                any(),
                eq(mockFee),
                eq(changeAddress)
            )
        )
            .thenReturn(mockTx)
        val mockCall = mock<Call<ResponseBody>>()
        val response = Response.success(mock(ResponseBody::class.java))
        whenever(mockCall.execute()).thenReturn(response)
        whenever(payment.publishSimpleTransaction(mockTx)).thenReturn(mockCall)
        // Act
        val testObserver = subject.submitBtcPayment(
            mockOutputBundle,
            mockEcKeys,
            toAddress,
            changeAddress,
            mockFee,
            mockAmount
        ).test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        assertEquals(txHash, testObserver.values()[0])
        verify(payment).makeSimpleTransaction(
            eq<NetworkParameters>(environmentSettings.bitcoinNetworkParameters),
            eq(mockOutputs),
            any(),
            eq(mockFee),
            eq(changeAddress)
        )
        verify(payment).signSimpleTransaction(
            environmentSettings.bitcoinNetworkParameters,
            mockTx,
            mockEcKeys
        )
        verify(payment).publishSimpleTransaction(mockTx)
        verifyNoMoreInteractions(payment)
        verify(environmentSettings, atLeastOnce()).bitcoinNetworkParameters
        verify(environmentSettings, never()).bitcoinCashNetworkParameters
        verifyNoMoreInteractions(environmentSettings)
    }

    @Test
    fun submitPaymentFailure() {
        // Arrange
        val txHash = "TX_HASH"
        val mockOutputBundle = mock(SpendableUnspentOutputs::class.java)
        val mockOutputs = listOf(mock(UnspentOutput::class.java))
        whenever(mockOutputBundle.spendableOutputs).thenReturn(mockOutputs)
        val mockEcKeys = listOf(mock(ECKey::class.java))
        val toAddress = "TO_ADDRESS"
        val changeAddress = "CHANGE_ADDRESS"
        val mockFee = mock(BigInteger::class.java)
        val mockAmount = mock(BigInteger::class.java)
        val mockTx = mock(Transaction::class.java)
        whenever(mockTx.hashAsString).thenReturn(txHash)
        whenever(
            payment.makeSimpleTransaction(
                eq<NetworkParameters>(environmentSettings.bitcoinNetworkParameters),
                eq(mockOutputs),
                any(),
                eq(mockFee),
                eq(changeAddress)
            )
        )
            .thenReturn(mockTx)
        val mockCall = mock<Call<ResponseBody>>()
        val response = Response.error<ResponseBody>(
            500,
            ResponseBody.create(("application/json").toMediaTypeOrNull(), "{}")
        )
        whenever(mockCall.execute()).thenReturn(response)
        whenever(payment.publishSimpleTransaction(mockTx)).thenReturn(mockCall)
        // Act
        val testObserver = subject.submitBtcPayment(
            mockOutputBundle,
            mockEcKeys,
            toAddress,
            changeAddress,
            mockFee,
            mockAmount
        ).test()
        // Assert
        testObserver.assertNotComplete()
        testObserver.assertTerminated()
        testObserver.assertNoValues()
        testObserver.assertError {
            it `should be instance of` TransactionHashApiException::class
            if (it is TransactionHashApiException) {
                it.message `should equal` "500: {}"
                it.hashString `should equal` "TX_HASH"
            }
            true
        }
        verify(payment).makeSimpleTransaction(
            eq<NetworkParameters>(environmentSettings.bitcoinNetworkParameters),
            eq(mockOutputs),
            any(),
            eq(mockFee),
            eq(changeAddress)
        )
        verify(payment).signSimpleTransaction(
            environmentSettings.bitcoinNetworkParameters,
            mockTx,
            mockEcKeys
        )
        verify(payment).publishSimpleTransaction(mockTx)
        verifyNoMoreInteractions(payment)
        verify(environmentSettings, atLeastOnce()).bitcoinNetworkParameters
        verify(environmentSettings, never()).bitcoinCashNetworkParameters
        verifyNoMoreInteractions(environmentSettings)
    }

    @Test
    fun submitPaymentException() {
        // Arrange
        val txHash = "TX_HASH"
        val mockOutputBundle = mock(SpendableUnspentOutputs::class.java)
        val mockOutputs = listOf(mock(UnspentOutput::class.java))
        whenever(mockOutputBundle.spendableOutputs).thenReturn(mockOutputs)
        val mockEcKeys = listOf(mock(ECKey::class.java))
        val toAddress = "TO_ADDRESS"
        val changeAddress = "CHANGE_ADDRESS"
        val mockFee = mock(BigInteger::class.java)
        val mockAmount = mock(BigInteger::class.java)
        val mockTx = mock(Transaction::class.java)
        whenever(mockTx.hashAsString).thenReturn(txHash)
        whenever(
            payment.makeSimpleTransaction(
                eq<NetworkParameters>(environmentSettings.bitcoinNetworkParameters),
                eq(mockOutputs),
                any(),
                eq(mockFee),
                eq(changeAddress)
            )
        ).thenThrow(InsufficientMoneyException(BigInteger("1")))
        // Act
        val testObserver = subject.submitBtcPayment(
            mockOutputBundle,
            mockEcKeys,
            toAddress,
            changeAddress,
            mockFee,
            mockAmount
        ).test()
        // Assert
        testObserver.assertNotComplete()
        testObserver.assertTerminated()
        testObserver.assertNoValues()
        testObserver.assertError(InsufficientMoneyException::class.java)
        verify(payment).makeSimpleTransaction(
            eq<NetworkParameters>(environmentSettings.bitcoinNetworkParameters),
            eq(mockOutputs),
            any(),
            eq(mockFee),
            eq(changeAddress)
        )
        verifyNoMoreInteractions(payment)
        verify(environmentSettings, atLeastOnce()).bitcoinNetworkParameters
        verify(environmentSettings, never()).bitcoinCashNetworkParameters
        verifyNoMoreInteractions(environmentSettings)
    }

    @Test
    fun submitBchPaymentSuccess() {
        // Arrange
        val txHash = "TX_HASH"
        val mockOutputBundle: SpendableUnspentOutputs = mock()
        val mockOutputs: List<UnspentOutput> = listOf(mock())
        whenever(mockOutputBundle.spendableOutputs).thenReturn(mockOutputs)
        val mockEcKeys: List<ECKey> = listOf(mock())
        val toAddress = "TO_ADDRESS"
        val changeAddress = "CHANGE_ADDRESS"
        val mockFee: BigInteger = mock()
        val mockAmount: BigInteger = mock()
        val mockTx: Transaction = mock()
        val mockDust: DustInput = mock()
        whenever(mockDust.lockSecret).thenReturn("SECRET")
        whenever(mockTx.hashAsString).thenReturn(txHash)
        whenever(
            payment.makeNonReplayableTransaction(
                eq(environmentSettings.bitcoinCashNetworkParameters),
                eq(mockOutputs),
                any(),
                eq(mockFee),
                eq(changeAddress),
                eq(mockDust)
            )
        ).thenReturn(mockTx)
        whenever(dustService.getDust(CryptoCurrency.BCH)).thenReturn(Single.just(mockDust))
        val mockCall: Call<ResponseBody> = mock()
        val response: Response<ResponseBody> = Response.success<ResponseBody>(mock())
        whenever(mockCall.execute()).thenReturn(response)
        whenever(payment.publishTransactionWithSecret(CryptoCurrency.BCH, mockTx, "SECRET"))
            .thenReturn(mockCall)
        // Act
        val testObserver = subject.submitBchPayment(
            mockOutputBundle,
            mockEcKeys,
            toAddress,
            changeAddress,
            mockFee,
            mockAmount
        ).test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        assertEquals(txHash, testObserver.values()[0])
        verify(payment).makeNonReplayableTransaction(
            eq(environmentSettings.bitcoinCashNetworkParameters),
            eq(mockOutputs),
            any(),
            eq(mockFee),
            eq(changeAddress),
            eq(mockDust)
        )
        verify(payment).signBchTransaction(
            environmentSettings.bitcoinCashNetworkParameters,
            mockTx,
            mockEcKeys
        )
        verify(payment).publishTransactionWithSecret(CryptoCurrency.BCH, mockTx, "SECRET")
        verifyNoMoreInteractions(payment)
        verify(environmentSettings, atLeastOnce()).bitcoinCashNetworkParameters
        verify(environmentSettings, never()).bitcoinNetworkParameters
        verifyNoMoreInteractions(environmentSettings)
    }

    @Test
    fun submitBchPaymentFailure() {
        // Arrange
        val txHash = "TX_HASH"
        val mockOutputBundle: SpendableUnspentOutputs = mock()
        val mockOutputs: List<UnspentOutput> = listOf(mock())
        whenever(mockOutputBundle.spendableOutputs).thenReturn(mockOutputs)
        val mockEcKeys: List<ECKey> = listOf(mock())
        val toAddress = "TO_ADDRESS"
        val changeAddress = "CHANGE_ADDRESS"
        val mockFee: BigInteger = mock()
        val mockAmount: BigInteger = mock()
        val mockTx: Transaction = mock()
        val mockDust: DustInput = mock()
        whenever(mockDust.lockSecret).thenReturn("SECRET")
        whenever(mockTx.hashAsString).thenReturn(txHash)
        whenever(
            payment.makeNonReplayableTransaction(
                eq(environmentSettings.bitcoinCashNetworkParameters),
                eq(mockOutputs),
                any(),
                eq(mockFee),
                eq(changeAddress),
                eq(mockDust)
            )
        ).thenReturn(mockTx)
        whenever(dustService.getDust(CryptoCurrency.BCH)).thenReturn(Single.just(mockDust))
        val mockCall: Call<ResponseBody> = mock()
        val response = Response.error<ResponseBody>(
            500,
            ResponseBody.create(("application/json").toMediaTypeOrNull(), "{}")
        )
        whenever(mockCall.execute()).thenReturn(response)
        whenever(payment.publishTransactionWithSecret(CryptoCurrency.BCH, mockTx, "SECRET"))
            .thenReturn(mockCall)
        // Act
        val testObserver = subject.submitBchPayment(
            mockOutputBundle,
            mockEcKeys,
            toAddress,
            changeAddress,
            mockFee,
            mockAmount
        ).test()
        // Assert
        testObserver.assertNotComplete()
        testObserver.assertTerminated()
        testObserver.assertNoValues()
        testObserver.assertError {
            it `should be instance of` TransactionHashApiException::class
            if (it is TransactionHashApiException) {
                it.message `should equal` "500: {}"
                it.hashString `should equal` "TX_HASH"
            }
            true
        }
        verify(payment).makeNonReplayableTransaction(
            eq(environmentSettings.bitcoinCashNetworkParameters),
            eq(mockOutputs),
            any(),
            eq(mockFee),
            eq(changeAddress),
            eq(mockDust)
        )
        verify(payment).signBchTransaction(
            environmentSettings.bitcoinCashNetworkParameters,
            mockTx,
            mockEcKeys
        )
        verify(payment).publishTransactionWithSecret(CryptoCurrency.BCH, mockTx, "SECRET")
        verifyNoMoreInteractions(payment)
        verify(environmentSettings, atLeastOnce()).bitcoinCashNetworkParameters
        verify(environmentSettings, never()).bitcoinNetworkParameters
        verifyNoMoreInteractions(environmentSettings)
    }

    @Test
    fun submitBchPaymentException() {
        // Arrange
        val txHash = "TX_HASH"
        val mockOutputBundle: SpendableUnspentOutputs = mock()
        val mockOutputs: List<UnspentOutput> = listOf(mock())
        whenever(mockOutputBundle.spendableOutputs).thenReturn(mockOutputs)
        val mockEcKeys: List<ECKey> = listOf(mock())
        val toAddress = "TO_ADDRESS"
        val changeAddress = "CHANGE_ADDRESS"
        val mockFee: BigInteger = mock()
        val mockAmount: BigInteger = mock()
        val mockTx: Transaction = mock()
        whenever(mockTx.hashAsString).thenReturn(txHash)
        val mockDust: DustInput = mock()
        whenever(dustService.getDust(CryptoCurrency.BCH)).thenReturn(Single.just(mockDust))
        whenever(
            payment.makeNonReplayableTransaction(
                eq(environmentSettings.bitcoinCashNetworkParameters),
                eq(mockOutputs),
                any(),
                eq(mockFee),
                eq(changeAddress),
                eq(mockDust)
            )
        ).thenThrow(InsufficientMoneyException(BigInteger("1")))
        // Act
        val testObserver = subject.submitBchPayment(
            mockOutputBundle,
            mockEcKeys,
            toAddress,
            changeAddress,
            mockFee,
            mockAmount
        ).test()
        // Assert
        testObserver.assertNotComplete()
        testObserver.assertTerminated()
        testObserver.assertNoValues()
        testObserver.assertError(InsufficientMoneyException::class.java)
        verify(payment).makeNonReplayableTransaction(
            eq(environmentSettings.bitcoinCashNetworkParameters),
            eq(mockOutputs),
            any(),
            eq(mockFee),
            eq(changeAddress),
            eq(mockDust)
        )
        verifyNoMoreInteractions(payment)
        verify(environmentSettings, atLeastOnce()).bitcoinCashNetworkParameters
        verify(environmentSettings, never()).bitcoinNetworkParameters
        verifyNoMoreInteractions(environmentSettings)
    }

    @Test
    fun getUnspentOutputsSuccess() {
        // Arrange
        val address = "ADDRESS"
        val mockCall = mock<Call<UnspentOutputs>>()
        val mockOutputs = mock(UnspentOutputs::class.java)
        val response = Response.success(mockOutputs)
        whenever(mockCall.execute()).thenReturn(response)
        whenever(payment.getUnspentCoins(listOf(address))).thenReturn(mockCall)
        // Act
        val testObserver = subject.getUnspentBtcOutputs(address).test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        assertEquals(mockOutputs, testObserver.values()[0])
        verify(payment).getUnspentCoins(listOf(address))
        verifyNoMoreInteractions(payment)
    }

    @Test
    fun getUnspentOutputs500Error() {
        // Arrange
        val address = "ADDRESS"
        val mockCall = mock<Call<UnspentOutputs>>()
        val response = Response.error<UnspentOutputs>(500, mock(ResponseBody::class.java))
        whenever(mockCall.execute()).thenReturn(response)
        whenever(payment.getUnspentCoins(listOf(address))).thenReturn(mockCall)
        // Act
        val testObserver = subject.getUnspentBtcOutputs(address).test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        assertTrue(testObserver.values()[0].unspentOutputs.isEmpty())
        verify(payment).getUnspentCoins(listOf(address))
        verifyNoMoreInteractions(payment)
    }

    @Test
    fun getUnspentOutputsFailed() {
        // Arrange
        val address = "ADDRESS"
        val mockCall = mock<Call<UnspentOutputs>>()
        val response = Response.error<UnspentOutputs>(404, mock(ResponseBody::class.java))
        whenever(mockCall.execute()).thenReturn(response)
        whenever(payment.getUnspentCoins(listOf(address))).thenReturn(mockCall)
        // Act
        val testObserver = subject.getUnspentBtcOutputs(address).test()
        // Assert
        testObserver.assertNotComplete()
        testObserver.assertTerminated()
        testObserver.assertNoValues()
        testObserver.assertError(ApiException::class.java)
        verify(payment).getUnspentCoins(listOf(address))
        verifyNoMoreInteractions(payment)
    }

    @Test
    fun getUnspentBchOutputsSuccess() {
        // Arrange
        val address = "ADDRESS"
        val mockCall = mock<Call<UnspentOutputs>>()
        val mockOutputs = mock(UnspentOutputs::class.java)
        val response = Response.success(mockOutputs)
        whenever(mockCall.execute()).thenReturn(response)
        whenever(payment.getUnspentBchCoins(listOf(address))).thenReturn(mockCall)
        // Act
        val testObserver = subject.getUnspentBchOutputs(address).test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        assertEquals(mockOutputs, testObserver.values()[0])
        verify(payment).getUnspentBchCoins(listOf(address))
        verifyNoMoreInteractions(payment)
    }

    @Test
    fun getUnspentBchOutputs500Error() {
        // Arrange
        val address = "ADDRESS"
        val mockCall = mock<Call<UnspentOutputs>>()
        val response = Response.error<UnspentOutputs>(500, mock(ResponseBody::class.java))
        whenever(mockCall.execute()).thenReturn(response)
        whenever(payment.getUnspentBchCoins(listOf(address))).thenReturn(mockCall)
        // Act
        val testObserver = subject.getUnspentBchOutputs(address).test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        assertTrue(testObserver.values()[0].unspentOutputs.isEmpty())
        verify(payment).getUnspentBchCoins(listOf(address))
        verifyNoMoreInteractions(payment)
    }

    @Test
    fun getUnspentBchOutputsFailed() {
        // Arrange
        val address = "ADDRESS"
        val mockCall = mock<Call<UnspentOutputs>>()
        val response = Response.error<UnspentOutputs>(404, mock(ResponseBody::class.java))
        whenever(mockCall.execute()).thenReturn(response)
        whenever(payment.getUnspentBchCoins(listOf(address))).thenReturn(mockCall)
        // Act
        val testObserver = subject.getUnspentBchOutputs(address).test()
        // Assert
        testObserver.assertNotComplete()
        testObserver.assertTerminated()
        testObserver.assertNoValues()
        testObserver.assertError(ApiException::class.java)
        verify(payment).getUnspentBchCoins(listOf(address))
        verifyNoMoreInteractions(payment)
    }

    @Test
    fun getSpendableCoins() {
        // Arrange
        val mockUnspent = mock(UnspentOutputs::class.java)
        val mockPayment = mock(BigInteger::class.java)
        val mockFee = mock(BigInteger::class.java)
        val mockOutputs = mock(SpendableUnspentOutputs::class.java)
        whenever(payment.getSpendableCoins(mockUnspent, mockPayment, mockFee, false))
            .thenReturn(mockOutputs)
        // Act
        val result = subject.getSpendableCoins(mockUnspent, mockPayment, mockFee, false)
        // Assert
        assertEquals(mockOutputs, result)
        verify(payment).getSpendableCoins(mockUnspent, mockPayment, mockFee, false)
        verifyNoMoreInteractions(payment)
    }

    @Test
    fun getMaximumAvailable() {
        // Arrange
        val mockUnspent = mock(UnspentOutputs::class.java)
        val mockFee = mock(BigInteger::class.java)
        val mockSweepableCoins = mock<Pair<BigInteger, BigInteger>>()
        whenever(payment.getMaximumAvailable(mockUnspent, mockFee, false))
            .thenReturn(mockSweepableCoins)
        // Act
        val result = subject.getMaximumAvailable(mockUnspent, mockFee, false)
        // Assert
        assertEquals(mockSweepableCoins, result)
        verify(payment).getMaximumAvailable(mockUnspent, mockFee, false)
        verifyNoMoreInteractions(payment)
    }

    @Test
    fun isAdequateFee() {
        // Arrange
        val inputs = 1
        val outputs = 101
        val mockFee = mock(BigInteger::class.java)
        whenever(payment.isAdequateFee(inputs, outputs, mockFee)).thenReturn(false)
        // Act
        val result = subject.isAdequateFee(inputs, outputs, mockFee)
        // Assert
        assertEquals(false, result)
        verify(payment).isAdequateFee(inputs, outputs, mockFee)
        verifyNoMoreInteractions(payment)
    }

    @Test
    fun estimateSize() {
        // Arrange
        val inputs = 1
        val outputs = 101
        val estimatedSize = 1337
        whenever(payment.estimatedSize(inputs, outputs)).thenReturn(estimatedSize)
        // Act
        val result = subject.estimateSize(inputs, outputs)
        // Assert
        assertEquals(estimatedSize, result)
        verify(payment).estimatedSize(inputs, outputs)
        verifyNoMoreInteractions(payment)
    }

    @Test
    fun estimateFee() {
        // Arrange
        val inputs = 1
        val outputs = 101
        val mockFeePerKb = mock(BigInteger::class.java)
        val mockAbsoluteFee = mock(BigInteger::class.java)
        whenever(payment.estimatedFee(inputs, outputs, mockFeePerKb)).thenReturn(mockAbsoluteFee)
        // Act
        val result = subject.estimateFee(inputs, outputs, mockFeePerKb)
        // Assert
        assertEquals(mockAbsoluteFee, result)
        verify(payment).estimatedFee(inputs, outputs, mockFeePerKb)
        verifyNoMoreInteractions(payment)
    }

    @Test
    fun `TransactionHashApiException is also an ApiException`() {
        ApiException::class `should be assignable from` TransactionHashApiException::class
    }
}
