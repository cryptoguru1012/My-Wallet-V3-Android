package piuk.blockchain.android.ui.dashboard

import com.blockchain.logging.CrashLogger
import com.blockchain.nabu.datamanagers.CustodialWalletManager
import com.blockchain.nabu.datamanagers.custodialwalletimpl.PaymentMethodType
import com.blockchain.nabu.models.data.LinkBankTransfer
import com.blockchain.notifications.analytics.Analytics
import com.blockchain.preferences.CurrencyPrefs
import com.blockchain.preferences.SimpleBuyPrefs
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.ExchangeRates
import info.blockchain.balance.Money
import info.blockchain.wallet.payload.PayloadManager
import info.blockchain.wallet.prices.TimeAgo
import info.blockchain.wallet.prices.TimeInterval
import info.blockchain.wallet.prices.data.PriceDatum
import io.reactivex.Maybe
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.Singles
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import piuk.blockchain.android.coincore.AccountGroup
import piuk.blockchain.android.coincore.AssetAction
import piuk.blockchain.android.coincore.AssetFilter
import piuk.blockchain.android.coincore.AssetOrdering
import piuk.blockchain.android.coincore.Coincore
import piuk.blockchain.android.coincore.CryptoAccount
import piuk.blockchain.android.coincore.CryptoAsset
import piuk.blockchain.android.coincore.FiatAccount
import piuk.blockchain.android.coincore.SingleAccount
import piuk.blockchain.android.coincore.fiat.LinkedBankAccount
import piuk.blockchain.android.coincore.fiat.LinkedBanksFactory
import piuk.blockchain.android.simplebuy.SimpleBuyAnalytics
import piuk.blockchain.android.ui.dashboard.assetdetails.AssetDetailsFlow
import piuk.blockchain.android.ui.settings.LinkablePaymentMethods
import piuk.blockchain.android.ui.transactionflow.TransactionFlow
import piuk.blockchain.androidcore.data.exchangerate.TimeSpan
import piuk.blockchain.androidcore.utils.extensions.emptySubscribe
import timber.log.Timber
import java.util.concurrent.TimeUnit

private class DashboardGroupLoadFailure(msg: String, e: Throwable) : Exception(msg, e)
private class DashboardBalanceLoadFailure(msg: String, e: Throwable) : Exception(msg, e)

