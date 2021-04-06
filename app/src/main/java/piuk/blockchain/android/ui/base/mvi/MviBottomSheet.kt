package piuk.blockchain.android.ui.base.mvi

import androidx.annotation.CallSuper
import androidx.viewbinding.ViewBinding
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.subscribeBy
import piuk.blockchain.android.BuildConfig
import piuk.blockchain.android.ui.base.SlidingModalBottomDialog
import timber.log.Timber

abstract class MviBottomSheet<M : MviModel<S, I>, I : MviIntent<S>, S : MviState, E : ViewBinding> :
    SlidingModalBottomDialog<E>() {

    protected abstract val model: M

    private var subscription: Disposable? = null

    override fun onResume() {
        super.onResume()
        dispose()
        subscription = model.state.subscribeBy(
            onNext = { render(it) },
            onError = {
                if (BuildConfig.DEBUG) {
                    throw it
                }
                Timber.e(it)
            },
            onComplete = { Timber.d("***> State on complete!!") }
        )
    }

    override fun onPause() {
        dispose()
        super.onPause()
    }

    protected abstract fun render(newState: S)

    @CallSuper
    protected open fun dispose() {
        subscription?.dispose()
    }
}
