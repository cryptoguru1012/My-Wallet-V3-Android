package com.blockchain.nabu.datamanagers

import com.blockchain.logging.DigitalTrust
import com.blockchain.nabu.getEmptySessionToken
import com.blockchain.nabu.models.responses.nabu.NabuCountryResponse
import com.blockchain.nabu.models.responses.nabu.NabuStateResponse
import com.blockchain.nabu.models.responses.nabu.NabuUser
import com.blockchain.nabu.models.responses.nabu.RegisterCampaignRequest
import com.blockchain.nabu.models.responses.nabu.Scope
import com.blockchain.nabu.models.responses.nabu.SupportedDocuments
import com.blockchain.nabu.models.responses.tokenresponse.NabuOfflineTokenResponse
import com.blockchain.nabu.models.responses.wallet.RetailJwtResponse
import com.blockchain.nabu.service.NabuService
import com.blockchain.nabu.service.RetailWalletTokenService
import com.blockchain.nabu.stores.NabuSessionTokenStore
import com.blockchain.utils.Optional
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import info.blockchain.wallet.api.data.Settings
import info.blockchain.wallet.exceptions.ApiException
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import org.amshove.kluent.`it returns`
import org.amshove.kluent.mock
import org.junit.Before
import org.junit.Test
import piuk.blockchain.androidcore.data.payload.PayloadDataManager
import piuk.blockchain.androidcore.data.settings.SettingsDataManager
import piuk.blockchain.androidcore.utils.PersistentPrefs

class NabuDataManagerTest {

    private lateinit var subject: NabuDataManagerImpl
    private val nabuService: NabuService = mock()
    private val tokenService: RetailWalletTokenService = mock()
    private val nabuTokenStore: NabuSessionTokenStore = mock()
    private val settingsDataManager: SettingsDataManager = mock()
    private val payloadDataManager: PayloadDataManager = mock()
    private val userReporter: NabuUserReporter = mock()
    private val walletReporter: WalletReporter = mock()
    private val digitalTrust: DigitalTrust = mock()
    private val prefs: PersistentPrefs = mock()
    private val appVersion = "6.23.2"
    private val deviceId = "DEVICE_ID"
    private val email = "EMAIL"
    private val guid = "GUID"
    private val sharedKey = "SHARED_KEY"

    @Before
    fun setUp() {
        whenever(payloadDataManager.guid).thenReturn(guid)
        whenever(payloadDataManager.sharedKey).thenReturn(sharedKey)

        val settings: Settings = mock()
        whenever(settings.email).thenReturn(email)
        whenever(settingsDataManager.getSettings()).thenReturn(Observable.just(settings))

        subject = NabuDataManagerImpl(
            nabuService,
            tokenService,
            nabuTokenStore,
            appVersion,
            settingsDataManager,
            userReporter,
            walletReporter,
            digitalTrust,
            payloadDataManager,
            prefs
        )
    }

    @Test
    fun `createUser success`() {
        // Arrange
        val jwt = "JWT"
        whenever(
            tokenService.requestJwt(
                guid = guid,
                sharedKey = sharedKey
            )
        ).thenReturn(Single.just(RetailJwtResponse(true, jwt, null)))
        // Act
        val testObserver = subject.requestJwt().test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        testObserver.assertValue(jwt)
        verify(tokenService).requestJwt(
            guid = guid,
            sharedKey = sharedKey
        )
    }

    @Test
    fun `createUser failure`() {
        // Arrange
        val error = "ERROR"
        whenever(
            tokenService.requestJwt(
                guid = guid,
                sharedKey = sharedKey
            )
        ).thenReturn(Single.just(RetailJwtResponse(false, null, error)))
        // Act
        val testObserver = subject.requestJwt().test()
        // Assert
        testObserver.assertNotComplete()
        testObserver.assertError(ApiException::class.java)
        verify(tokenService).requestJwt(
            guid = guid,
            sharedKey = sharedKey
        )
    }

    @Test
    fun getAuthToken() {
        // Arrange
        val userId = "USER_ID"
        val token = "TOKEN"
        val jwt = "JWT"
        val tokenResponse = NabuOfflineTokenResponse(userId, token)
        whenever(nabuService.getAuthToken(jwt))
            .thenReturn(Single.just(tokenResponse))
        // Act
        val testObserver = subject.getAuthToken(jwt).test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        testObserver.assertValue(tokenResponse)
        verify(nabuService).getAuthToken(jwt)
    }

