package piuk.blockchain.android

import piuk.blockchain.android.ui.launcher.LauncherActivity
import piuk.blockchain.androidcore.BuildConfig
import piuk.blockchain.android.util.AppUtil
import timber.log.Timber

class UncaughtExceptionHandler private constructor(val appUtil: AppUtil) : Thread.UncaughtExceptionHandler {

    private val rootHandler = Thread.getDefaultUncaughtExceptionHandler()

    init {
        Thread.setDefaultUncaughtExceptionHandler(this)
    }

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        if (!BuildConfig.DEBUG) {
            // Don't restart when debugging - it unhelpfully spams logcat
            appUtil.restartApp(LauncherActivity::class.java)
        } else {
            Timber.e(throwable)
        }

        // Re-throw the exception so that the system can fail as it normally would, and so that
        // Firebase can log the exception automatically
        rootHandler.uncaughtException(thread, throwable)
    }

    companion object {
        fun install(appUtil: AppUtil) {
            UncaughtExceptionHandler(appUtil)
        }
    }
}
