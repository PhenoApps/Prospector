package org.phenoapps.prospector.fragments.nano_configuration_creator

import android.content.Context
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
class WidthFragment: Fragment(R.layout.fragment_config_creator_width) {

    private val args: WidthFragmentArgs by navArgs()

    private var uiListView: ListView? = null
    private var uiBackButton: Button? = null
    private var uiNextButton: Button? = null

    private var mSelectedWidth: String? = null
    private var mSelectedIndex: Int? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        uiListView = view.findViewById(R.id.frag_cc_width_lv)
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

    private fun setupButtons() {

        uiBackButton?.setOnClickListener {
            findNavController().popBackStack()
        }

        uiNextButton?.setOnClickListener {
            findNavController().navigate(WidthFragmentDirections
                .actionWidthFragmentToDigitalResolutionFragment(args.config.apply {
                    this.sections?.get(this.currentIndex)?.apply {
                        this.width = mSelectedWidth?.toDoubleOrNull() ?: 2.34
                        this.widthIndex = mSelectedIndex ?: 0
                    }
                }))
        }
    }

    private fun setupToolbar() {
        val toolbar = view?.findViewById<Toolbar>(R.id.toolbar)

        toolbar?.setNavigationOnClickListener {
            findNavController().popBackStack()
        }
    }

    //iterates over all list items and changes button text depending on if user selected one
    private fun checkButtonText(view: AdapterView<*>) {
        uiNextButton?.isEnabled = view.children.any { (it as CheckedTextView).isChecked }
    }

    private fun setupAdapter() {

        activity?.let { act ->

            val widths = resources.getStringArray(R.array.inno_spectra_widths)

            val adapter = WidthAdapter(act, widths)

            uiListView?.adapter = adapter

            uiListView?.setOnItemClickListener { adapterView, view, i, l ->

                with (view as CheckedTextView) {
                    isChecked = !isChecked
                    mSelectedWidth = if (isChecked) view.text.toString() else null
                    mSelectedIndex = if (isChecked) i else null
                }

                //uncheck all other selections
                adapterView?.children?.forEachIndexed { _, ctv ->
                    with (ctv as CheckedTextView) {
                        if (ctv.text.toString() != mSelectedWidth) this.isChecked = false
                    }
                }

                checkButtonText(adapterView)
            }

            (uiListView?.adapter as? ArrayAdapter<*>)?.notifyDataSetChanged()

        }
    }

    //adapter that disables recycling, which is fine for a small list of strings s.a widths
    private inner class WidthAdapter(context: Context, widths: Array<String>):
        ArrayAdapter<String>(context, android.R.layout.simple_list_item_checked, widths) {

        override fun getViewTypeCount(): Int {
            return count
        }

        override fun getItemViewType(position: Int): Int {
            return position
        }
    }
}