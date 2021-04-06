package piuk.blockchain.androidcoreui.ui.base;

import android.os.Bundle;
import androidx.annotation.CallSuper;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

@Deprecated
public abstract class BaseMvpFragment<VIEW extends View, PRESENTER extends BasePresenter<VIEW>>
        extends Fragment {

    private PRESENTER presenter;

    @CallSuper
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        presenter = createPresenter();
        presenter.initView(getMvpView());
    }

    @CallSuper
    @Override
    public void onResume() {
        super.onResume();
        presenter.onViewResumed();
    }

    @CallSuper
    @Override
    public void onPause() {
        super.onPause();
        presenter.onViewPaused();
    }

    @CallSuper
    @Override
    public void onDestroy() {
        super.onDestroy();
        presenter.onViewDestroyed();
    }

    protected void onViewReady() {
        presenter.onViewReady();
    }

    protected PRESENTER getPresenter() {
        return presenter;
    }

    protected abstract PRESENTER createPresenter();

    protected abstract VIEW getMvpView();

}
