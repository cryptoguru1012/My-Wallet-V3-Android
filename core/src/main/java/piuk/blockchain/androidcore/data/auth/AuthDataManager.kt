package piuk.blockchain.androidcore.data.auth

import androidx.annotation.VisibleForTesting
import com.blockchain.logging.CrashLogger
import info.blockchain.wallet.api.data.WalletOptions
import info.blockchain.wallet.crypto.AESUtil
import info.blockchain.wallet.exceptions.InvalidCredentialsException
import info.blockchain.wallet.exceptions.ServerConnectionException
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.exceptions.Exceptions
import okhttp3.ResponseBody
import org.spongycastle.util.encoders.Hex
import piuk.blockchain.androidcore.data.access.AccessState
import piuk.blockchain.androidcore.utils.AESUtilWrapper
import piuk.blockchain.androidcore.utils.PersistentPrefs
import piuk.blockchain.androidcore.utils.PrngFixer
import piuk.blockchain.androidcore.utils.extensions.applySchedulers
import piuk.blockchain.androidcore.utils.extensions.isValidPin
import retrofit2.Response
import java.security.SecureRandom
import java.util.concurrent.TimeUnit

class AuthDataManager(
    private val prefs: PersistentPrefs,
    private val authService: AuthService,
    private val accessState: AccessState,
    private val aesUtilWrapper: AESUtilWrapper,
    private val prngHelper: PrngFixer,
    private val crashLogger: CrashLogger
) {

    @VisibleForTesting
    internal var timer: Int = 0

    /**
     * Returns a [WalletOptions] object from the website. This object is used to get the
     * current buy/sell partner info as well as a list of countries where buy/sell is rolled out.
     *
     * @return An [Observable] wrapping a [WalletOptions] object
     */
    fun getWalletOptions(): Observable<WalletOptions> =
        authService.getWalletOptions()
            .applySchedulers()

    /**
     * Attempts to retrieve an encrypted Payload from the server, but may also return just part of a
     * Payload or an error response.
     *
     * @param guid The user's unique GUID
     * @param sessionId The current session ID
     * @return An [Observable] wrapping a [<] which could notify
     * the user that authentication (ie checking your email, 2FA etc) is required
     * @see .getSessionId
     */
    fun getEncryptedPayload(guid: String, sessionId: String): Observable<Response<ResponseBody>> =
        authService.getEncryptedPayload(guid, sessionId)
            .applySchedulers()

    /**
     * Gets an ephemeral session ID from the server.
     *
     * @param guid The user's unique GUID
     * @return An [Observable] wrapping a session ID as a String
     */
    fun getSessionId(guid: String): Observable<String> =
        authService.getSessionId(guid)
            .applySchedulers()

    /**
     * Submits a user's 2FA code to the server and returns a response. This response will contain
     * the user's encrypted Payload if successful, if not it will contain an error.
     *
     * @param sessionId The current session ID
     * @param guid The user's unique GUID
     * @param twoFactorCode A valid 2FA code generated from Google Authenticator or similar
     * @see .getSessionId
     */
    fun submitTwoFactorCode(
        sessionId: String,
        guid: String,
        twoFactorCode: String
    ): Observable<ResponseBody> = authService.submitTwoFactorCode(sessionId, guid, twoFactorCode)
        .applySchedulers()

    /**
     * Polls for the auth status of a user's account every 2 seconds until either the user checks
     * their email and a valid Payload is returned, or the call fails.
     *
     * @param guid The user's unique GUID
     * @param sessionId The current session ID
     * @return An [Observable] wrapping a String which represents the user's Payload OR an
     * auth required response from the API
     */
    fun startPollingAuthStatus(guid: String, sessionId: String): Observable<String> {
        // Emit tick every 2 seconds
        return Observable.interval(2, TimeUnit.SECONDS)
            // For each emission from the timer, try to get the payload
            .map { getEncryptedPayload(guid, sessionId).blockingFirst() }
            // If auth not required, emit payload
            .filter { s ->
                s.errorBody() == null ||
                        !s.errorBody()!!.string().contains(AUTHORIZATION_REQUIRED)
            }
            // Return message in response
            .map { responseBodyResponse -> responseBodyResponse.body()!!.string() }
            // If error called, emit Auth Required
            .onErrorReturn { AUTHORIZATION_REQUIRED }
            // Only emit the first object
            .firstElement()
            // As Observable rather than Maybe
            .toObservable()
            // Apply correct threading
            .applySchedulers()
    }

    /**
     * Creates a timer which counts down for two minutes and emits the remaining time on each count.
     * This is used to show the user how long they have to check their email before the login
     * request expires.
     *
     * @return An [Observable] where the emitted int is the number of seconds left
     */
    fun createCheckEmailTimer(): Observable<Int> {
        timer = 2 * 60

        return Observable.interval(0, 1, TimeUnit.SECONDS)
            .map { timer-- }
            .takeUntil { timer < 0 }
    }

    /**
     * Validates the passed PIN for the user's GUID and shared key and returns a decrypted password.
     *
     * @param passedPin The PIN to be used
     * @return An [Observable] where the wrapped String is the user's decrypted password
     */
    fun validatePin(passedPin: String): Observable<String> {
        return getValidatePinObservable(passedPin)
            .applySchedulers()
    }

    /**
     * Creates a new PIN for a user
     *
     * @param password The user's password
     * @param pin The new chosen PIN
     * @return A [Completable] object
     */
    fun createPin(password: String, passedPin: String): Completable {
        return getCreatePinObservable(password, passedPin)
            .applySchedulers()
    }

    private fun getValidatePinObservable(passedPin: String): Observable<String> {
        val key = prefs.pinId

        if (!passedPin.isValidPin()) {
            return Observable.error(IllegalArgumentException("Invalid PIN"))
        } else {
            accessState.setPin(passedPin)
            crashLogger.logEvent("validatePin. pin set. validity: ${passedPin.isValidPin()}")
        }

        return authService.validateAccess(key, passedPin)
            .map { response ->
                /*
                Note: Server side issue - If the incorrect PIN is supplied the server will respond
                with a 403 { code: 1, error: "Incorrect PIN you have x attempts left" }
                 */
                if (response.isSuccessful) {
                    accessState.isNewlyCreated = false
                    accessState.isRestored = false
                    val decryptionKey = response.body()!!.success

                    handleBackup(decryptionKey)

                    return@map aesUtilWrapper.decrypt(
                        prefs.getValue(PersistentPrefs.KEY_ENCRYPTED_PASSWORD, ""),
                        decryptionKey,
                        AESUtil.PIN_PBKDF2_ITERATIONS
                    )
                } else {
                    if (response.code() == 403) {
                        // Invalid PIN
                        throw InvalidCredentialsException("Validate access failed")
                    } else {
                        throw ServerConnectionException("""${response.code()} ${response.message()}""")
                    }
                }
            }
    }

    /*
    This function takes care of saving the encrypted values into the special storage for
    the automatic backup, and also decrypts the values when necessary into local storage.
     */
    private fun handleBackup(decryptionKey: String) {
        if (!prefs.backupEnabled) {
            // Just to make sure, if the user specifically opted out out of cloud backups,
            // always clear the backup over here. 
            prefs.clearBackup()
            return
        }

        if (prefs.hasBackup() && prefs.getValue(PersistentPrefs.KEY_WALLET_GUID) == null) {
            prefs.restoreFromBackup(decryptionKey, aesUtilWrapper)
        } else {
            prefs.backupCurrentPrefs(decryptionKey, aesUtilWrapper)
        }
    }

    private fun getCreatePinObservable(password: String, passedPin: String): Completable {

        if (!passedPin.isValidPin()) {
            return Completable.error(IllegalArgumentException("Invalid PIN"))
        } else {
            accessState.setPin(passedPin)
            crashLogger.logEvent("createPin. pin set. validity: ${passedPin.isValidPin()}")
        }

        prngHelper.applyPRNGFixes()

        return Completable.create { subscriber ->
            val bytes = ByteArray(16)
            val random = SecureRandom()
            random.nextBytes(bytes)
            val key = String(Hex.encode(bytes), Charsets.UTF_8)
            random.nextBytes(bytes)
            val value = String(Hex.encode(bytes), Charsets.UTF_8)

            authService.setAccessKey(key, value, passedPin)
                .subscribe({ response ->
                    if (response.isSuccessful) {
                        val encryptionKey = Hex.toHexString(value.toByteArray(Charsets.UTF_8))

                        val encryptedPassword = aesUtilWrapper.encrypt(
                            password,
                            encryptionKey,
                            AESUtil.PIN_PBKDF2_ITERATIONS
                        )

                        prefs.setValue(PersistentPrefs.KEY_ENCRYPTED_PASSWORD, encryptedPassword)
                        prefs.pinId = key

                        handleBackup(encryptionKey)

                        if (!subscriber.isDisposed) {
                            subscriber.onComplete()
                        }
                    } else {
                        throw Exceptions.propagate(
                            Throwable("Validate access failed: ${response.errorBody()?.string()}")
                        )
                    }
                }) { throwable ->
                    if (!subscriber.isDisposed) {
                        subscriber.onError(throwable)
                        subscriber.onComplete()
                    }
                }
        }
    }

    /**
     * Get the encryption password for pairing
     *
     * @param guid A user's GUID
     * @return [<] wrapping the pairing encryption password
     */
    fun getPairingEncryptionPassword(guid: String): Observable<ResponseBody> =
        authService.getPairingEncryptionPassword(guid)
            .applySchedulers()

    companion object {
        @VisibleForTesting
        internal const val AUTHORIZATION_REQUIRED = "authorization_required"
    }
}
