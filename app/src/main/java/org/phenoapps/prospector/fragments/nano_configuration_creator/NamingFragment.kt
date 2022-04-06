package org.phenoapps.prospector.fragments.nano_configuration_creator

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toolbar
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import dagger.hilt.android.AndroidEntryPoint
import org.phenoapps.prospector.R
import org.phenoapps.prospector.activities.MainActivity
import org.phenoapps.prospector.data.viewmodels.devices.InnoSpectraViewModel
import org.phenoapps.prospector.fragments.InnoSpectraToolbarSettingsFragmentDirections
import org.phenoapps.prospector.fragments.nano_configuration_creator.models.Config

@AndroidEntryPoint
class NamingFragment: Fragment(R.layout.fragment_config_creator_naming) {

    private val args: NamingFragmentArgs by navArgs()

    private var uiNameEditText: EditText? = null
    private var uiAverageEditText: EditText? = null
    private var uiBackButton: Button? = null
    private var uiNextButton: Button? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        uiNameEditText = view.findViewById(R.id.frag_cc_naming_name_et)
        uiAverageEditText = view.findViewById(R.id.frag_cc_naming_average_et)
        uiBackButton = view.findViewById(R.id.frag_cc_back_btn)
        uiNextButton = view.findViewById(R.id.frag_cc_next_btn)

        view.findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)?.title = ""

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
            activity?.onBackPressed()
        }

        uiNextButton?.setOnClickListener {
            val name = uiNameEditText?.text?.toString() ?: ""
            val repeats = uiAverageEditText?.text?.toString()?.toIntOrNull() ?: 1
            if (verifyUi()) {
                findNavController().navigate(NamingFragmentDirections
                    .actionNamingFragmentToSectionsFragment(Config(name, repeats)))
            }
        }
    }

    private fun verifyUi(): Boolean {

        with (activity as MainActivity) {

            val name = uiNameEditText?.text?.toString() ?: String()
            val average = uiAverageEditText?.text?.toString()?.toIntOrNull() ?: 0

            if (name.isNotBlank()) {

                if (!checkNameExists(name)) {

                    if (average in 1..65535) {

                        return true

                    } else {

                        notify(getString(R.string.frag_cc_invalid_average_range))

                    }

                } else {

                    notify(getString(R.string.frag_cc_naming_exists))

                }

            } else {

                notify(getString(R.string.frag_cc_naming_blank))
            }
        }

        return false
    }

    private fun checkNameExists(name: String) = args.names?.any { it == name } ?: false
}