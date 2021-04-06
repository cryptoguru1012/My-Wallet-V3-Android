package piuk.blockchain.android.coincore.impl.txEngine

import com.blockchain.android.testutils.rxInit
import com.blockchain.nabu.datamanagers.CustodialWalletManager
import com.blockchain.nabu.models.data.WithdrawalFeeAndLimit
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.verifyNoMoreInteractions
import com.nhaarman.mockito_kotlin.whenever
import info.blockchain.balance.FiatValue
import io.reactivex.Completable
import io.reactivex.Single
import org.amshove.kluent.itReturns
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import piuk.blockchain.android.coincore.CryptoAccount
import piuk.blockchain.android.coincore.FeeLevel
import piuk.blockchain.android.coincore.FiatAccount
import piuk.blockchain.android.coincore.PendingTx
import piuk.blockchain.android.coincore.TxResult
import piuk.blockchain.android.coincore.ValidationState
import piuk.blockchain.android.coincore.fiat.LinkedBankAccount
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateDataManager

class FiatWithdrawalTxEngineTest {

    @get:Rule
    val initSchedulers = rxInit {
        mainTrampoline()
        ioTrampoline()
        computationTrampoline()
    }

    private lateinit var subject: FiatWithdrawalTxEngine
    private val walletManager: CustodialWalletManager = mock()
    private val exchangeRates: ExchangeRateDataManager = mock()

    @Before
    fun setup() {
        subject = FiatWithdrawalTxEngine(walletManager)
    }

    @Test
    fun `inputs validate when correct`() {
        val sourceAccount: FiatAccount = mock()
        val txTarget: LinkedBankAccount = mock()

        // Act
        subject.start(
            sourceAccount,
            txTarget,
            exchangeRates
        )

        subject.assertInputsValid()

        verifyNoMoreInteractions(txTarget)
        verifyNoMoreInteractions(sourceAccount)
    }

    @Test(expected = IllegalStateException::class)
    fun `inputs fail validation when source Account incorrect`() {
        val sourceAccount: CryptoAccount = mock()
        val txTarget: LinkedBankAccount = mock()

        // Act
        subject.start(
            sourceAccount,
            txTarget,
            exchangeRates
        )

        subject.assertInputsValid()
    }

    @Test(expected = IllegalStateException::class)
    fun `inputs fail validation when target account incorrect`() {
        val sourceAccount: FiatAccount = mock()
        val txTarget: CryptoAccount = mock()

        // Act
        subject.start(
            sourceAccount,
            txTarget,
            exchangeRates
        )

        subject.assertInputsValid()
    }

    @Test
    fun `PendingTx is correctly initialised`() {
        val zeroFiat = FiatValue.zero(SELECTED_FIAT)
        val expectedBalance = FiatValue.fromMinor(TGT_ASSET, 10000L)
        val expectedAccountBalance = FiatValue.fromMinor(TGT_ASSET, 100000L)
        val sourceAccount: FiatAccount = mock {
            on { fiatCurrency } itReturns SELECTED_FIAT
            on { actionableBalance } itReturns Single.just(expectedBalance)
            on { accountBalance } itReturns Single.just(expectedAccountBalance)
        }

        val expectedMinAmountAndFee = WithdrawalFeeAndLimit(
            minLimit = FiatValue.fromMinor(TGT_ASSET, 100L),
            fee = FiatValue.fromMinor(TGT_ASSET, 1000L)
        )

        val txTarget: LinkedBankAccount = mock {
            on { fiatCurrency } itReturns TGT_ASSET
            on { getWithdrawalFeeAndMinLimit() } itReturns Single.just(expectedMinAmountAndFee)
        }

        // Act
        subject.start(
            sourceAccount,
            txTarget,
            exchangeRates
        )

        subject.doInitialiseTx()
            .test()
            .assertValue {
                it.amount == zeroFiat &&
                    it.totalBalance == expectedAccountBalance &&
                    it.availableBalance == expectedBalance &&
                    it.fees == expectedMinAmountAndFee.fee &&
                    it.selectedFiat == SELECTED_FIAT &&
                    it.customFeeAmount == -1L &&
                    it.confirmations.isEmpty() &&
                    it.minLimit == expectedMinAmountAndFee.minLimit &&
                    it.maxLimit == expectedBalance &&
                    it.validationState == ValidationState.UNINITIALISED &&
                    it.engineState.isEmpty()
            }
            .assertValue { verifyFeeLevels(it, FeeLevel.None) }
            .assertNoErrors()
            .assertComplete()
    }