    @Test
    fun getSessionToken() {
        // Arrange
        val offlineToken = NabuOfflineTokenResponse("", "")
        val sessionTokenResponse = getEmptySessionToken()
        whenever(
            nabuService.getSessionToken(
                userId = offlineToken.userId,
                offlineToken = offlineToken.token,
                guid = guid,
                email = email,
                deviceId = deviceId,
                appVersion = appVersion
            )
        ).thenReturn(Single.just(sessionTokenResponse))
        whenever(prefs.deviceId).thenReturn(deviceId)
        // Act
        val testObserver = subject.getSessionToken(offlineToken).test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        testObserver.assertValue(sessionTokenResponse)
        verify(nabuService).getSessionToken(
            userId = offlineToken.userId,
            offlineToken = offlineToken.token,
            guid = guid,
            email = email,
            deviceId = deviceId,
            appVersion = appVersion
        )
    }

    @Test
    fun createBasicUser() {
        // Arrange
        val firstName = "FIRST_NAME"
        val lastName = "LAST_NAME"
        val dateOfBirth = "25-02-1995"
        val offlineToken = NabuOfflineTokenResponse("", "")
        val sessionToken = getEmptySessionToken()
        whenever(nabuTokenStore.requiresRefresh()).thenReturn(false)
        whenever(nabuTokenStore.getAccessToken())
            .thenReturn(Observable.just(Optional.Some(sessionToken)))
        whenever(
            nabuService.createBasicUser(
                firstName,
                lastName,
                dateOfBirth,
                sessionToken
            )
        ).thenReturn(Completable.complete())
        // Act
        val testObserver = subject.createBasicUser(
            firstName,
            lastName,
            dateOfBirth,
            offlineToken
        ).test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        verify(nabuService).createBasicUser(
            firstName,
            lastName,
            dateOfBirth,
            sessionToken
        )
    }

    @Test
    fun getUser() {
        // Arrange
        val userObject: NabuUser = mock()
        val offlineToken = NabuOfflineTokenResponse(USER_ID, "")
        val sessionToken = getEmptySessionToken()
        whenever(nabuTokenStore.requiresRefresh()).thenReturn(false)
        whenever(nabuTokenStore.getAccessToken())
            .thenReturn(Observable.just(Optional.Some(sessionToken)))
        whenever(nabuService.getUser(sessionToken))
            .thenReturn(Single.just(userObject))
        // Act
        val testObserver = subject.getUser(offlineToken).test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        testObserver.assertValue(userObject)
        verify(nabuService).getUser(sessionToken)
        verify(walletReporter).reportWalletGuid(payloadDataManager.guid)
        verify(userReporter).reportUser(userObject)
        verify(userReporter).reportUserId(USER_ID)
        verify(digitalTrust).setUserId(USER_ID)
    }

    @Test
    fun `get users tags with values`() {
        // Arrange
        val userObject: NabuUser = mock {
            on { tags } `it returns` mapOf("campaign" to mapOf("some tag" to "some data"))
        }
        val offlineToken = NabuOfflineTokenResponse("", "")
        val sessionToken = getEmptySessionToken()
        whenever(nabuTokenStore.requiresRefresh()).thenReturn(false)
        whenever(nabuTokenStore.getAccessToken())
            .thenReturn(Observable.just(Optional.Some(sessionToken)))
        whenever(nabuService.getUser(sessionToken))
            .thenReturn(Single.just(userObject))
        // Act
        val testObserver = subject.getCampaignList(offlineToken).test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        testObserver.assertValue(listOf("campaign"))
        verify(nabuService).getUser(sessionToken)
    }

    @Test
    fun `get users tags returns empty list`() {
        // Arrange
        val userObject: NabuUser = mock()
        val offlineToken = NabuOfflineTokenResponse("", "")
        val sessionToken = getEmptySessionToken()
        whenever(nabuTokenStore.requiresRefresh()).thenReturn(false)
        whenever(nabuTokenStore.getAccessToken())
            .thenReturn(Observable.just(Optional.Some(sessionToken)))
        whenever(nabuService.getUser(sessionToken))
            .thenReturn(Single.just(userObject))
        // Act
        val testObserver = subject.getCampaignList(offlineToken).test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        testObserver.assertValue(emptyList())
        verify(nabuService).getUser(sessionToken)
    }

