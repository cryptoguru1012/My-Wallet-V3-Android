package com.blockchain.sunriver

import com.blockchain.koin.payloadScope
import com.blockchain.koin.sunriverModule
import com.blockchain.network.initRule
import com.blockchain.testutils.after
import com.blockchain.testutils.before
import com.blockchain.testutils.bitcoin
import com.blockchain.testutils.getStringFromResource
import com.blockchain.testutils.lumens
import com.blockchain.testutils.stroops
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.Money
import info.blockchain.balance.withMajorValue
import io.fabric8.mockwebserver.DefaultMockServer
import org.amshove.kluent.`should be instance of`
import org.amshove.kluent.`should be less than`
import org.amshove.kluent.`should be`
import org.amshove.kluent.`should equal`
import org.amshove.kluent.`should not be`
import org.amshove.kluent.`should throw`
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.koin.core.context.startKoin
import org.koin.test.AutoCloseKoinTest
import org.stellar.sdk.CreateAccountOperation
import org.stellar.sdk.KeyPair
import org.stellar.sdk.Memo
import org.stellar.sdk.PaymentOperation
import org.stellar.sdk.requests.ErrorResponse
import org.stellar.sdk.responses.operations.CreateAccountOperationResponse
import org.stellar.sdk.responses.operations.PaymentOperationResponse
import java.util.Locale

class HorizonProxyTest : AutoCloseKoinTest() {

    private val server = DefaultMockServer()

    @get:Rule
    val initMockServer = server.initRule()

    @get:Rule
    val ensureNoLocalization = before {
        Locale.setDefault(Locale.FRANCE)
    } after {
        Locale.setDefault(Locale.US)
    }

    @Before
    fun setUp() {
        startKoin {
            properties(mapOf("HorizonURL" to server.url("")))
            modules(listOf(
                sunriverModule
            ))
        }
        server.requestCount
    }

    @Test
    fun `get xlm balance`() {
        server.expect().get().withPath("/accounts/GC7GSOOQCBBWNUOB6DIWNVM7537UKQ353H6LCU3DB54NUTVFR2T6OHF4")
            .andReturn(
                200,
                getStringFromResource("accounts/GC7GSOOQCBBWNUOB6DIWNVM7537UKQ353H6LCU3DB54NUTVFR2T6OHF4.json")
            )
            .once()

        val proxy = payloadScope.get<HorizonProxy>().apply {
            update(server.url(""))
        }

        val balance =
            proxy.getBalance("GC7GSOOQCBBWNUOB6DIWNVM7537UKQ353H6LCU3DB54NUTVFR2T6OHF4")

        balance `should equal` 109969.99997.lumens()
    }

    @Test
    fun `get xlm balance and min`() {
        server.expect().get().withPath("/accounts/GC7GSOOQCBBWNUOB6DIWNVM7537UKQ353H6LCU3DB54NUTVFR2T6OHF4")
            .andReturn(
                200,
                getStringFromResource("accounts/GC7GSOOQCBBWNUOB6DIWNVM7537UKQ353H6LCU3DB54NUTVFR2T6OHF4.json")
            )
            .once()

        val proxy = payloadScope.get<HorizonProxy>().apply {
            update(server.url(""))
        }

        proxy.getBalanceAndMin("GC7GSOOQCBBWNUOB6DIWNVM7537UKQ353H6LCU3DB54NUTVFR2T6OHF4").apply {
            balance `should equal` 109969.99997.lumens()
            minimumBalance `should equal` 1.lumens()
        }
    }

    @Test
    fun `get xlm balance and min, account with 5x subentries`() {
        server.expect().get().withPath("/accounts/GC3OI356MOU4VUR4SMTSALQBI6HFCGKSSBWO5EMZABMN5AF3L3K6B6BK")
            .andReturn(
                200,
                getStringFromResource("accounts/GC3OI356MOU4VUR4SMTSALQBI6HFCGKSSBWO5EMZABMN5AF3L3K6B6BK.json")
            )
            .once()

        val proxy = payloadScope.get<HorizonProxy>().apply {
            update(server.url(""))
        }

        proxy.getBalanceAndMin("GC3OI356MOU4VUR4SMTSALQBI6HFCGKSSBWO5EMZABMN5AF3L3K6B6BK").apply {
            balance `should equal` 100.lumens()
            minimumBalance `should equal` ((2 + 5) * 0.5).lumens()
        }
    }

    @Test
    fun `get balance if account does not exist`() {
        server.expect().get().withPath("/accounts/GC7GSOOQCBBWNUOB6DIWNVM7537UKQ353H6LCU3DB54NUTVFR2T6OHF4")
            .andReturn(
                404,
                getStringFromResource("accounts/not_found.json")
            )
            .once()

        val proxy = payloadScope.get<HorizonProxy>().apply {
            update(server.url(""))
        }

        val balance =
            proxy.getBalance("GC7GSOOQCBBWNUOB6DIWNVM7537UKQ353H6LCU3DB54NUTVFR2T6OHF4")

        balance `should equal` 0.lumens()
    }

