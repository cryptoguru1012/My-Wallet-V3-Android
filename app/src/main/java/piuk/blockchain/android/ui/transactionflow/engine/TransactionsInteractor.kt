package piuk.blockchain.android.ui.transactionflow.engine

import com.blockchain.nabu.datamanagers.CurrencyPair
import com.blockchain.nabu.datamanagers.CustodialWalletManager
import com.blockchain.nabu.datamanagers.EligibilityProvider
import com.blockchain.nabu.datamanagers.repositories.swap.CustodialRepository
import com.blockchain.nabu.models.data.LinkBankTransfer
import com.blockchain.preferences.CurrencyPrefs
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.ExchangeRate
import info.blockchain.balance.Money
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.rxkotlin.Singles
import io.reactivex.rxkotlin.zipWith
import io.reactivex.subjects.PublishSubject
import piuk.blockchain.android.coincore.AddressFactory
import piuk.blockchain.android.coincore.AddressParseError
import piuk.blockchain.android.coincore.AssetAction
import piuk.blockchain.android.coincore.BlockchainAccount
import piuk.blockchain.android.coincore.Coincore
import piuk.blockchain.android.coincore.CryptoAccount
import piuk.blockchain.android.coincore.FiatAccount
import piuk.blockchain.android.coincore.NonCustodialAccount
import piuk.blockchain.android.coincore.PendingTx
import piuk.blockchain.android.coincore.ReceiveAddress
import piuk.blockchain.android.coincore.SingleAccount
import piuk.blockchain.android.coincore.SingleAccountList
import piuk.blockchain.android.coincore.TransactionProcessor
import piuk.blockchain.android.coincore.TransactionTarget
import piuk.blockchain.android.coincore.TxConfirmationValue
import piuk.blockchain.android.coincore.TxValidationFailure
import piuk.blockchain.android.coincore.ValidationState
import piuk.blockchain.android.coincore.fiat.LinkedBanksFactory
import piuk.blockchain.android.ui.transfer.AccountsSorting
import piuk.blockchain.androidcore.utils.extensions.mapList
import timber.log.Timber