    @Test
    fun addAddress() {
        // Arrange
        val city = "CITY"
        val line1 = "LINE1"
        val line2 = "LINE2"
        val state = null
        val countryCode = "COUNTRY_CODE"
        val postCode = "POST_CODE"
        val offlineToken = NabuOfflineTokenResponse("", "")
        val sessionToken = getEmptySessionToken()
        whenever(nabuTokenStore.requiresRefresh()).thenReturn(false)
        whenever(nabuTokenStore.getAccessToken())
            .thenReturn(Observable.just(Optional.Some(sessionToken)))
        whenever(
            nabuService.addAddress(
                sessionToken,
                line1,
                line2,
                city,
                state,
                postCode,
                countryCode
            )
        ).thenReturn(Completable.complete())
        // Act
        val testObserver = subject.addAddress(
            offlineToken,
            line1,
            line2,
            city,
            state,
            postCode,
            countryCode
        ).test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        verify(nabuService).addAddress(
            sessionToken,
            line1,
            line2,
            city,
            state,
            postCode,
            countryCode
        )
    }

    @Test
    fun recordCountrySelection() {
        // Arrange
        val jwt = "JWT"
        val countryCode = "US"
        val stateCode = "US-AL"
        val notifyWhenAvailable = true
        val offlineToken = NabuOfflineTokenResponse("", "")
        val sessionToken = getEmptySessionToken()
        whenever(nabuTokenStore.requiresRefresh()).thenReturn(false)
        whenever(nabuTokenStore.getAccessToken())
            .thenReturn(Observable.just(Optional.Some(sessionToken)))
        whenever(
            nabuService.recordCountrySelection(
                sessionToken,
                jwt,
                countryCode,
                stateCode,
                notifyWhenAvailable
            )
        ).thenReturn(Completable.complete())
        // Act
        val testObserver = subject.recordCountrySelection(
            offlineToken,
            jwt,
            countryCode,
            stateCode,
            notifyWhenAvailable
        ).test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        verify(nabuService).recordCountrySelection(
            sessionToken,
            jwt,
            countryCode,
            stateCode,
            notifyWhenAvailable
        )
    }

    @Test
    fun getCountriesList() {
        // Arrange
        val countriesList = listOf(
            NabuCountryResponse("GER", "Germany", listOf("EEA"), listOf("KYC")),
            NabuCountryResponse("UK", "United Kingdom", listOf("EEA"), listOf("KYC"))
        )
        whenever(nabuService.getCountriesList(Scope.Kyc))
            .thenReturn(Single.just(countriesList))
        // Act
        val testObserver = subject.getCountriesList(Scope.Kyc).test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        testObserver.assertValue(countriesList)
        verify(nabuService).getCountriesList(Scope.Kyc)
    }

    @Test
    fun getStatesList() {
        // Arrange
        val statesList = listOf(
            NabuStateResponse("US-AL", "Alabama", listOf("KYC"), "US"),
            NabuStateResponse("US-AZ", "Arizona", listOf("KYC"), "US")
        )
        whenever(nabuService.getStatesList("US", Scope.Kyc))
            .thenReturn(Single.just(statesList))
        // Act
        val testObserver = subject.getStatesList("US", Scope.Kyc).test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        testObserver.assertValue(statesList)
        verify(nabuService).getStatesList("US", Scope.Kyc)
    }

    @Test
    fun getSupportedDocuments() {
        // Arrange
        val countryCode = "US"
        val offlineToken = NabuOfflineTokenResponse("", "")
        val sessionToken = getEmptySessionToken()
        whenever(nabuTokenStore.requiresRefresh()).thenReturn(false)
        whenever(nabuTokenStore.getAccessToken())
            .thenReturn(Observable.just(Optional.Some(sessionToken)))
        whenever(
            nabuService.getSupportedDocuments(
                sessionToken,
                countryCode
            )
        ).thenReturn(Single.just(listOf(SupportedDocuments.PASSPORT)))
        // Act
        val testObserver = subject.getSupportedDocuments(
            offlineToken,
            countryCode
        ).test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        testObserver.assertValue(listOf(SupportedDocuments.PASSPORT))
        verify(nabuService).getSupportedDocuments(
            sessionToken,
            countryCode
        )
    }

    @Test
    fun registerCampaign() {
        // Arrange
        val offlineToken = NabuOfflineTokenResponse("", "")
        val sessionToken = getEmptySessionToken()
        val campaignRequest = RegisterCampaignRequest(emptyMap(), false)
        whenever(nabuTokenStore.requiresRefresh()).thenReturn(false)
        whenever(nabuTokenStore.getAccessToken())
            .thenReturn(Observable.just(Optional.Some(sessionToken)))
        whenever(nabuService.registerCampaign(sessionToken, campaignRequest, "campaign"))
            .thenReturn(Completable.complete())
        // Act
        val testObserver = subject.registerCampaign(
            offlineToken,
            campaignRequest,
            "campaign"
        ).test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        verify(nabuService).registerCampaign(sessionToken, campaignRequest, "campaign")
    }

    companion object {
        private const val USER_ID = "1"
    }
}