    @Test
    fun `get balance and min if account does not exist`() {
        server.expect().get().withPath("/accounts/GC7GSOOQCBBWNUOB6DIWNVM7537UKQ353H6LCU3DB54NUTVFR2T6OHF4")
            .andReturn(
                404,
                getStringFromResource("accounts/not_found.json")
            )
            .once()

        val proxy = payloadScope.get<HorizonProxy>().apply {
            update(server.url(""))
        }

        proxy.getBalanceAndMin("GC7GSOOQCBBWNUOB6DIWNVM7537UKQ353H6LCU3DB54NUTVFR2T6OHF4")
            .apply {
                balance `should equal` 0.lumens()
                minimumBalance `should equal` 0.lumens()
            }
    }

    @Test
    fun `on any other kind of server error, bubble up exception`() {
        server.expect().get().withPath("/accounts/GC7GSOOQCBBWNUOB6DIWNVM7537UKQ353H6LCU3DB54NUTVFR2T6OHF4")
            .andReturn(
                301,
                getStringFromResource("accounts/not_found.json")
            )
            .once()

        val proxy = payloadScope.get<HorizonProxy>().apply {
            update(server.url(""))
        };

        {
            proxy.getBalance("GC7GSOOQCBBWNUOB6DIWNVM7537UKQ353H6LCU3DB54NUTVFR2T6OHF4")
        } `should throw` ErrorResponse::class
    }

    @Test
    fun `get xlm transaction history`() {
        server.expect().get().withPath(
            "/accounts/GC7GSOOQCBBWNUOB6DIWNVM7537UKQ353H6LCU3DB54NUTVFR2T6OHF4/operations?order=desc&limit=50"
        ).andReturn(
            200,
            getStringFromResource("transactions/transaction_list.json")
        )
            .once()

        val proxy = payloadScope.get<HorizonProxy>().apply {
            update(server.url(""))
        }

        val transactions =
            proxy.getTransactionList("GC7GSOOQCBBWNUOB6DIWNVM7537UKQ353H6LCU3DB54NUTVFR2T6OHF4")

        transactions.size `should equal` 3
        transactions[0] `should be instance of` CreateAccountOperationResponse::class.java
        transactions[1] `should be instance of` PaymentOperationResponse::class.java
        transactions[2] `should be instance of` PaymentOperationResponse::class.java
    }

    @Test
    fun `get xlm transaction history if not found`() {
        server.expect().get().withPath(
            "/accounts/GC7GSOOQCBBWNUOB6DIWNVM7537UKQ353H6LCU3DB54NUTVFR2T6OHF4/operations?order=desc&limit=50"
        ).andReturn(
            404,
            getStringFromResource("accounts/not_found.json")
        )
            .once()

        val proxy = payloadScope.get<HorizonProxy>().apply {
            update(server.url(""))
        }

        val transactions =
            proxy.getTransactionList("GC7GSOOQCBBWNUOB6DIWNVM7537UKQ353H6LCU3DB54NUTVFR2T6OHF4")

        transactions.size `should equal` 0
    }

    @Test
    fun `get xlm transaction history, on any other kind of server error, bubble up exception`() {
        server.expect().get().withPath(
            "/accounts/GC7GSOOQCBBWNUOB6DIWNVM7537UKQ353H6LCU3DB54NUTVFR2T6OHF4/operations?order=desc&limit=50"
        ).andReturn(
            301,
            getStringFromResource("accounts/not_found.json")
        )
            .once()

        val proxy = payloadScope.get<HorizonProxy>().apply {
            update(server.url(""))
        };

        {
            proxy.getTransactionList("GC7GSOOQCBBWNUOB6DIWNVM7537UKQ353H6LCU3DB54NUTVFR2T6OHF4")
        } `should throw` ErrorResponse::class
    }

    @Test
    fun `get specific transaction by hash`() {
        server.expect().get().withPath("/transactions/2dcb356e88d0c778a0c5ed8d33543f167994744ed0019b96553c310449133aba")
            .andReturn(
                200,
                getStringFromResource("transactions/single_transaction.json")
            )
            .once()

        val proxy = payloadScope.get<HorizonProxy>().apply {
            update(server.url(""))
        }

        val transaction =
            proxy.getTransaction("2dcb356e88d0c778a0c5ed8d33543f167994744ed0019b96553c310449133aba")

        transaction.feeCharged `should equal` 100L
    }

    @Test
    fun `accountExists - get account existence`() {
        server.givenAccountExists("GC7GSOOQCBBWNUOB6DIWNVM7537UKQ353H6LCU3DB54NUTVFR2T6OHF4")

        val proxy = payloadScope.get<HorizonProxy>().apply {
            update(server.url(""))
        }

        proxy.accountExists("GC7GSOOQCBBWNUOB6DIWNVM7537UKQ353H6LCU3DB54NUTVFR2T6OHF4") `should be` true
    }

