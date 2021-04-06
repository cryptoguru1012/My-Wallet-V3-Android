package piuk.blockchain.android.ui.onboarding

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.fragment_email_prompt.*
import piuk.blockchain.android.R
import java.lang.NullPointerException

class EmailPromptFragment : Fragment() {

    private var listener: OnFragmentInteractionListener? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_email_prompt, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        button_enable?.setOnClickListener { listener?.onVerifyEmailClicked() }
        email_address?.text = arguments?.emailAddress
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)

        if (context is OnFragmentInteractionListener) {
            listener = context
        } else {
            throw RuntimeException("$context must implement OnFragmentInteractionListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    internal interface OnFragmentInteractionListener {
        fun onVerifyEmailClicked()
    }

    companion object {

        private const val ARGUMENT_EMAIL = "email"

        fun newInstance(email: String): EmailPromptFragment {
            val fragment = EmailPromptFragment()

            fragment.arguments = Bundle().apply {
                emailAddress = email
            }
            return fragment
        }

        private var Bundle?.emailAddress: String
            get() = this?.getString(ARGUMENT_EMAIL) ?: ""
            set(v) = this?.putString(ARGUMENT_EMAIL, v) ?: throw NullPointerException()
    }
}
