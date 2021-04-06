package piuk.blockchain.androidcore.data.auth

import com.blockchain.logging.CrashLogger
import com.nhaarman.mockito_kotlin.eq
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.verifyNoMoreInteractions
import com.nhaarman.mockito_kotlin.verifyZeroInteractions
import com.nhaarman.mockito_kotlin.whenever
import info.blockchain.wallet.api.data.Status
import info.blockchain.wallet.api.data.WalletOptions
import info.blockchain.wallet.crypto.AESUtil
import info.blockchain.wallet.exceptions.InvalidCredentialsException
import io.reactivex.Observable
import junit.framework.TestCase.assertTrue
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.ResponseBody
import org.amshove.kluent.mock
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.anyString
import piuk.blockchain.android.testutils.RxTest
import piuk.blockchain.androidcore.data.access.AccessState
import piuk.blockchain.androidcore.utils.AESUtilWrapper
import piuk.blockchain.androidcore.utils.PersistentPrefs
import piuk.blockchain.androidcore.utils.PrngFixer
import retrofit2.Response
import java.util.concurrent.TimeUnit

class AuthDataManagerTest : RxTest() {

    private val prefsUtil: PersistentPrefs = mock()
    private val authService: AuthService = mock()
    private val accessState: AccessState = mock()
    private val aesUtilWrapper: AESUtilWrapper = mock()
    private val prngHelper: PrngFixer = mock()
    private val crashLogger: CrashLogger = mock()

    private lateinit var subject: AuthDataManager

    @Before
    fun setUp() {
        subject = AuthDataManager(
            prefsUtil,
            authService,
            accessState,
            aesUtilWrapper,
            prngHelper,
            crashLogger
        )
    }

    @Test
    fun getEncryptedPayload() {
        // Arrange
        val mockResponseBody = mock<ResponseBody>()
        whenever(
            authService.getEncryptedPayload(
                anyString(),
                anyString()
            )
        ).thenReturn(Observable.just(Response.success(mockResponseBody)))
        // Act
        val observer = subject.getEncryptedPayload("1234567890", "1234567890").test()
        // Assert
        verify(authService).getEncryptedPayload(anyString(), anyString())
        observer.assertComplete()
        observer.assertNoErrors()
        assertTrue(observer.values()[0].isSuccessful)
    }

    @Test
    fun getSessionId() {
        // Arrange
        val sessionId = "SESSION_ID"
        whenever(authService.getSessionId(anyString()))
            .thenReturn(Observable.just(sessionId))
        // Act
        val testObserver = subject.getSessionId("1234567890").test()
        // Assert
        verify(authService).getSessionId(anyString())
        testObserver.assertComplete()
        testObserver.onNext(sessionId)
        testObserver.assertNoErrors()
    }

    @Test
    fun submitTwoFactorCode() {
        // Arrange
        val sessionId = "SESSION_ID"
        val guid = "GUID"
        val code = "123456"
        val responseBody = ResponseBody.create(("application/json").toMediaTypeOrNull(), "{}")
        whenever(authService.submitTwoFactorCode(sessionId, guid, code))
            .thenReturn(Observable.just(responseBody))
        // Act
        val testObserver = subject.submitTwoFactorCode(sessionId, guid, code).test()
        // Assert
        verify(authService).submitTwoFactorCode(sessionId, guid, code)
        testObserver.assertComplete()
        testObserver.onNext(responseBody)
        testObserver.assertNoErrors()
    }