    @Test
    fun `accountExists - get account non-existence`() {
        server.givenAccountDoesNotExist("GC7GSOOQCBBWNUOB6DIWNVM7537UKQ353H6LCU3DB54NUTVFR2T6OHF4")

        val proxy = payloadScope.get<HorizonProxy>().apply {
            update(server.url(""))
        }

        proxy.accountExists("GC7GSOOQCBBWNUOB6DIWNVM7537UKQ353H6LCU3DB54NUTVFR2T6OHF4") `should be` false
    }

    @Test
    fun `accountExists - on any other kind of server error, bubble up exception`() {
        server.expect().get().withPath("/accounts/GC7GSOOQCBBWNUOB6DIWNVM7537UKQ353H6LCU3DB54NUTVFR2T6OHF4")
            .andReturn(
                301,
                getStringFromResource("accounts/not_found.json")
            )
            .once()

        val proxy = payloadScope.get<HorizonProxy>().apply {
            update(server.url(""))
        };

        {
            proxy.accountExists("GC7GSOOQCBBWNUOB6DIWNVM7537UKQ353H6LCU3DB54NUTVFR2T6OHF4")
        } `should throw` ErrorResponse::class
    }

    @Test
    fun `can send transaction to an account that exists`() {
        server.givenAccountExists("GC7GSOOQCBBWNUOB6DIWNVM7537UKQ353H6LCU3DB54NUTVFR2T6OHF4")
        server.givenAccountExists("GCO724H2FOHPBFF4OQ6IB5GB3CVE4W3UGDY4RIHHG6UPQ2YZSSCINMAI")
        server.givenRootResponse()
        server.givenPostWillBeSuccessful()

        val proxy = payloadScope.get<HorizonProxy>().apply {
            update(server.url(""))
        }

        val source = KeyPair.fromSecretSeed("SAD6LOTFMPIGAPOF2SPQSYD4OIGIE5XVVX3FW3K7QVFUTRSUUHMZQ76I")
        source.accountId `should equal` "GC7GSOOQCBBWNUOB6DIWNVM7537UKQ353H6LCU3DB54NUTVFR2T6OHF4"

        proxy.sendTransaction(
            source,
            "GCO724H2FOHPBFF4OQ6IB5GB3CVE4W3UGDY4RIHHG6UPQ2YZSSCINMAI",
            123.4567891.lumens(),
            10
        ).apply {
            success `should be` true
            success `should be` true
            transaction `should not be` null
            transaction!!.operations.single().apply {
                this `should be instance of` PaymentOperation::class
                (this as PaymentOperation).apply {
                    destination `should equal` "GCO724H2FOHPBFF4OQ6IB5GB3CVE4W3UGDY4RIHHG6UPQ2YZSSCINMAI"
                    amount `should equal` "123.4567891"
                }
            }
            transaction.fee `should equal` 100L
        }

        server.requestCount `should be` 5
    }

    @Test
    fun `can dry run send transaction to an account that exists`() {
        server.givenAccountExists("GC7GSOOQCBBWNUOB6DIWNVM7537UKQ353H6LCU3DB54NUTVFR2T6OHF4")
        server.givenAccountExists("GCO724H2FOHPBFF4OQ6IB5GB3CVE4W3UGDY4RIHHG6UPQ2YZSSCINMAI")

        server.givenPostWillBeSuccessful()

        val proxy = payloadScope.get<HorizonProxy>().apply {
            update(server.url(""))
        }

        val source = KeyPair.fromSecretSeed("SAD6LOTFMPIGAPOF2SPQSYD4OIGIE5XVVX3FW3K7QVFUTRSUUHMZQ76I")
        source.accountId `should equal` "GC7GSOOQCBBWNUOB6DIWNVM7537UKQ353H6LCU3DB54NUTVFR2T6OHF4"

        proxy.dryRunTransaction(
            source,
            "GCO724H2FOHPBFF4OQ6IB5GB3CVE4W3UGDY4RIHHG6UPQ2YZSSCINMAI",
            123.4567891.lumens(),
            Memo.none()
        ).apply {
            success `should be` true
            success `should be` true
            transaction `should not be` null
            transaction!!.operations.single().apply {
                this `should be instance of` PaymentOperation::class
                (this as PaymentOperation).apply {
                    destination `should equal` "GCO724H2FOHPBFF4OQ6IB5GB3CVE4W3UGDY4RIHHG6UPQ2YZSSCINMAI"
                    amount `should equal` "123.4567891"
                }
            }
            transaction.fee `should equal` 100L
        }

        server.requestCount `should be` 2
    }

    private val fee = 100.stroops()
    private val minimumBalance = 1.lumens()

    @Test
    fun `insufficient funds that we know about before transaction send - whole balance`() {
        assertFailsAndTransactionIsNotSentToHorizon(
            value = 109969.999970.lumens(),
            expectedReason = HorizonProxy.FailureReason.InsufficientFunds,
            expectedFailureValue = 109969.999970.lumens() - minimumBalance - fee
        )
    }