    @Test
    fun `update amount modifies the pendingTx correctly`() {
        val sourceAccount: FiatAccount = mock {
            on { fiatCurrency } itReturns SELECTED_FIAT
        }
        val txTarget: LinkedBankAccount = mock()

        subject.start(
            sourceAccount,
            txTarget,
            exchangeRates
        )

        val zeroFiat = FiatValue.zero(SELECTED_FIAT)
        val pendingTx = PendingTx(
            amount = zeroFiat,
            totalBalance = zeroFiat,
            availableBalance = zeroFiat,
            fees = zeroFiat,
            selectedFiat = SELECTED_FIAT,
            feeLevel = FeeLevel.None,
            availableFeeLevels = setOf(FeeLevel.None)
        )

        val inputAmount = FiatValue.fromMinor(SELECTED_FIAT, 1000L)

        subject.doUpdateAmount(
            inputAmount,
            pendingTx
        ).test()
            .assertComplete()
            .assertNoErrors()
            .assertValue {
                it.amount == inputAmount &&
                    it.totalBalance == zeroFiat &&
                    it.availableBalance == zeroFiat &&
                    it.fees == zeroFiat
            }
            .assertValue { verifyFeeLevels(it, FeeLevel.None) }
    }

    @Test
    fun `validate amount when pendingTx uninitialised`() {
        val sourceAccount: FiatAccount = mock {
            on { fiatCurrency } itReturns SELECTED_FIAT
        }
        val txTarget: LinkedBankAccount = mock()

        subject.start(
            sourceAccount,
            txTarget,
            exchangeRates
        )

        val zeroFiat = FiatValue.zero(SELECTED_FIAT)
        val pendingTx = PendingTx(
            amount = zeroFiat,
            validationState = ValidationState.UNINITIALISED,
            totalBalance = zeroFiat,
            availableBalance = zeroFiat,
            fees = zeroFiat,
            selectedFiat = SELECTED_FIAT,
            feeLevel = FeeLevel.None,
            availableFeeLevels = setOf(FeeLevel.None)
        )

        subject.doValidateAmount(
            pendingTx
        ).test()
            .assertComplete()
            .assertNoErrors()
            .assertValue {
                it.amount == zeroFiat &&
                    it.totalBalance == zeroFiat &&
                    it.availableBalance == zeroFiat &&
                    it.fees == zeroFiat
            }
            .assertValue { verifyFeeLevels(it, FeeLevel.None) }
    }

    @Test
    fun `validate amount when limits not set`() {
        val sourceAccount: FiatAccount = mock {
            on { fiatCurrency } itReturns SELECTED_FIAT
        }
        val txTarget: LinkedBankAccount = mock()

        subject.start(
            sourceAccount,
            txTarget,
            exchangeRates
        )

        val amount = FiatValue.fromMinor(SELECTED_FIAT, 1000L)
        val zeroFiat = FiatValue.zero(SELECTED_FIAT)
        val pendingTx = PendingTx(
            amount = amount,
            totalBalance = zeroFiat,
            availableBalance = zeroFiat,
            fees = zeroFiat,
            selectedFiat = SELECTED_FIAT,
            feeLevel = FeeLevel.None,
            availableFeeLevels = setOf(FeeLevel.None),
            minLimit = null,
            maxLimit = null
        )

        subject.doValidateAmount(
            pendingTx
        ).test()
            .assertComplete()
            .assertValue {
                it.validationState == ValidationState.UNKNOWN_ERROR
            }
    }

