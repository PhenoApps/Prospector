package org.phenoapps.prospector.fragments.tutorials

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.view.ContextThemeWrapper
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import org.phenoapps.prospector.R
import org.phenoapps.prospector.activities.MainActivity
import org.phenoapps.prospector.databinding.FragmentIndigoConnectInstructionsBinding

/**
 * A simple fragment that displays a scroll view of instructions.
 * Includes connection instructions and basic app usage.
 */
class IndigoInstructionsFragment : Fragment() {

    private var mBinding: FragmentIndigoConnectInstructionsBinding? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        val contextThemeWrapper = ContextThemeWrapper(activity, R.style.AppTheme)

        val localInflater = inflater.cloneInContext(contextThemeWrapper)

        mBinding = DataBindingUtil.inflate(localInflater, R.layout.fragment_indigo_connect_instructions, null, false)

        mBinding?.toolbar?.setNavigationOnClickListener {
            findNavController().popBackStack()
        }

        return mBinding?.root

    }

    override fun onResume() {
        super.onResume()

        (activity as? MainActivity)?.setToolbar(R.id.action_nav_data)

    }
}