    @Test
    fun `insufficient funds that we know about before transaction send - whole balance - fee`() {
        assertFailsAndTransactionIsNotSentToHorizon(
            value = 109969.999970.lumens() - fee,
            expectedReason = HorizonProxy.FailureReason.InsufficientFunds,
            expectedFailureValue = 109969.999970.lumens() - minimumBalance - fee
        )
    }

    @Test
    fun `insufficient funds by 1 stoop`() {
        val maximum = 109969.999970.lumens() - fee - minimumBalance
        assertFailsAndTransactionIsNotSentToHorizon(
            value = maximum + 1.stroops(),
            expectedReason = HorizonProxy.FailureReason.InsufficientFunds,
            expectedFailureValue = maximum
        )
    }

    @Test
    fun `absolute minimum balance can be sent`() {
        assertSendPasses(109969.999970.lumens() - fee - minimumBalance, expectedRequests = 5)
    }

    @Test
    fun `existing account - minimum send`() {
        assertSendPasses(minimumBalance, destinationAccountExists = true, expectedRequests = 5)
    }

    @Test
    fun `non-existing account - minimum send`() {
        assertSendPasses(minimumBalance, destinationAccountExists = false, expectedRequests = 4)
    }

    @Test
    fun `existing account - no lower limit on send`() {
        assertSendPasses(1.stroops(), destinationAccountExists = true, expectedRequests = 5)
    }

    @Test
    fun `existing account - can't send zero`() {
        assertFailsAndTransactionIsNotSentToHorizon(
            value = 0.lumens(),
            destinationAccountExists = true,
            expectedReason = HorizonProxy.FailureReason.BelowMinimumSend,
            expectedFailureValue = 1.stroops()
        )
        server.assertNoInteractions()
    }

    @Test
    fun `existing account - can't send negative`() {
        assertFailsAndTransactionIsNotSentToHorizon(
            value = (-1).stroops(),
            destinationAccountExists = true,
            expectedReason = HorizonProxy.FailureReason.BelowMinimumSend,
            expectedFailureValue = 1.stroops()
        )
        server.assertNoInteractions()
    }

    @Test
    fun `non-existing account - can't send zero`() {
        assertFailsAndTransactionIsNotSentToHorizon(
            value = 0.lumens(),
            destinationAccountExists = false,
            expectedReason = HorizonProxy.FailureReason.BelowMinimumSend,
            expectedFailureValue = 1.stroops()
        )
        server.assertNoInteractions()
    }

    @Test
    fun `non-existing account - can't send zero - dryRun`() {
        assertDryRunFailsAndTransactionIsNotSentToHorizon(
            value = 0.lumens(),
            destinationAccountExists = false,
            expectedReason = HorizonProxy.FailureReason.BelowMinimumSend,
            expectedFailureValue = 1.stroops()
        )
        server.assertNoInteractions()
    }

    @Test
    fun `non-existing account - can't send negative`() {
        assertFailsAndTransactionIsNotSentToHorizon(
            value = (-1).stroops(),
            destinationAccountExists = false,
            expectedReason = HorizonProxy.FailureReason.BelowMinimumSend,
            expectedFailureValue = 1.stroops()
        )
        server.assertNoInteractions()
    }

    @Test
    fun `non-existing account - can't send negative - dryRun`() {
        assertDryRunFailsAndTransactionIsNotSentToHorizon(
            value = (-1).stroops(),
            destinationAccountExists = false,
            expectedReason = HorizonProxy.FailureReason.BelowMinimumSend,
            expectedFailureValue = 1.stroops()
        )
        server.assertNoInteractions()
    }

    @Test
    fun `non-existing account - minimum send less 1 stroop fails`() {
        assertFailsAndTransactionIsNotSentToHorizon(
            value = minimumBalance - 1.stroops(),
            destinationAccountExists = false,
            expectedReason = HorizonProxy.FailureReason.BelowMinimumBalanceForNewAccount,
            expectedFailureValue = 1.lumens()
        )
        server.requestCount `should be` 1
    }

    @Test
    fun `non-existing account - minimum send less 1 stroop fails - dryRun`() {
        assertDryRunFailsAndTransactionIsNotSentToHorizon(
            value = minimumBalance - 1.stroops(),
            destinationAccountExists = false,
            expectedReason = HorizonProxy.FailureReason.BelowMinimumBalanceForNewAccount,
            expectedFailureValue = 1.lumens()
        )
        server.requestCount `should be` 1
    }

    @Test
    fun `account with 5 subentries - insufficient funds`() {
        val maximum = 100.lumens() - ((2 + 5) * 0.5).lumens() - fee
        assertFailsAndTransactionIsNotSentToHorizon(
            value = maximum + 1.stroops(),
            destinationAccountExists = true,
            expectedReason = HorizonProxy.FailureReason.InsufficientFunds,
            expectedFailureValue = maximum,
            sourceAccount = KeyPair.fromSecretSeed("SDRURR3G4FSIRO6ZD2UEPY3OAVT6MFBUO7I3DPG2QLUD6CPKTMHEC557")
        )
        server.requestCount `should be` 2
    }