    @Test
    fun `validate amount when under min limit`() {
        val sourceAccount: FiatAccount = mock {
            on { fiatCurrency } itReturns SELECTED_FIAT
        }
        val txTarget: LinkedBankAccount = mock()

        subject.start(
            sourceAccount,
            txTarget,
            exchangeRates
        )

        val amount = FiatValue.fromMinor(SELECTED_FIAT, 1000L)
        val minLimit = FiatValue.fromMinor(SELECTED_FIAT, 2000L)
        val maxLimit = FiatValue.fromMinor(SELECTED_FIAT, 10000L)

        val zeroFiat = FiatValue.zero(SELECTED_FIAT)
        val pendingTx = PendingTx(
            amount = amount,
            totalBalance = zeroFiat,
            availableBalance = zeroFiat,
            fees = zeroFiat,
            selectedFiat = SELECTED_FIAT,
            feeLevel = FeeLevel.None,
            availableFeeLevels = setOf(FeeLevel.None),
            minLimit = minLimit,
            maxLimit = maxLimit
        )

        subject.doValidateAmount(
            pendingTx
        ).test()
            .assertComplete()
            .assertValue {
                it.validationState == ValidationState.UNDER_MIN_LIMIT
            }
    }

    @Test
    fun `validate amount when over max limit`() {
        val sourceAccount: FiatAccount = mock {
            on { fiatCurrency } itReturns SELECTED_FIAT
        }
        val txTarget: LinkedBankAccount = mock()

        subject.start(
            sourceAccount,
            txTarget,
            exchangeRates
        )

        val amount = FiatValue.fromMinor(SELECTED_FIAT, 1000000L)
        val minLimit = FiatValue.fromMinor(SELECTED_FIAT, 2000L)
        val maxLimit = FiatValue.fromMinor(SELECTED_FIAT, 10000L)

        val zeroFiat = FiatValue.zero(SELECTED_FIAT)
        val pendingTx = PendingTx(
            amount = amount,
            totalBalance = zeroFiat,
            availableBalance = zeroFiat,
            fees = zeroFiat,
            selectedFiat = SELECTED_FIAT,
            feeLevel = FeeLevel.None,
            availableFeeLevels = setOf(FeeLevel.None),
            minLimit = minLimit,
            maxLimit = maxLimit
        )

        subject.doValidateAmount(
            pendingTx
        ).test()
            .assertComplete()
            .assertValue {
                it.validationState == ValidationState.OVER_MAX_LIMIT
            }
    }

    @Test
    fun `validate amount when over available balance`() {
        val sourceAccount: FiatAccount = mock {
            on { fiatCurrency } itReturns SELECTED_FIAT
        }
        val txTarget: LinkedBankAccount = mock()

        subject.start(
            sourceAccount,
            txTarget,
            exchangeRates
        )

        val amount = FiatValue.fromMinor(SELECTED_FIAT, 3000L)
        val minLimit = FiatValue.fromMinor(SELECTED_FIAT, 2000L)
        val maxLimit = FiatValue.fromMinor(SELECTED_FIAT, 10000L)

        val zeroFiat = FiatValue.zero(SELECTED_FIAT)
        val pendingTx = PendingTx(
            amount = amount,
            totalBalance = zeroFiat,
            availableBalance = zeroFiat,
            fees = zeroFiat,
            selectedFiat = SELECTED_FIAT,
            feeLevel = FeeLevel.None,
            availableFeeLevels = setOf(FeeLevel.None),
            minLimit = minLimit,
            maxLimit = maxLimit
        )

        subject.doValidateAmount(
            pendingTx
        ).test()
            .assertComplete()
            .assertValue {
                it.validationState == ValidationState.INSUFFICIENT_FUNDS
            }
    }

    @Test
    fun `validate amount when correct`() {
        val sourceAccount: FiatAccount = mock {
            on { fiatCurrency } itReturns SELECTED_FIAT
        }
        val txTarget: LinkedBankAccount = mock()

        subject.start(
            sourceAccount,
            txTarget,
            exchangeRates
        )

        val balance = FiatValue.fromMinor(SELECTED_FIAT, 4000L)
        val amount = FiatValue.fromMinor(SELECTED_FIAT, 3000L)
        val minLimit = FiatValue.fromMinor(SELECTED_FIAT, 2000L)
        val maxLimit = FiatValue.fromMinor(SELECTED_FIAT, 10000L)

        val zeroFiat = FiatValue.zero(SELECTED_FIAT)
        val pendingTx = PendingTx(
            amount = amount,
            totalBalance = balance,
            availableBalance = balance,
            fees = zeroFiat,
            selectedFiat = SELECTED_FIAT,
            feeLevel = FeeLevel.None,
            availableFeeLevels = setOf(FeeLevel.None),
            minLimit = minLimit,
            maxLimit = maxLimit
        )

        subject.doValidateAmount(
            pendingTx
        ).test()
            .assertComplete()
            .assertNoErrors()
            .assertValue {
                it.amount == pendingTx.amount &&
                    it.minLimit == pendingTx.minLimit &&
                    it.maxLimit == pendingTx.maxLimit &&
                    it.validationState == ValidationState.CAN_EXECUTE
            }
    }

