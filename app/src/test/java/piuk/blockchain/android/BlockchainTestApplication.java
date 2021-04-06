package piuk.blockchain.android;

import android.annotation.SuppressLint;
import com.google.firebase.FirebaseApp;

/**
 * Created by adambennett on 09/08/2016.
 */
@SuppressLint("Registered")
public class BlockchainTestApplication extends BlockchainApplication {

    @Override
    public void onCreate() {
        FirebaseApp.initializeApp(this);
        super.onCreate();
    }

    @Override
    protected void checkSecurityProviderAndPatchIfNeeded() {
        // No-op
    }
}