    @Test
    fun `account with 5 subentries - insufficient funds - dryRun`() {
        val maximum = 100.lumens() - ((2 + 5) * 0.5).lumens() - fee
        assertDryRunFailsAndTransactionIsNotSentToHorizon(
            value = maximum + 1.stroops(),
            destinationAccountExists = true,
            expectedReason = HorizonProxy.FailureReason.InsufficientFunds,
            expectedFailureValue = maximum,
            sourceAccount = KeyPair.fromSecretSeed("SDRURR3G4FSIRO6ZD2UEPY3OAVT6MFBUO7I3DPG2QLUD6CPKTMHEC557")
        )
        server.requestCount `should be` 2
    }

    @Test
    fun `account with 5 subentries - sufficient funds`() {
        assertSendPasses(
            value = 100.lumens() - ((2 + 5) * 0.5).lumens() - fee,
            destinationAccountExists = true,
            sourceAccount = KeyPair.fromSecretSeed("SDRURR3G4FSIRO6ZD2UEPY3OAVT6MFBUO7I3DPG2QLUD6CPKTMHEC557"),
            expectedRequests = 5
        )
    }

    @Test
    fun `account with 5 subentries - sufficient funds - dryRun`() {
        assertDryRunSaysSendShouldPass(
            value = 100.lumens() - ((2 + 5) * 0.5).lumens() - fee,
            destinationAccountExists = true,
            sourceAccount = KeyPair.fromSecretSeed("SDRURR3G4FSIRO6ZD2UEPY3OAVT6MFBUO7I3DPG2QLUD6CPKTMHEC557")
        )
        server.requestCount `should be` 2
    }

    @Test
    fun `can't send non-lumen amount`() {
        {
            assertFailsAndTransactionIsNotSentToHorizon(100.bitcoin())
        } `should throw` IllegalArgumentException::class
        server.assertNoInteractions()
    }

    @Test
    fun `can't send non-lumen amount - dryRun`() {
        {
            assertDryRunFailsAndTransactionIsNotSentToHorizon(100.bitcoin())
        } `should throw` IllegalArgumentException::class
        server.assertNoInteractions()
    }

    /**
     * Ensures that the request is never sent to Horizon as that would cost the fee
     */
    private fun assertFailsAndTransactionIsNotSentToHorizon(
        value: Money,
        destinationAccountExists: Boolean = true,
        expectedReason: HorizonProxy.FailureReason? = null,
        expectedFailureValue: Money? = null,
        sourceAccount: KeyPair = KeyPair.fromSecretSeed(
            "SAD6LOTFMPIGAPOF2SPQSYD4OIGIE5XVVX3FW3K7QVFUTRSUUHMZQ76I"
        ),
        destinationAccount: KeyPair = KeyPair.fromAccountId(
            "GCO724H2FOHPBFF4OQ6IB5GB3CVE4W3UGDY4RIHHG6UPQ2YZSSCINMAI"
        )
    ) {
        server.givenAccounts(
            sourceAccountId = sourceAccount.accountId,
            destinationAccountId = destinationAccount.accountId,
            destinationAccountExists = destinationAccountExists
        )

        val proxy = payloadScope.get<HorizonProxy>().apply {
            update(server.url(""))
        }

        proxy.sendTransaction(
            sourceAccount,
            destinationAccount.accountId,
            value as CryptoValue,
            10
        ).apply {
            success `should be` false
            transaction `should be` null
            if (expectedReason != null) {
                failureReason `should be` expectedReason
            }
            failureValue `should equal` expectedFailureValue
        }

        server.requestCount `should be less than` 3
    }

    private fun assertDryRunFailsAndTransactionIsNotSentToHorizon(
        value: Money,
        destinationAccountExists: Boolean = true,
        expectedReason: HorizonProxy.FailureReason? = null,
        expectedFailureValue: Money? = null,
        sourceAccount: KeyPair = KeyPair.fromSecretSeed(
            "SAD6LOTFMPIGAPOF2SPQSYD4OIGIE5XVVX3FW3K7QVFUTRSUUHMZQ76I"
        ),
        destinationAccount: KeyPair = KeyPair.fromAccountId(
            "GCO724H2FOHPBFF4OQ6IB5GB3CVE4W3UGDY4RIHHG6UPQ2YZSSCINMAI"
        )
    ) {
        server.givenAccounts(
            sourceAccountId = sourceAccount.accountId,
            destinationAccountId = destinationAccount.accountId,
            destinationAccountExists = destinationAccountExists
        )

        val proxy = payloadScope.get<HorizonProxy>().apply {
            update(server.url(""))
        }

        proxy.dryRunTransaction(
            sourceAccount,
            destinationAccount.accountId,
            value as CryptoValue,
            Memo.none()
        ).apply {
            success `should be` false
            transaction `should be` null
            if (expectedReason != null) {
                failureReason `should be` expectedReason
            }
            failureValue `should equal` expectedFailureValue
        }

        server.requestCount `should be less than` 3
    }

