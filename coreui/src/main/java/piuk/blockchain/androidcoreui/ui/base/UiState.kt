package piuk.blockchain.androidcoreui.ui.base

import androidx.annotation.IntDef

object UiState {

    const val LOADING = 0
    const val CONTENT = 1
    const val FAILURE = 2
    const val EMPTY = 3

    @Retention(AnnotationRetention.SOURCE)
    @IntDef(LOADING, CONTENT, FAILURE, EMPTY)
    annotation class UiStateDef
}
