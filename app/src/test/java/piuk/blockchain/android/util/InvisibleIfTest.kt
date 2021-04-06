package piuk.blockchain.android.util

import android.view.View
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.never
import com.nhaarman.mockito_kotlin.verify
import org.amshove.kluent.`it returns`
import org.junit.Test

class InvisibleIfTest {

    @Test
    fun `when function evaluates to true, the view is set to invisible`() {
        mock<View>()
            .apply {
                invisibleIf { true }
                verify(this).visibility = View.INVISIBLE
            }
    }

    @Test
    fun `when function evaluates to false, the view is set to visible`() {
        mock<View>()
            .apply {
                invisibleIf { false }
                verify(this).visibility = View.VISIBLE
            }
    }

    @Test
    fun `when view is null, the function does not evaluate`() {
        mock<() -> Boolean> {
            onGeneric { invoke() } `it returns` true
        }.apply {
            (null as View?).invisibleIf(this)
            verify(this, never()).invoke()
        }
    }

    @Test
    fun `when supplied true, the view is set to invisible`() {
        mock<View>()
            .apply {
                invisibleIf(true)
                verify(this).visibility = View.INVISIBLE
            }
    }

    @Test
    fun `when supplied false, the view is set to visible`() {
        mock<View>()
            .apply {
                invisibleIf(false)
                verify(this).visibility = View.VISIBLE
            }
    }
}