    @Test
    fun validatePinSuccessful() {
        val pin = "1234"
        val key = "SHARED_KEY"
        val encryptedPassword = "ENCRYPTED_PASSWORD"
        val decryptionKey = "DECRYPTION_KEY"
        val plaintextPassword = "PLAINTEXT_PASSWORD"
        val status = Status()

        status.success = decryptionKey
        whenever(prefsUtil.pinId).thenReturn(key)
        whenever(prefsUtil.getValue(PersistentPrefs.KEY_ENCRYPTED_PASSWORD, ""))
            .thenReturn(encryptedPassword)
        whenever(prefsUtil.backupEnabled).thenReturn(true)
        whenever(prefsUtil.hasBackup()).thenReturn(true)
        whenever(authService.validateAccess(key, pin))
            .thenReturn(Observable.just(Response.success(status)))

        whenever(
            aesUtilWrapper.decrypt(
                encryptedPassword,
                decryptionKey,
                AESUtil.PIN_PBKDF2_ITERATIONS
            )
        ).thenReturn(plaintextPassword)

        // Act
        val observer = subject.validatePin(pin).test()

        // Assert
        verify(accessState).setPin(pin)
        verify(accessState).isNewlyCreated = false
        verify(accessState).isRestored = false
        verifyNoMoreInteractions(accessState)
        verify(prefsUtil).pinId
        verify(prefsUtil).hasBackup()
        verify(prefsUtil).backupEnabled
        verify(prefsUtil).getValue(PersistentPrefs.KEY_WALLET_GUID)

        verify(prefsUtil).getValue(PersistentPrefs.KEY_ENCRYPTED_PASSWORD, "")

        verify(prefsUtil).restoreFromBackup(anyString(), eq(aesUtilWrapper))

        verifyNoMoreInteractions(prefsUtil)

        verify(authService).validateAccess(key, pin)
        verifyNoMoreInteractions(authService)

        verify(aesUtilWrapper).decrypt(
            encryptedPassword,
            decryptionKey,
            AESUtil.PIN_PBKDF2_ITERATIONS
        )

        verifyNoMoreInteractions(aesUtilWrapper)
        verifyZeroInteractions(prngHelper)
        observer.assertComplete()
        observer.assertValue(plaintextPassword)
        observer.assertNoErrors()
    }

    @Test
    fun validatePinFailure() {
        // Arrange
        val pin = "1234"
        val key = "SHARED_KEY"

        val decryptionKey = "DECRYPTION_KEY"
        val status = Status()
        status.success = decryptionKey

        whenever(prefsUtil.pinId).thenReturn(key)
        whenever(authService.validateAccess(key, pin))
            .thenReturn(
                Observable.just(
                    Response.error(
                        403,
                        ResponseBody.create(
                            ("application/json").toMediaTypeOrNull(),
                            "{}"
                        )
                    )
                )
            )
        // Act
        val observer = subject.validatePin(pin).test()
        // Assert
        verify(accessState).setPin(pin)
        verifyNoMoreInteractions(accessState)
        verify(prefsUtil).pinId == ""
        verifyNoMoreInteractions(prefsUtil)
        verify(authService).validateAccess(key, pin)
        verifyNoMoreInteractions(authService)
        verifyZeroInteractions(aesUtilWrapper)
        observer.assertNotComplete()
        observer.assertNoValues()
        observer.assertError(InvalidCredentialsException::class.java)
    }

    @Test
    fun createPinInvalid() {
        // Arrange
        val password = "PASSWORD"
        val pin = "123"

        // Act
        val observer = subject.createPin(password, pin).test()

        // Assert
        verifyZeroInteractions(accessState)
        verifyZeroInteractions(prefsUtil)
        verifyZeroInteractions(authService)
        verifyZeroInteractions(aesUtilWrapper)

        observer.assertNotComplete()
        observer.assertError(IllegalArgumentException::class.java)
    }

    @Test
    fun createPinSuccessful() {
        // Arrange
        val password = "PASSWORD"
        val pin = "1234"
        val encryptedPassword = "ENCRYPTED_PASSWORD"
        val status = Status()
        whenever(
            authService.setAccessKey(
                anyString(),
                anyString(),
                eq(pin)
            )
        ).thenReturn(Observable.just(Response.success(status)))
        whenever(
            aesUtilWrapper.encrypt(
                eq(password),
                anyString(),
                eq(AESUtil.PIN_PBKDF2_ITERATIONS)
            )
        ).thenReturn(encryptedPassword)
        whenever(prefsUtil.backupEnabled).thenReturn(true)
        whenever(prefsUtil.hasBackup()).thenReturn(false)

        // Act
        val observer = subject.createPin(password, pin).test()

        // Assert
        verify(accessState).setPin(pin)
        verifyNoMoreInteractions(accessState)
        verify(prngHelper).applyPRNGFixes()
        verifyNoMoreInteractions(prngHelper)
        verify(authService).setAccessKey(
            anyString(),
            anyString(),
            eq(pin)
        )
        verifyNoMoreInteractions(authService)
        verify(aesUtilWrapper).encrypt(
            eq(password),
            anyString(),
            eq(AESUtil.PIN_PBKDF2_ITERATIONS)
        )
        verifyNoMoreInteractions(aesUtilWrapper)
        verify(prefsUtil).setValue(PersistentPrefs.KEY_ENCRYPTED_PASSWORD, encryptedPassword)
        verify(prefsUtil).pinId = anyString()
        verify(prefsUtil).backupEnabled
        verify(prefsUtil).hasBackup()

        verify(prefsUtil).backupCurrentPrefs(anyString(), eq(aesUtilWrapper))
        verifyNoMoreInteractions(prefsUtil)
        observer.assertComplete()
        observer.assertNoErrors()
    }