    private fun assertSendPasses(
        value: Money,
        destinationAccountExists: Boolean = true,
        sourceAccount: KeyPair = KeyPair.fromSecretSeed(
            "SAD6LOTFMPIGAPOF2SPQSYD4OIGIE5XVVX3FW3K7QVFUTRSUUHMZQ76I"
        ),
        destinationAccount: KeyPair = KeyPair.fromAccountId(
            "GCO724H2FOHPBFF4OQ6IB5GB3CVE4W3UGDY4RIHHG6UPQ2YZSSCINMAI"
        ),
        expectedRequests: Int
    ) {
        server.givenAccounts(
            sourceAccountId = sourceAccount.accountId,
            destinationAccountId = destinationAccount.accountId,
            destinationAccountExists = destinationAccountExists
        )
        server.givenRootResponse()
        server.givenPostWillBeSuccessful()

        val proxy = payloadScope.get<HorizonProxy>().apply {
            update(server.url(""))
        }

        proxy.sendTransaction(
            sourceAccount,
            destinationAccount.accountId,
            value as CryptoValue,
            10
        ).apply {
            success `should be` true
            transaction?.signatures?.size `should be` 1
        }

        server.requestCount `should be` expectedRequests
    }

    private fun assertDryRunSaysSendShouldPass(
        value: Money,
        destinationAccountExists: Boolean = true,
        sourceAccount: KeyPair = KeyPair.fromSecretSeed(
            "SAD6LOTFMPIGAPOF2SPQSYD4OIGIE5XVVX3FW3K7QVFUTRSUUHMZQ76I"
        ),
        destinationAccount: KeyPair = KeyPair.fromAccountId(
            "GCO724H2FOHPBFF4OQ6IB5GB3CVE4W3UGDY4RIHHG6UPQ2YZSSCINMAI"
        )
    ) {
        server.givenAccounts(
            sourceAccountId = sourceAccount.accountId,
            destinationAccountId = destinationAccount.accountId,
            destinationAccountExists = destinationAccountExists
        )

        val proxy = payloadScope.get<HorizonProxy>().apply {
            update(server.url(""))
        }

        proxy.dryRunTransaction(
            sourceAccount,
            destinationAccount.accountId,
            value as CryptoValue,
            Memo.none()
        ).apply {
            success `should be` true
            transaction?.signatures?.size `should be` 0
        }

        server.requestCount `should be` 2
    }

    @Test
    fun `insufficient funds during transaction send`() {
        server.givenAccountExists("GC7GSOOQCBBWNUOB6DIWNVM7537UKQ353H6LCU3DB54NUTVFR2T6OHF4")
        server.givenAccountExists("GCO724H2FOHPBFF4OQ6IB5GB3CVE4W3UGDY4RIHHG6UPQ2YZSSCINMAI")

        server.givenRootResponse()

        server.expect().post().withPath("/transactions")
            .andReturn(
                400,
                getStringFromResource("transactions/post_fail_insufficient_funds.json")
            )
            .once()

        val proxy = payloadScope.get<HorizonProxy>().apply {
            update(server.url(""))
        }

        proxy.sendTransaction(
            KeyPair.fromSecretSeed("SAD6LOTFMPIGAPOF2SPQSYD4OIGIE5XVVX3FW3K7QVFUTRSUUHMZQ76I"),
            "GCO724H2FOHPBFF4OQ6IB5GB3CVE4W3UGDY4RIHHG6UPQ2YZSSCINMAI",
            123.456789.lumens(),
            10
        ).success `should be` false

        server.requestCount `should be` 5
    }

    @Test
    fun `if destination account does not exist, it will do a create operation`() {
        server.givenAccountExists("GC7GSOOQCBBWNUOB6DIWNVM7537UKQ353H6LCU3DB54NUTVFR2T6OHF4")
        server.givenAccountDoesNotExist("GCO724H2FOHPBFF4OQ6IB5GB3CVE4W3UGDY4RIHHG6UPQ2YZSSCINMAI")
        server.givenPostWillBeSuccessful()
        server.givenRootResponse()
        val proxy = payloadScope.get<HorizonProxy>().apply {
            update(server.url(""))
        }

        val source = KeyPair.fromSecretSeed("SAD6LOTFMPIGAPOF2SPQSYD4OIGIE5XVVX3FW3K7QVFUTRSUUHMZQ76I")
        source.accountId `should equal` "GC7GSOOQCBBWNUOB6DIWNVM7537UKQ353H6LCU3DB54NUTVFR2T6OHF4"

        proxy.sendTransaction(
            source,
            "GCO724H2FOHPBFF4OQ6IB5GB3CVE4W3UGDY4RIHHG6UPQ2YZSSCINMAI",
            CryptoCurrency.XLM.withMajorValue("1.23E+4".toBigDecimal()),
            10
        ).apply {
            success `should be` true
            transaction `should not be` null
            transaction!!.operations.single().apply {
                this `should be instance of` CreateAccountOperation::class
                (this as CreateAccountOperation).apply {
                    destination `should equal` "GCO724H2FOHPBFF4OQ6IB5GB3CVE4W3UGDY4RIHHG6UPQ2YZSSCINMAI"
                    startingBalance `should equal` "12300.0000000"
                }
            }
            transaction.fee `should equal` 100L
            transaction.memo `should equal` Memo.none()
        }

        server.requestCount `should be` 4
    }

