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
import org.phenoapps.prospector.adapter.SpectrometerAdapter
import org.phenoapps.prospector.databinding.FragmentDeviceConnectionTutorialIndexBinding
import org.phenoapps.prospector.interfaces.OnModelClickListener

/**
 * A simple fragment that displays a scroll view of instructions.
 * Includes connection instructions and basic app usage.
 */
class ConnectionInstructionsFragment : Fragment(), OnModelClickListener {

    private var mBinding: FragmentDeviceConnectionTutorialIndexBinding? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        val contextThemeWrapper = ContextThemeWrapper(activity, R.style.AppTheme)

        val localInflater = inflater.cloneInContext(contextThemeWrapper)

        mBinding = DataBindingUtil.inflate(localInflater, R.layout.fragment_device_connection_tutorial_index, null, false)

        context?.let { ctx ->

            val lsTitle = ctx.getString(R.string.linksquare)
            val lsIcon = R.mipmap.linksquare_logo
            val isTitle = ctx.getString(R.string.innospectra_nano)
            val isIcon = R.mipmap.isc_logo
            mBinding?.fragDeviceConnectionTutorialIndexRv?.adapter = SpectrometerAdapter(this).also {
                it.submitList(mutableListOf(
                    SpectrometerAdapter.SpectrometerListItem(0, isTitle, isIcon),
                    SpectrometerAdapter.SpectrometerListItem(1, lsTitle, lsIcon))
                )
            }

        }

        mBinding?.toolbar?.setNavigationOnClickListener {
            findNavController().popBackStack()
        }

        return mBinding?.root

    }

    override fun onResume() {
        super.onResume()

        (activity as? MainActivity)?.setToolbar(R.id.action_nav_data)

    }

    override fun onClickModel(model: Any?) {

        if (model is SpectrometerAdapter.SpectrometerListItem) {

            when (model.id) {

                0 -> findNavController().navigate(ConnectionInstructionsFragmentDirections
                    .actionFromIndexToInnoSpectraTutorial())

                1 -> findNavController().navigate(ConnectionInstructionsFragmentDirections
                    .actionFromIndexToLinksquareTutorial())
            }
        }
    }
}