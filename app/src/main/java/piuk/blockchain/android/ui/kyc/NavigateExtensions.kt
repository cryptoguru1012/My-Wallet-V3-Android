package piuk.blockchain.android.ui.kyc

import androidx.fragment.app.Fragment
import androidx.navigation.NavDirections
import androidx.navigation.fragment.NavHostFragment

fun Fragment.navigate(directions: NavDirections) {
    NavHostFragment
        .findNavController(this)
        .navigate(directions)
}