    @Test
    fun createPinError() {
        // Arrange
        val password = "PASSWORD"
        val pin = "1234"
        whenever(
            authService.setAccessKey(
                anyString(),
                anyString(),
                eq(pin)
            )
        ).thenReturn(
            Observable.just(
                Response.error(
                    500,
                    ResponseBody.create(
                        ("application/json").toMediaTypeOrNull(),
                        "{}"
                    )
                )
            )
        )
        // Act
        val observer = subject.createPin(password, pin).test()
        // Assert
        verify(accessState).setPin(pin)
        verifyNoMoreInteractions(accessState)
        verify(prngHelper).applyPRNGFixes()
        verifyNoMoreInteractions(prngHelper)
        verify(authService).setAccessKey(
            anyString(),
            anyString(),
            eq(pin)
        )
        verifyNoMoreInteractions(authService)
        verifyZeroInteractions(aesUtilWrapper)
        verifyZeroInteractions(prefsUtil)
        observer.assertNotComplete()
        observer.assertError(Throwable::class.java)
    }

    @Test
    fun getWalletOptions() {
        // Arrange
        val walletOptions = WalletOptions()
        whenever(authService.getWalletOptions()).thenReturn(Observable.just(walletOptions))
        // Act
        val observer = subject.getWalletOptions().test()
        // Assert
        verify(authService).getWalletOptions()
        observer.assertComplete()
        observer.assertNoErrors()
        observer.assertValue(walletOptions)
    }

    /**
     * Getting encrypted payload returns error, should be caught by Observable and transformed into
     * [AuthDataManager.AUTHORIZATION_REQUIRED]
     */
    @Test
    fun startPollingAuthStatusError() {
        // Arrange
        val sessionId = "SESSION_ID"
        val guid = "GUID"
        whenever(authService.getEncryptedPayload(guid, sessionId))
            .thenReturn(Observable.error(Throwable()))
        // Act
        val testObserver = subject.startPollingAuthStatus(guid, sessionId).test()
        testScheduler.advanceTimeBy(3, TimeUnit.SECONDS)
        // Assert
        verify(authService).getEncryptedPayload(guid, sessionId)
        testObserver.assertComplete()
        testObserver.assertValue(AuthDataManager.AUTHORIZATION_REQUIRED)
        testObserver.assertNoErrors()
    }

    /**
     * Getting encrypted payload returns Auth Required, should be filtered out and emit no values.
     */
    @Test
    fun startPollingAuthStatusAccessRequired() {
        // Arrange
        val sessionId = "SESSION_ID"
        val guid = "GUID"
        val responseBody = ResponseBody.create(
            ("application/json").toMediaTypeOrNull(),
            ERROR_BODY
        )
        whenever(authService.getEncryptedPayload(guid, sessionId))
            .thenReturn(Observable.just(Response.error(500, responseBody)))
        // Act
        val testObserver = subject.startPollingAuthStatus(guid, sessionId).test()
        testScheduler.advanceTimeBy(2, TimeUnit.SECONDS)
        // Assert
        verify(authService).getEncryptedPayload(guid, sessionId)
        testObserver.assertNotComplete()
        testObserver.assertNoValues()
        testObserver.assertNoErrors()
    }

    @Test
    fun createCheckEmailTimer() {
        // Arrange

        // Act
        val testObserver = subject.createCheckEmailTimer().take(1).test()
        subject.timer = 1
        testScheduler.advanceTimeBy(2, TimeUnit.SECONDS)
        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        testObserver.assertValue(1)
    }

    companion object {

        private const val ERROR_BODY = "{\n" +
            "\t\"authorization_required\": \"true\"\n" +
            "}"
    }
}