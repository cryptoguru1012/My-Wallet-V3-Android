package com.blockchain.preferences

interface SecurityPrefs {
    var disableRootedWarning: Boolean
    var trustScreenOverlay: Boolean
    val areScreenshotsEnabled: Boolean
    val isUnderTest: Boolean

    fun setScreenshotsEnabled(enable: Boolean)
    fun setIsUnderTest()
}