    @Test
    fun `memo during send - payment operation`() {
        server.givenAccountExists("GC7GSOOQCBBWNUOB6DIWNVM7537UKQ353H6LCU3DB54NUTVFR2T6OHF4")
        server.givenAccountExists("GCO724H2FOHPBFF4OQ6IB5GB3CVE4W3UGDY4RIHHG6UPQ2YZSSCINMAI")
        server.givenPostWillBeSuccessful()
        server.givenRootResponse()
        val proxy = payloadScope.get<HorizonProxy>().apply {
            update(server.url(""))
        }

        val source = KeyPair.fromSecretSeed("SAD6LOTFMPIGAPOF2SPQSYD4OIGIE5XVVX3FW3K7QVFUTRSUUHMZQ76I")
        source.accountId `should equal` "GC7GSOOQCBBWNUOB6DIWNVM7537UKQ353H6LCU3DB54NUTVFR2T6OHF4"

        val memo = Memo.text("Memo!")
        proxy.sendTransaction(
            source,
            "GCO724H2FOHPBFF4OQ6IB5GB3CVE4W3UGDY4RIHHG6UPQ2YZSSCINMAI",
            CryptoCurrency.XLM.withMajorValue("1.23E+4".toBigDecimal()),
            memo,
            10
        ).apply {
            success `should be` true
            transaction `should not be` null
            transaction!!.memo `should be` memo
            transaction.fee `should equal` 100L
        }
    }

    @Test
    fun `memo during dry run - create operation`() {
        server.givenAccountExists("GC7GSOOQCBBWNUOB6DIWNVM7537UKQ353H6LCU3DB54NUTVFR2T6OHF4")
        server.givenAccountDoesNotExist("GCO724H2FOHPBFF4OQ6IB5GB3CVE4W3UGDY4RIHHG6UPQ2YZSSCINMAI")

        val proxy = payloadScope.get<HorizonProxy>().apply {
            update(server.url(""))
        }

        val source = KeyPair.fromSecretSeed("SAD6LOTFMPIGAPOF2SPQSYD4OIGIE5XVVX3FW3K7QVFUTRSUUHMZQ76I")
        source.accountId `should equal` "GC7GSOOQCBBWNUOB6DIWNVM7537UKQ353H6LCU3DB54NUTVFR2T6OHF4"

        val memo = Memo.text("Memo!")
        proxy.dryRunTransaction(
            source,
            "GCO724H2FOHPBFF4OQ6IB5GB3CVE4W3UGDY4RIHHG6UPQ2YZSSCINMAI",
            CryptoCurrency.XLM.withMajorValue("1.23E+4".toBigDecimal()),
            memo = memo
        ).apply {
            success `should be` true
            transaction `should not be` null
            transaction!!.memo `should be` memo
            transaction.fee `should equal` 100L
        }
    }

    @Test
    fun `fee during send - payment operation`() {
        server.givenAccountExists("GC7GSOOQCBBWNUOB6DIWNVM7537UKQ353H6LCU3DB54NUTVFR2T6OHF4")
        server.givenAccountExists("GCO724H2FOHPBFF4OQ6IB5GB3CVE4W3UGDY4RIHHG6UPQ2YZSSCINMAI")
        server.givenPostWillBeSuccessful()
        server.givenRootResponse()
        val proxy = payloadScope.get<HorizonProxy>().apply {
            update(server.url(""))
        }

        val source = KeyPair.fromSecretSeed("SAD6LOTFMPIGAPOF2SPQSYD4OIGIE5XVVX3FW3K7QVFUTRSUUHMZQ76I")
        source.accountId `should equal` "GC7GSOOQCBBWNUOB6DIWNVM7537UKQ353H6LCU3DB54NUTVFR2T6OHF4"

        val memo = Memo.text("Memo!")
        proxy.sendTransaction(
            source,
            "GCO724H2FOHPBFF4OQ6IB5GB3CVE4W3UGDY4RIHHG6UPQ2YZSSCINMAI",
            CryptoCurrency.XLM.withMajorValue("1.23E+4".toBigDecimal()),
            timeout = 10,
            memo = memo,
            perOperationFee = 2100.stroops()
        ).apply {
            success `should be` true
            transaction `should not be` null
            transaction!!.memo `should be` memo
            transaction.fee `should equal` 2100L
        }
    }

