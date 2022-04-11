package org.phenoapps.prospector.fragments.nano_configuration_creator

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import dagger.hilt.android.AndroidEntryPoint
import org.phenoapps.prospector.R
import org.phenoapps.prospector.activities.MainActivity

@AndroidEntryPoint
class SpectralRangeFragment: Fragment(R.layout.fragment_config_creator_spectral_range) {

    private val args: SpectralRangeFragmentArgs by navArgs()

    private var uiStartEditText: EditText? = null
    private var uiEndEditText: EditText? = null
    private var uiBackButton: Button? = null
    private var uiNextButton: Button? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        uiStartEditText = view.findViewById(R.id.frag_cc_spectral_range_start_et)
        uiEndEditText = view.findViewById(R.id.frag_cc_spectral_range_end_et)
        uiBackButton = view.findViewById(R.id.frag_cc_back_btn)
        uiNextButton = view.findViewById(R.id.frag_cc_next_btn)

        val section = "${args.config.currentIndex + 1}"
        view.findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)?.title = getString(R.string.section, section)

        view.findViewById<ProgressBar>(R.id.frag_cc_pb)?.progress =
            args.config.currentIndex * (100.0 / (args.config.sections?.size ?: 1)).toInt()

        setupButtonListeners()
        setupToolbar()
    }

    private fun setupToolbar() {
        val toolbar = view?.findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)

        toolbar?.setNavigationOnClickListener {
            findNavController().popBackStack()
        }
    }

    private fun setupButtonListeners() {

        uiBackButton?.setOnClickListener {
            findNavController().popBackStack()
        }

        uiNextButton?.setOnClickListener {
            if (verifyUi()) {

                val start = uiStartEditText?.text?.toString()?.toIntOrNull() ?: 0
                val end = uiEndEditText?.text?.toString()?.toIntOrNull() ?: 0

                findNavController().navigate(SpectralRangeFragmentDirections
                    .actionSpectralRangeFragmentToExposureFragment(args.config.apply {
                        this.sections?.get(this.currentIndex)?.apply {
                            this.start = start.toFloat()
                            this.end = end.toFloat()
                        }
                    }))
            }
        }
    }

    private fun verifyUi(): Boolean {

        with (activity as MainActivity) {

            val start = uiStartEditText?.text?.toString()?.toIntOrNull() ?: 0
            val end = uiEndEditText?.text?.toString()?.toIntOrNull() ?: 0

            if (start in 900..1699) {

                if (end in 901..1700) {

                    return true

                } else {

                    notify(getString(R.string.frag_cc_spectral_end_range))

                }

            } else {

                notify(getString(R.string.frag_cc_spectral_start_range))
            }
        }

        return false
    }
}