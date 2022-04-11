package org.phenoapps.prospector.fragments.nano_configuration_creator

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import dagger.hilt.android.AndroidEntryPoint
import org.phenoapps.prospector.R
import org.phenoapps.prospector.activities.MainActivity

@AndroidEntryPoint
class DigitalResolutionFragment: Fragment(R.layout.fragment_config_creator_digital_resolution) {

    private val args: DigitalResolutionFragmentArgs by navArgs()

    private var uiProgressBar: ProgressBar? = null
    private var uiDigitalResolutionEditText: EditText? = null
    private var uiPreviewTextView: TextView? = null
    private var uiBackButton: Button? = null
    private var uiNextButton: Button? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        uiDigitalResolutionEditText = view.findViewById(R.id.frag_cc_digital_resolution_et)
        uiPreviewTextView = view.findViewById(R.id.frag_cc_subtitle_fwhm_preview_tv)
        uiBackButton = view.findViewById(R.id.frag_cc_back_btn)
        uiNextButton = view.findViewById(R.id.frag_cc_next_btn)

        val section = "${args.config.currentIndex + 1}"
        view.findViewById<Toolbar>(R.id.toolbar)?.title = getString(R.string.section, section)

        view.findViewById<ProgressBar>(R.id.frag_cc_pb)?.progress =
            args.config.currentIndex * (100.0 / (args.config.sections?.size ?: 1)).toInt()

        args.config.apply {
            this.sections?.get(this.currentIndex)?.apply {
                resolution = 0
            }
        }

        updatePreviewText()
        setupButtonListeners()
        setupToolbar()
    }

    private fun setupToolbar() {
        val toolbar = view?.findViewById<Toolbar>(R.id.toolbar)

        toolbar?.setNavigationOnClickListener {
            findNavController().popBackStack()
        }
    }

    private fun updatePreviewText() {

        val rangeEnd = args.config.sections?.get(args.config.currentIndex)?.end ?: 1700f
        val rangeStart = args.config.sections?.get(args.config.currentIndex)?.start ?: 900f
        val width = args.config.sections?.get(args.config.currentIndex)?.width ?: 2.34

        val limitDr = args.config.sections?.sumOf { it.resolution } ?: 0

        val recDr = (624 - limitDr).coerceAtMost((2 * (rangeEnd - rangeStart) / width).toInt())

        uiDigitalResolutionEditText?.setText("$recDr")

        uiPreviewTextView?.text = getString(R.string.frag_cc_digital_resolution_fwhm_preview,
            (624 - limitDr).toString(), recDr.toString(), rangeEnd.toString(), rangeStart.toString(), width.toString())
    }

    private fun setupButtonListeners() {

        uiBackButton?.setOnClickListener {

            findNavController().popBackStack()
        }

        uiNextButton?.setOnClickListener {

            val dr = uiDigitalResolutionEditText?.text?.toString()?.toIntOrNull() ?: 0
            val limitDr = args.config.sections?.sumOf { it.resolution } ?: 624

            if (verifyUi(dr, limitDr)) {

                if (args.config.currentIndex + 1 == (args.config.sections?.size ?: 1)) {
                    findNavController().navigate(DigitalResolutionFragmentDirections
                        .actionDigitalResolutionFragmentToSummaryFragment(args.config.apply {
                            this.sections?.get(this.currentIndex)?.apply {
                                resolution = dr
                            }
                        }))
                } else {

                    uiProgressBar?.secondaryProgress = 0
                    uiProgressBar?.incrementProgressBy(95/(args.config.sections?.size ?: 2))
                    findNavController().navigate(DigitalResolutionFragmentDirections
                        .actionDigitalResolutionFragmentToMethodFragment(args.config.apply {
                            this.sections?.get(this.currentIndex++)?.apply {
                                resolution = dr
                            }
                        }))
                }
            }
        }
    }

    private fun verifyUi(dr: Int, limit: Int): Boolean {

        with (activity as MainActivity) {

            val left = 624 - limit

            if (left != 0) {

                if (dr in 3..left) {

                    return true

                } else {

                    notify(getString(R.string.frag_cc_digital_resolution_invalid))
                }
            } else {

                notify(getString(R.string.frag_cc_digital_resolution_limit_met))

            }

        }

        return false
    }
}