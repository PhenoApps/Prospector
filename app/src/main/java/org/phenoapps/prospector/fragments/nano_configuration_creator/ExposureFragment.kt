package org.phenoapps.prospector.fragments.nano_configuration_creator

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.widget.Toolbar
import androidx.core.view.children
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import dagger.hilt.android.AndroidEntryPoint
import org.phenoapps.prospector.R

@AndroidEntryPoint
class ExposureFragment: Fragment(R.layout.fragment_config_creator_exposure) {

    private val args: ExposureFragmentArgs by navArgs()

    private var uiListView: ListView? = null
    private var uiBackButton: Button? = null
    private var uiNextButton: Button? = null

    private var mSelectedExposure: String? = null
    private var mSelectedIndex: Int = 0

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        uiListView = view.findViewById(R.id.frag_cc_exposure_lv)
        uiBackButton = view.findViewById(R.id.frag_cc_back_btn)
        uiNextButton = view.findViewById(R.id.frag_cc_next_btn)

        uiNextButton?.isEnabled = false

        val section = "${args.config.currentIndex + 1}"
        view.findViewById<Toolbar>(R.id.toolbar)?.title = getString(R.string.section, section)

        view.findViewById<ProgressBar>(R.id.frag_cc_pb)?.progress =
            args.config.currentIndex * (100.0 / (args.config.sections?.size ?: 1)).toInt()

        setupAdapter()
        setupButtons()
        setupToolbar()
    }

    private fun setupToolbar() {
        val toolbar = view?.findViewById<Toolbar>(R.id.toolbar)

        toolbar?.setNavigationOnClickListener {
            findNavController().popBackStack()
        }
    }

    private fun setupButtons() {

        uiBackButton?.setOnClickListener {
            findNavController().popBackStack()
        }

        uiNextButton?.setOnClickListener {
            findNavController().navigate(ExposureFragmentDirections
                .actionExposureFragmentToWidthFragment(args.config.apply {
                    this.sections?.get(this.currentIndex)?.apply {
                        this.exposure = mSelectedExposure?.toDoubleOrNull() ?: 0.635
                        this.exposureIndex = mSelectedIndex
                    }
                }))
        }
    }

    //iterates over all list items and changes button text depending on if user selected one
    private fun checkButtonText(view: AdapterView<*>) {
        uiNextButton?.isEnabled = view.children.any { (it as CheckedTextView).isChecked }
    }

    private fun setupAdapter() {

        activity?.let { act ->

            val times = resources.getStringArray(R.array.inno_spectra_exposure_times)

            val adapter = ArrayAdapter(act, android.R.layout.simple_list_item_checked, times)

            uiListView?.adapter = adapter

            uiListView?.setOnItemClickListener { adapterView, view, i, l ->
                with (view as CheckedTextView) {
                    isChecked = !isChecked
                    mSelectedExposure = if (isChecked) view.text.toString() else null
                    mSelectedIndex = if (isChecked) i else 0
                }

                //uncheck all other selections
                adapterView?.children?.forEachIndexed { index, ctv ->
                    with (ctv as CheckedTextView) {
                        if (index != i) this.isChecked = false
                    }
                }

                checkButtonText(adapterView)
            }

            (uiListView?.adapter as? ArrayAdapter<*>)?.notifyDataSetChanged()

        }
    }
}