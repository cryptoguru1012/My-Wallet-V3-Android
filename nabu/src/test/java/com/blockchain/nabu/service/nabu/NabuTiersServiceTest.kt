package com.blockchain.nabu.service.nabu

import com.blockchain.nabu.api.nabu.NABU_KYC_TIERS
import com.blockchain.nabu.models.responses.nabu.KycTierState
import com.blockchain.nabu.models.responses.nabu.KycTierStateAdapter
import com.blockchain.nabu.models.responses.nabu.LimitsJson
import com.blockchain.nabu.models.responses.nabu.TierResponse
import com.blockchain.nabu.models.responses.nabu.KycTiers
import com.blockchain.nabu.Authenticator
import com.blockchain.nabu.models.responses.tokenresponse.NabuSessionTokenResponse
import com.blockchain.serialization.BigDecimalAdaptor
import com.blockchain.nabu.api.nabu.Nabu
import com.blockchain.nabu.service.NabuTierService
import com.blockchain.testutils.MockedRetrofitTest
import com.blockchain.testutils.getStringFromResource
import com.blockchain.testutils.mockWebServerInit
import com.squareup.moshi.Moshi
import io.reactivex.Single
import okhttp3.mockwebserver.MockResponse
import com.blockchain.android.testutils.rxInit
import io.reactivex.Maybe
import okhttp3.mockwebserver.MockWebServer
import org.amshove.kluent.`should equal to`
import org.amshove.kluent.`should equal`
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class NabuTiersServiceTest {

    private lateinit var subject: NabuTierService

    private val moshi: Moshi = Moshi.Builder()
        .add(BigDecimalAdaptor())
        .add(KycTierStateAdapter())
        .build()
    private val server: MockWebServer = MockWebServer()

    @get:Rule
    val initMockServer = mockWebServerInit(server)

    @get:Rule
    val initSchedulers = rxInit {
        mainTrampoline()
        ioTrampoline()
    }

    @Before
    fun setUp() {
        val nabuToken: Authenticator = FakeAuthenticator("Token1")
        subject = NabuTierService(
            MockedRetrofitTest(moshi, server).retrofit.create(
                Nabu::
                class.java
            ), nabuToken
        )
    }

    @Test
    fun `get tiers`() {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(getStringFromResource("com/blockchain/kyc/services/nabu/GetTiers.json"))
        )
        subject.tiers()
            .test()
            .assertComplete()
            .assertNoErrors()
            .values()
            .single() `should equal`
            KycTiers(
                tiersResponse = listOf(
                    TierResponse(
                        0,
                        "Tier 0",
                        state = KycTierState.Verified,
                        limits = LimitsJson(
                            currency = "USD",
                            daily = null,
                            annual = null
                        )
                    ),
                    TierResponse(
                        1,
                        "Tier 1",
                        state = KycTierState.Pending,
                        limits = LimitsJson(
                            currency = "USD",
                            daily = null,
                            annual = 1000.0.toBigDecimal()
                        )
                    ),
                    TierResponse(
                        2,
                        "Tier 2",
                        state = KycTierState.None,
                        limits = LimitsJson(
                            currency = "USD",
                            daily = 25000.0.toBigDecimal(),
                            annual = null
                        )
                    )
                )
            )
        server.urlRequested!! `should equal to` "/$NABU_KYC_TIERS"
    }

    @Test
    fun `set tier`() {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("")
        )
        subject.setUserTier(1)
            .test()
            .assertComplete()
            .assertNoErrors()
        server.takeRequest().apply {
            path!! `should equal to` "/$NABU_KYC_TIERS"
            body.toString() `should equal to` """[text={"selectedTier":1}]"""
            headers.get("authorization") `should equal` "Bearer Token1"
        }
    }

    private val MockWebServer.urlRequested get() = takeRequest().path
}

private class FakeAuthenticator(private val token: String) : Authenticator {

    override fun <T> authenticate(singleFunction: (NabuSessionTokenResponse) -> Single<T>): Single<T> =
        Single.just(
            NabuSessionTokenResponse(
                id = "",
                userId = "",
                token = token,
                isActive = true,
                expiresAt = "",
                insertedAt = "",
                updatedAt = ""
            )
        ).flatMap { singleFunction(it) }

    override fun <T> authenticateMaybe(maybeFunction: (NabuSessionTokenResponse) -> Maybe<T>): Maybe<T> {
        throw Exception("Not expected")
    }

    override fun <T> authenticateSingle(singleFunction: (Single<NabuSessionTokenResponse>) -> Single<T>): Single<T> {
        throw Exception("Not expected")
    }

    override fun invalidateToken() {
        throw Exception("Not expected")
    }
}