class TransactionInteractor(
    private val coincore: Coincore,
    private val addressFactory: AddressFactory,
    private val custodialRepository: CustodialRepository,
    private val custodialWalletManager: CustodialWalletManager,
    private val currencyPrefs: CurrencyPrefs,
    private val eligibilityProvider: EligibilityProvider,
    private val accountsSorting: AccountsSorting,
    private val linkedBanksFactory: LinkedBanksFactory
) {
    private var transactionProcessor: TransactionProcessor? = null
    private val invalidate = PublishSubject.create<Unit>()

    fun invalidateTransaction() =
        Completable.fromAction {
            reset()
            transactionProcessor = null
        }

    fun validatePassword(password: String): Single<Boolean> =
        Single.just(coincore.validateSecondPassword(password))

    fun validateTargetAddress(address: String, asset: CryptoCurrency): Single<ReceiveAddress> =
        addressFactory.parse(address, asset)
            .switchIfEmpty(
                Single.error(
                    TxValidationFailure(ValidationState.INVALID_ADDRESS)
                )
            )
            .onErrorResumeNext { e ->
                if (e.isUnexpectedContractError) {
                    Single.error(TxValidationFailure(ValidationState.ADDRESS_IS_CONTRACT))
                } else {
                    Single.error(e)
                }
            }

    fun initialiseTransaction(
        sourceAccount: BlockchainAccount,
        target: TransactionTarget,
        action: AssetAction
    ): Observable<PendingTx> =
        coincore.createTransactionProcessor(sourceAccount, target, action)
            .doOnSubscribe { Timber.d("!TRANSACTION!> SUBSCRIBE") }
            .doOnSuccess {
                if (transactionProcessor != null)
                    throw IllegalStateException("TxProcessor double init")
            }
            .doOnSuccess { transactionProcessor = it }
            .doOnError {
                Timber.e("!TRANSACTION!> error initialising $it")
            }.flatMapObservable {
                it.initialiseTx()
            }.takeUntil(invalidate)

    val canTransactFiat: Boolean
        get() = transactionProcessor?.canTransactFiat ?: throw IllegalStateException("TxProcessor not initialised")

    fun updateTransactionAmount(amount: Money): Completable =
        transactionProcessor?.updateAmount(amount) ?: throw IllegalStateException("TxProcessor not initialised")

    fun getTargetAccounts(sourceAccount: BlockchainAccount, action: AssetAction): Single<SingleAccountList> =
        when (action) {
            AssetAction.Swap -> swapTargets(sourceAccount as CryptoAccount)
            AssetAction.Sell -> sellTargets(sourceAccount as CryptoAccount)
            AssetAction.FiatDeposit -> linkedBanksFactory.getNonWireTransferBanks().mapList { it }
            AssetAction.Withdraw -> linkedBanksFactory.getAllLinkedBanks().mapList { it }
            else -> coincore.getTransactionTargets(sourceAccount as CryptoAccount, action)
        }

    private fun sellTargets(sourceAccount: CryptoAccount): Single<List<SingleAccount>> {
        val availableFiats =
            custodialWalletManager.getSupportedFundsFiats(currencyPrefs.selectedFiatCurrency)
        val apiPairs = custodialWalletManager.getSupportedBuySellCryptoCurrencies()
            .zipWith(availableFiats) { supportedPairs, fiats ->
                supportedPairs.pairs.filter { fiats.contains(it.fiatCurrency) }
                    .map {
                        CurrencyPair.CryptoToFiatCurrencyPair(
                            it.cryptoCurrency,
                            it.fiatCurrency
                        )
                    }
            }

        return Singles.zip(
            coincore.getTransactionTargets(sourceAccount, AssetAction.Sell),
            apiPairs
        ).map { (accountList, pairs) ->
            accountList.filterIsInstance(FiatAccount::class.java)
                .filter { account ->
                    pairs.any { it.source == sourceAccount.asset && account.fiatCurrency == it.destination }
                }
        }
    }

    private fun swapTargets(sourceAccount: CryptoAccount): Single<List<SingleAccount>> =
        Singles.zip(
            coincore.getTransactionTargets(sourceAccount, AssetAction.Swap),
            custodialRepository.getSwapAvailablePairs(),
            eligibilityProvider.isEligibleForSimpleBuy()
        ).map { (accountList, pairs, eligible) ->
            accountList.filterIsInstance(CryptoAccount::class.java)
                .filter { account ->
                    pairs.any { it.source == sourceAccount.asset && account.asset == it.destination }
                }.filter { account ->
                    eligible or (account is NonCustodialAccount)
                }
        }

    fun getAvailableSourceAccounts(action: AssetAction) =
        when (action) {
            AssetAction.Swap -> {
                coincore.allWalletsWithActions(setOf(action), accountsSorting.sorter())
                    .zipWith(
                        custodialRepository.getSwapAvailablePairs()
                    ).map { (accounts, pairs) ->
                        accounts.filter { account ->
                            (account as? CryptoAccount)?.isAvailableToSwapFrom(pairs) ?: false
                        }
                    }.map {
                        it.map { account -> account as CryptoAccount }
                    }
            }
            AssetAction.FiatDeposit -> {
                linkedBanksFactory.getNonWireTransferBanks()
            }
            else -> throw IllegalStateException("Source account should be preselected for action $action")
        }

    fun verifyAndExecute(secondPassword: String): Completable =
        transactionProcessor?.execute(secondPassword) ?: throw IllegalStateException("TxProcessor not initialised")

    fun modifyOptionValue(newConfirmation: TxConfirmationValue): Completable =
        transactionProcessor?.setOption(newConfirmation) ?: throw IllegalStateException("TxProcessor not initialised")

    fun startFiatRateFetch(): Observable<ExchangeRate> =
        transactionProcessor?.userExchangeRate()?.takeUntil(invalidate) ?: throw IllegalStateException(
            "TxProcessor not initialised"
        )

    fun startTargetRateFetch(): Observable<ExchangeRate> =
        transactionProcessor?.targetExchangeRate()?.takeUntil(invalidate) ?: throw IllegalStateException(
            "TxProcessor not initialised"
        )

    fun validateTransaction(): Completable =
        transactionProcessor?.validateAll() ?: throw IllegalStateException("TxProcessor not initialised")

    fun reset() {
        invalidate.onNext(Unit)
        transactionProcessor?.reset() ?: Timber.i("TxProcessor is not initialised yet")
    }

    fun linkABank(selectedFiat: String): Single<LinkBankTransfer> = custodialWalletManager.linkToABank(selectedFiat)
}

private fun CryptoAccount.isAvailableToSwapFrom(pairs: List<CurrencyPair.CryptoCurrencyPair>): Boolean =
    pairs.any { it.source == this.asset }

private val Throwable.isUnexpectedContractError
    get() = (this is AddressParseError && this.error == AddressParseError.Error.ETH_UNEXPECTED_CONTRACT_ADDRESS)
