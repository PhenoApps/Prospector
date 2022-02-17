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
import dagger.hilt.android.WithFragmentBindings
import org.phenoapps.prospector.R
import org.phenoapps.prospector.activities.MainActivity
import org.phenoapps.prospector.fragments.InnoSpectraToolbarSettingsFragmentDirections

@AndroidEntryPoint
class MethodFragment: Fragment(R.layout.fragment_config_creator_method) {

    private val args: MethodFragmentArgs by navArgs()

    private var uiMethodListView: ListView? = null
    private var uiBackButton: Button? = null
    private var uiNextButton: Button? = null

    private var mSelectedMethod: String? = null
    private var mSelectedIndex: Int = 0

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        uiMethodListView = view.findViewById(R.id.frag_cc_method_lv)
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
            if (args.config.currentIndex > 0) {
                args.config.currentIndex--
            }
            findNavController().popBackStack()
        }

        uiNextButton?.setOnClickListener {
            findNavController().navigate(MethodFragmentDirections
                .actionMethodFragmentToSpectralRangeFragment(args.config.apply {
                    this.sections?.get(this.currentIndex)?.apply {
                        this.method = mSelectedMethod ?: "Column"
                        this.methodIndex = mSelectedIndex
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

            val methods = arrayOf(getString(R.string.method_column), getString(R.string.method_hadamard))

            val adapter = ArrayAdapter(act, android.R.layout.simple_list_item_checked, methods)

            uiMethodListView?.adapter = adapter

            uiMethodListView?.setOnItemClickListener { adapterView, view, i, l ->
                with (view as CheckedTextView) {
                    isChecked = !isChecked
                    mSelectedMethod = if (isChecked) view.text.toString() else null
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

            (uiMethodListView?.adapter as? ArrayAdapter<*>)?.notifyDataSetChanged()

        }
    }
}