    @Test
    fun `fee during dry run - create operation`() {
        server.givenAccountExists("GC7GSOOQCBBWNUOB6DIWNVM7537UKQ353H6LCU3DB54NUTVFR2T6OHF4")
        server.givenAccountDoesNotExist("GCO724H2FOHPBFF4OQ6IB5GB3CVE4W3UGDY4RIHHG6UPQ2YZSSCINMAI")

        val proxy = payloadScope.get<HorizonProxy>().apply {
            update(server.url(""))
        }

        val source = KeyPair.fromSecretSeed("SAD6LOTFMPIGAPOF2SPQSYD4OIGIE5XVVX3FW3K7QVFUTRSUUHMZQ76I")
        source.accountId `should equal` "GC7GSOOQCBBWNUOB6DIWNVM7537UKQ353H6LCU3DB54NUTVFR2T6OHF4"

        val memo = Memo.text("Memo!")
        proxy.dryRunTransaction(
            source,
            "GCO724H2FOHPBFF4OQ6IB5GB3CVE4W3UGDY4RIHHG6UPQ2YZSSCINMAI",
            CryptoCurrency.XLM.withMajorValue("1.23E+4".toBigDecimal()),
            memo = memo,
            perOperationFee = 1700.stroops()
        ).apply {
            success `should be` true
            transaction `should not be` null
            transaction!!.memo `should be` memo
            transaction.fee `should equal` 1700L
        }
    }

    @Test
    fun `bad fee`() {
        server.givenAccountExists("GC7GSOOQCBBWNUOB6DIWNVM7537UKQ353H6LCU3DB54NUTVFR2T6OHF4")
        server.givenAccountExists("GCO724H2FOHPBFF4OQ6IB5GB3CVE4W3UGDY4RIHHG6UPQ2YZSSCINMAI")

        val proxy = payloadScope.get<HorizonProxy>().apply {
            update(server.url(""))
        }

        val source = KeyPair.fromSecretSeed("SAD6LOTFMPIGAPOF2SPQSYD4OIGIE5XVVX3FW3K7QVFUTRSUUHMZQ76I")
        source.accountId `should equal` "GC7GSOOQCBBWNUOB6DIWNVM7537UKQ353H6LCU3DB54NUTVFR2T6OHF4"
        {
            proxy.dryRunTransaction(
                source,
                "GCO724H2FOHPBFF4OQ6IB5GB3CVE4W3UGDY4RIHHG6UPQ2YZSSCINMAI",
                CryptoCurrency.XLM.withMajorValue("1.23E+4".toBigDecimal()),
                memo = Memo.none(),
                perOperationFee = 1.bitcoin()
            )
        } `should throw` RuntimeException::class
    }

    @Test
    fun `transaction time bounds are specified`() {
        server.givenAccountExists("GC7GSOOQCBBWNUOB6DIWNVM7537UKQ353H6LCU3DB54NUTVFR2T6OHF4")
        server.givenAccountExists("GCO724H2FOHPBFF4OQ6IB5GB3CVE4W3UGDY4RIHHG6UPQ2YZSSCINMAI")
        server.givenPostWillBeSuccessful()
        server.givenRootResponse()

        val proxy = payloadScope.get<HorizonProxy>().apply {
            update(server.url(""))
        }

        val source = KeyPair.fromSecretSeed("SAD6LOTFMPIGAPOF2SPQSYD4OIGIE5XVVX3FW3K7QVFUTRSUUHMZQ76I")
        source.accountId `should equal` "GC7GSOOQCBBWNUOB6DIWNVM7537UKQ353H6LCU3DB54NUTVFR2T6OHF4"

        proxy.sendTransaction(
            source,
            "GCO724H2FOHPBFF4OQ6IB5GB3CVE4W3UGDY4RIHHG6UPQ2YZSSCINMAI",
            CryptoCurrency.XLM.withMajorValue("1.23E+4".toBigDecimal()),
            10
        ).apply {
            transaction!!.timeBounds `should not be` null
        }
    }
}

private fun DefaultMockServer.givenPostWillBeSuccessful() {
    expect().post().withPath("/transactions")
        .andReturn(
            200,
            getStringFromResource("transactions/post_success.json")
        )
        .once()
}

private fun DefaultMockServer.assertNoInteractions() {
    requestCount `should be` 0
}

private fun DefaultMockServer.givenAccounts(
    sourceAccountId: String,
    destinationAccountId: String,
    destinationAccountExists: Boolean
) {
    givenAccountExists(sourceAccountId)
    if (destinationAccountExists) {
        givenAccountExists(destinationAccountId)
    } else {
        givenAccountDoesNotExist(destinationAccountId)
    }
}

private fun DefaultMockServer.givenAccountExists(accountId: String) {
    expect().get().withPath("/accounts/$accountId")
        .andReturn(
            200,
            getStringFromResource("accounts/$accountId.json")
        )
        .once()
}

private fun DefaultMockServer.givenRootResponse() {
    expect().get().withPath("/")
        .andReturn(
            200,
            getStringFromResource("root_response.json")
        )
        .once()
}

private fun DefaultMockServer.givenAccountDoesNotExist(accountId: String) {
    expect().get().withPath("/accounts/$accountId")
        .andReturn(
            404,
            getStringFromResource("accounts/not_found.json")
        )
        .once()
}