    @Test
    fun `executing tx works`() {
        val bankAccountAddress = LinkedBankAccount.BankAccountAddress("address", "label")
        val sourceAccount: FiatAccount = mock {
            on { fiatCurrency } itReturns SELECTED_FIAT
        }
        val txTarget: LinkedBankAccount = mock {
            on { receiveAddress } itReturns Single.just(bankAccountAddress)
        }

        subject.start(
            sourceAccount,
            txTarget,
            exchangeRates
        )

        val amount = FiatValue.fromMinor(SELECTED_FIAT, 3000L)
        val minLimit = FiatValue.fromMinor(SELECTED_FIAT, 2000L)
        val maxLimit = FiatValue.fromMinor(SELECTED_FIAT, 10000L)

        val zeroFiat = FiatValue.zero(SELECTED_FIAT)
        val pendingTx = PendingTx(
            amount = amount,
            totalBalance = zeroFiat,
            availableBalance = zeroFiat,
            fees = zeroFiat,
            selectedFiat = SELECTED_FIAT,
            feeLevel = FeeLevel.None,
            availableFeeLevels = setOf(FeeLevel.None),
            minLimit = minLimit,
            maxLimit = maxLimit
        )

        whenever(walletManager.createWithdrawOrder(amount, bankAccountAddress.address)).thenReturn(
            Completable.complete()
        )

        subject.doExecute(
            pendingTx, ""
        ).test()
            .assertComplete()
            .assertNoErrors()
            .assertValue {
                it is TxResult.UnHashedTxResult &&
                    it.amount == pendingTx.amount
            }

        verify(walletManager).createWithdrawOrder(amount, bankAccountAddress.address)
    }

    @Test
    fun `executing tx throws exception`() {
        val bankAccountAddress = LinkedBankAccount.BankAccountAddress("address", "label")
        val sourceAccount: FiatAccount = mock {
            on { fiatCurrency } itReturns SELECTED_FIAT
        }
        val txTarget: LinkedBankAccount = mock {
            on { receiveAddress } itReturns Single.just(bankAccountAddress)
        }

        subject.start(
            sourceAccount,
            txTarget,
            exchangeRates
        )

        val amount = FiatValue.fromMinor(SELECTED_FIAT, 3000L)
        val minLimit = FiatValue.fromMinor(SELECTED_FIAT, 2000L)
        val maxLimit = FiatValue.fromMinor(SELECTED_FIAT, 10000L)

        val zeroFiat = FiatValue.zero(SELECTED_FIAT)
        val pendingTx = PendingTx(
            amount = amount,
            totalBalance = zeroFiat,
            availableBalance = zeroFiat,
            fees = zeroFiat,
            selectedFiat = SELECTED_FIAT,
            feeLevel = FeeLevel.None,
            availableFeeLevels = setOf(FeeLevel.None),
            minLimit = minLimit,
            maxLimit = maxLimit
        )

        val exception = IllegalStateException("")
        whenever(walletManager.createWithdrawOrder(amount, bankAccountAddress.address)).thenReturn(
            Completable.error(exception)
        )

        subject.doExecute(
            pendingTx, ""
        ).test()
            .assertError {
                it == exception
            }

        verify(walletManager).createWithdrawOrder(amount, bankAccountAddress.address)
    }

    private fun verifyFeeLevels(pendingTx: PendingTx, expectedLevel: FeeLevel) =
        pendingTx.feeLevel == expectedLevel &&
            pendingTx.availableFeeLevels == setOf(FeeLevel.None) &&
            pendingTx.availableFeeLevels.contains(pendingTx.feeLevel)

    companion object {
        private const val SELECTED_FIAT = "USD"
        private const val TGT_ASSET = "USD"
    }
}