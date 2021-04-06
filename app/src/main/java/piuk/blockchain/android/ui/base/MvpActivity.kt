package piuk.blockchain.android.ui.base

import androidx.annotation.CallSuper

abstract class MvpActivity<V : MvpView, P : MvpPresenter<V>> : BlockchainActivity() {

    protected abstract val presenter: P
    protected abstract val view: V

    final override val alwaysDisableScreenshots
        get() = presenter.alwaysDisableScreenshots

    final override val enableLogoutTimer
        get() = presenter.enableLogoutTimer

    @CallSuper
    override fun onResume() {
        super.onResume()
        presenter.attachView(view)
    }

    @CallSuper
    override fun onPause() {
        presenter.detachView(view)
        super.onPause()
    }
}