class DashboardInteractor(
    private val coincore: Coincore,
    private val payloadManager: PayloadManager,
    private val exchangeRates: ExchangeRates,
    private val currencyPrefs: CurrencyPrefs,
    private val custodialWalletManager: CustodialWalletManager,
    private val simpleBuyPrefs: SimpleBuyPrefs,
    private val analytics: Analytics,
    private val crashLogger: CrashLogger,
    private val linkedBanksFactory: LinkedBanksFactory,
    private val assetOrdering: AssetOrdering
) {

    // We have a problem here, in that pax init depends on ETH init
    // Ultimately, we want to init metadata straight after decrypting (or creating) the wallet
    // but we can't move that somewhere sensible yet, because 2nd password. When we remove that -
    // which is on the radar - then we can clean up the entire app init sequence.
    // But for now, we'll catch any pax init failure here, unless ETH has initialised OK. And when we
    // get a valid ETH balance, will try for a PX balance. Yeah, this is a nasty hack TODO: Fix this
    fun refreshBalances(model: DashboardModel, balanceFilter: AssetFilter, state: DashboardState): Disposable {
        val cd = CompositeDisposable()

        state.assetMapKeys
            .filter { !it.hasFeature(CryptoCurrency.IS_ERC20) }
            .forEach { asset ->
                cd += refreshAssetBalance(asset, model, balanceFilter)
                    .ifEthLoadedGetErc20Balance(model, balanceFilter, cd, state)
                    .ifEthFailedThenErc20Failed(asset, model, state)
                    .emptySubscribe()
            }

        cd += checkForFiatBalances(model, currencyPrefs.selectedFiatCurrency)

        return cd
    }

    fun getAvailableAssets(model: DashboardModel): Disposable =
        assetOrdering.getAssetOrdering().subscribeBy(
            onSuccess = { assetOrder ->
                val assets = coincore.cryptoAssets.map { enabledAssets ->
                    (enabledAssets as CryptoAsset).asset
                }

                val sortedAssets = assets.sortedBy { assetOrder.indexOf(it) }

                model.process(UpdateDashboardCurrencies(sortedAssets))
                model.process(RefreshAllIntent)
            },
            onError = {
                Timber.e("Error getting ordering - $it")
            }
        )

    private fun refreshAssetBalance(
        asset: CryptoCurrency,
        model: DashboardModel,
        balanceFilter: AssetFilter
    ): Single<CryptoValue> =
        coincore[asset].accountGroup(balanceFilter)
            .logGroupLoadError(asset, balanceFilter)
            .flatMapSingle { group ->
                group.accountBalance
                    .logBalanceLoadError(asset, balanceFilter)
            }
            .map { balance -> balance as CryptoValue }
            .doOnError { e ->
                Timber.e("Failed getting balance for ${asset.networkTicker}: $e")
                model.process(BalanceUpdateError(asset))
            }
            .doOnSuccess { v ->
                Timber.d("Got balance for ${asset.networkTicker}")
                model.process(BalanceUpdate(asset, v))
            }
            .retryOnError()

    private fun <T> Single<T>.retryOnError() =
        this.retryWhen { f ->
            f.take(RETRY_COUNT)
                .delay(RETRY_INTERVAL_MS, TimeUnit.MILLISECONDS)
        }

    private fun Single<CryptoValue>.ifEthLoadedGetErc20Balance(
        model: DashboardModel,
        balanceFilter: AssetFilter,
        disposables: CompositeDisposable,
        state: DashboardState
    ) = this.doOnSuccess { value ->
        if (value.currency == CryptoCurrency.ETHER) {
            state.erc20Assets.forEach {
                disposables += refreshAssetBalance(it, model, balanceFilter)
                    .emptySubscribe()
            }
        }
    }

    private fun Single<CryptoValue>.ifEthFailedThenErc20Failed(
        asset: CryptoCurrency,
        model: DashboardModel,
        state: DashboardState
    ) = this.doOnError {
        if (asset == CryptoCurrency.ETHER) {
            state.erc20Assets.forEach {
                model.process(BalanceUpdateError(it))
            }
        }
    }

    private fun Maybe<AccountGroup>.logGroupLoadError(asset: CryptoCurrency, filter: AssetFilter) =
        this.doOnError { e ->
            crashLogger.logException(
                DashboardGroupLoadFailure("Cannot load group for ${asset.networkTicker} - $filter:", e)
            )
        }

    private fun Single<Money>.logBalanceLoadError(asset: CryptoCurrency, filter: AssetFilter) =
        this.doOnError { e ->
            crashLogger.logException(
                DashboardBalanceLoadFailure("Cannot load balance for ${asset.networkTicker} - $filter:", e)
            )
        }

    private fun checkForFiatBalances(model: DashboardModel, fiatCurrency: String): Disposable =
        coincore.fiatAssets.accountGroup()
            .flattenAsObservable { g -> g.accounts }
            .flatMapSingle { a ->
                a.accountBalance.map { balance ->
                    FiatBalanceInfo(
                        balance,
                        balance.toFiat(exchangeRates, fiatCurrency),
                        a as FiatAccount
                    )
                }
            }
            .toList()
            .subscribeBy(
                onSuccess = { balances ->
                    if (balances.isNotEmpty()) {
                        model.process(FiatBalanceUpdate(balances))
                    }
                },
                onError = {
                    Timber.e("Error while loading fiat balances $it")
                }
            )

    fun refreshPrices(model: DashboardModel, crypto: CryptoCurrency): Disposable {

        return Singles.zip(
            coincore[crypto].exchangeRate(),
            coincore[crypto].historicRate(TimeAgo.ONE_DAY.epoch)
        ) { rate, day -> PriceUpdate(crypto, rate, day) }
            .subscribeBy(
                onSuccess = { model.process(it) },
                onError = { Timber.e(it) }
            )
    }

    fun refreshPriceHistory(model: DashboardModel, crypto: CryptoCurrency): Disposable =
        if (crypto.hasFeature(CryptoCurrency.PRICE_CHARTING)) {
            coincore[crypto].historicRateSeries(TimeSpan.DAY, TimeInterval.ONE_HOUR)
        } else {
            Single.just(FLATLINE_CHART)
        }
            .map { PriceHistoryUpdate(crypto, it) }
            .subscribeBy(
                onSuccess = { model.process(it) },
                onError = { Timber.e(it) }
            )

    fun checkForCustodialBalance(model: DashboardModel, crypto: CryptoCurrency): Disposable? {
        return coincore[crypto].accountGroup(AssetFilter.Custodial)
            .flatMapSingle { it.accountBalance }
            .subscribeBy(
                onSuccess = { model.process(UpdateHasCustodialBalanceIntent(crypto, !it.isZero)) },
                onError = { model.process(UpdateHasCustodialBalanceIntent(crypto, false)) }
            )
    }

    fun hasUserBackedUp(): Single<Boolean> = Single.just(payloadManager.isWalletBackedUp)

    fun cancelSimpleBuyOrder(orderId: String): Disposable {
        return custodialWalletManager.deleteBuyOrder(orderId)
            .subscribeBy(
                onComplete = { simpleBuyPrefs.clearState() },
                onError = { error ->
                    analytics.logEvent(SimpleBuyAnalytics.BANK_DETAILS_CANCEL_ERROR)
                    Timber.e(error)
                }
            )
    }

    fun getSendFlow(
        model: DashboardModel,
        fromAccount: SingleAccount,
        action: AssetAction
    ): Disposable? {
        if (fromAccount is CryptoAccount) {
            model.process(
                UpdateLaunchDialogFlow(
                    TransactionFlow(
                        sourceAccount = fromAccount,
                        action = action
                    )
                )
            )
        }
        return null
    }

    fun getAssetDetailsFlow(model: DashboardModel, cryptoCurrency: CryptoCurrency): Disposable? {
        model.process(
            UpdateLaunchDialogFlow(
                AssetDetailsFlow(
                    cryptoCurrency = cryptoCurrency,
                    coincore = coincore
                )
            )
        )
        return null
    }

    fun getInterestDepositFlow(
        model: DashboardModel,
        sourceAccount: SingleAccount,
        targetAccount: SingleAccount,
        action: AssetAction
    ): Disposable? {
        if (sourceAccount is CryptoAccount) {
            model.process(
                UpdateLaunchDialogFlow(
                    TransactionFlow(
                        sourceAccount = sourceAccount,
                        target = targetAccount,
                        action = action
                    )
                )
            )
        }
        return null
    }

    fun getBankDepositFlow(
        model: DashboardModel,
        targetAccount: SingleAccount,
        action: AssetAction,
        shouldLaunchBankLinkTransfer: Boolean
    ): Disposable {
        require(targetAccount is FiatAccount)
        return Singles.zip(
            linkedBanksFactory.eligibleBankPaymentMethods(targetAccount.fiatCurrency).map { paymentMethods ->
                // Ignore any WireTransferMethods In case BankLinkTransfer should launch
                paymentMethods.filter { it == PaymentMethodType.BANK_TRANSFER || !shouldLaunchBankLinkTransfer }
            },
            linkedBanksFactory.getNonWireTransferBanks().map {
                it.filter { bank -> bank.currency == targetAccount.fiatCurrency }
            }
        ).flatMap { (paymentMethods, linkedBanks) ->
            when {
                linkedBanks.isEmpty() -> {
                    handleNoLinkedBanks(
                        targetAccount,
                        LinkablePaymentMethodsForAction.LinkablePaymentMethodsForDeposit(
                            linkablePaymentMethods = LinkablePaymentMethods(
                                targetAccount.fiatCurrency,
                                paymentMethods
                            )
                        )
                    )
                }
                linkedBanks.size == 1 -> {
                    Single.just(FiatTransactionRequestResult.LaunchDepositFlow(linkedBanks[0]))
                }
                else -> {
                    Single.just(FiatTransactionRequestResult.LaunchDepositFlowWithMultipleAccounts)
                }
            }
        }.subscribeBy(
            onSuccess = {
                handlePaymentMethodsUpdate(it, model, targetAccount, action)
            },
            onError = {
                // TODO Add error state to Dashboard
            }
        )
    }

    private fun handlePaymentMethodsUpdate(
        it: FiatTransactionRequestResult?,
        model: DashboardModel,
        fiatAccount: SingleAccount,
        action: AssetAction
    ) {
        when (it) {
            is FiatTransactionRequestResult.LaunchDepositFlowWithMultipleAccounts -> {
                model.process(
                    UpdateLaunchDialogFlow(
                        TransactionFlow(
                            target = fiatAccount,
                            action = action
                        )
                    )
                )
            }
            is FiatTransactionRequestResult.LaunchDepositFlow -> {
                model.process(
                    UpdateLaunchDialogFlow(
                        TransactionFlow(
                            target = fiatAccount,
                            sourceAccount = it.preselectedBankAccount,
                            action = action
                        )
                    )
                )
            }
            is FiatTransactionRequestResult.LaunchWithdrawalFlowWithMultipleAccounts -> {
                model.process(
                    UpdateLaunchDialogFlow(
                        TransactionFlow(
                            sourceAccount = fiatAccount,
                            action = action
                        )
                    )
                )
            }
            is FiatTransactionRequestResult.LaunchWithdrawalFlow -> {
                model.process(
                    UpdateLaunchDialogFlow(
                        TransactionFlow(
                            sourceAccount = fiatAccount,
                            target = it.preselectedBankAccount,
                            action = action
                        )
                    )
                )
            }
            is FiatTransactionRequestResult.LaunchBankLink -> {
                model.process(
                    LaunchBankLinkFlow(
                        it.linkBankTransfer,
                        action
                    )
                )
            }
            is FiatTransactionRequestResult.NotSupportedPartner -> {
                // TODO Show an error
            }
            is FiatTransactionRequestResult.LaunchPaymentMethodChooser -> {
                model.process(
                    ShowLinkablePaymentMethodsSheet(it.paymentMethodForAction)
                )
            }
            is FiatTransactionRequestResult.LaunchDepositDetailsSheet -> {
                model.process(ShowBankLinkingSheet(it.targetAccount))
            }
        }
    }

    private fun handleNoLinkedBanks(
        targetAccount: FiatAccount,
        paymentMethodForAction: LinkablePaymentMethodsForAction
    ) =
        when {
            paymentMethodForAction.linkablePaymentMethods.linkMethods.containsAll(
                listOf(PaymentMethodType.BANK_TRANSFER, PaymentMethodType.BANK_ACCOUNT)
            ) -> {
                Single.just(
                    FiatTransactionRequestResult.LaunchPaymentMethodChooser(
                        paymentMethodForAction
                    )
                )
            }
            paymentMethodForAction.linkablePaymentMethods.linkMethods.contains(PaymentMethodType.BANK_TRANSFER) -> {
                linkBankTransfer(targetAccount.fiatCurrency).map {
                    FiatTransactionRequestResult.LaunchBankLink(it) as FiatTransactionRequestResult
                }.onErrorReturn {
                    FiatTransactionRequestResult.NotSupportedPartner
                }
            }
            paymentMethodForAction.linkablePaymentMethods.linkMethods.contains(PaymentMethodType.BANK_ACCOUNT) -> {
                Single.just(FiatTransactionRequestResult.LaunchDepositDetailsSheet(targetAccount))
            }
            else -> {
                Single.just(FiatTransactionRequestResult.NotSupportedPartner)
            }
        }

    fun linkBankTransfer(currency: String): Single<LinkBankTransfer> =
        custodialWalletManager.linkToABank(currency)

    fun getBankWithdrawalFlow(
        model: DashboardModel,
        sourceAccount: SingleAccount,
        action: AssetAction,
        shouldLaunchBankLinkTransfer: Boolean
    ): Disposable {
        require(sourceAccount is FiatAccount)

        return Singles.zip(
            linkedBanksFactory.eligibleBankPaymentMethods(sourceAccount.fiatCurrency).map { paymentMethods ->
                // Ignore any WireTransferMethods In case BankLinkTransfer should launch
                paymentMethods.filter { it == PaymentMethodType.BANK_TRANSFER || !shouldLaunchBankLinkTransfer }
            },
            linkedBanksFactory.getAllLinkedBanks().map {
                it.filter { bank -> bank.currency == sourceAccount.fiatCurrency }
            }
        ).flatMap { (paymentMethods, linkedBanks) ->
            when {
                linkedBanks.isEmpty() -> {
                    handleNoLinkedBanks(
                        sourceAccount,
                        LinkablePaymentMethodsForAction.LinkablePaymentMethodsForWithdraw(
                            LinkablePaymentMethods(
                                sourceAccount.fiatCurrency,
                                paymentMethods
                            )
                        )
                    )
                }
                linkedBanks.size == 1 -> {
                    Single.just(FiatTransactionRequestResult.LaunchWithdrawalFlow(linkedBanks[0]))
                }
                else -> {
                    Single.just(FiatTransactionRequestResult.LaunchWithdrawalFlowWithMultipleAccounts)
                }
            }
        }.subscribeBy(
            onSuccess = {
                handlePaymentMethodsUpdate(it, model, sourceAccount, action)
            },
            onError = {
                // TODO Add error state to Dashboard
            }
        )
    }

    companion object {
        private val FLATLINE_CHART = listOf(
            PriceDatum(price = 1.0, timestamp = 0),
            PriceDatum(price = 1.0, timestamp = System.currentTimeMillis() / 1000)
        )

        private const val RETRY_INTERVAL_MS = 3000L
        private const val RETRY_COUNT = 3L
    }
}

private sealed class FiatTransactionRequestResult {
    class LaunchBankLink(val linkBankTransfer: LinkBankTransfer) : FiatTransactionRequestResult()
    class LaunchDepositFlow(val preselectedBankAccount: LinkedBankAccount) : FiatTransactionRequestResult()
    class LaunchPaymentMethodChooser(val paymentMethodForAction: LinkablePaymentMethodsForAction) :
        FiatTransactionRequestResult()

    class LaunchDepositDetailsSheet(val targetAccount: FiatAccount) : FiatTransactionRequestResult()
    object LaunchDepositFlowWithMultipleAccounts : FiatTransactionRequestResult()
    class LaunchWithdrawalFlow(val preselectedBankAccount: LinkedBankAccount) : FiatTransactionRequestResult()
    object LaunchWithdrawalFlowWithMultipleAccounts : FiatTransactionRequestResult()
    object NotSupportedPartner : FiatTransactionRequestResult()
}
