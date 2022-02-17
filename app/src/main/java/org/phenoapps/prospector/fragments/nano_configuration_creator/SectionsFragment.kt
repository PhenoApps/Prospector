package org.phenoapps.prospector.fragments.nano_configuration_creator

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.WithFragmentBindings
import org.phenoapps.prospector.R
import org.phenoapps.prospector.activities.MainActivity
import org.phenoapps.prospector.fragments.InnoSpectraToolbarSettingsFragmentDirections
import org.phenoapps.prospector.fragments.nano_configuration_creator.models.Section

@AndroidEntryPoint
class SectionsFragment: Fragment(R.layout.fragment_config_creator_num_sections) {

    private val args: SectionsFragmentArgs by navArgs()

    private var uiSectionsEditText: EditText? = null
    private var uiBackButton: Button? = null
    private var uiNextButton: Button? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        uiSectionsEditText = view.findViewById(R.id.frag_cc_sections_et)
        uiBackButton = view.findViewById(R.id.frag_cc_back_btn)
        uiNextButton = view.findViewById(R.id.frag_cc_next_btn)

        view.findViewById<Toolbar>(R.id.toolbar)?.title = ""

        setupButtonListeners()
        setupToolbar()
    }

    private fun setupToolbar() {
        val toolbar = view?.findViewById<Toolbar>(R.id.toolbar)

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
                val sections = uiSectionsEditText?.text?.toString()?.toIntOrNull() ?: 0
                findNavController().navigate(SectionsFragmentDirections
                    .actionSectionsFragmentToMethodFragment(args.config.apply {
                        val temp = arrayListOf<Section>()
                        for (i in 0 until sections) {
                            temp.add(Section(
                                method = "Column",
                                methodIndex = 0,
                                start = 900f,
                                end = 1700f,
                                width = 2.34,
                                widthIndex = 0,
                                exposure = 0.635,
                                exposureIndex = 0,
                                resolution = 0)
                            )
                        }
                        this.sections = temp.toTypedArray()
                    }))
            }
        }
    }

    private fun verifyUi(): Boolean {

        with (activity as MainActivity) {

            val sections = uiSectionsEditText?.text?.toString()?.toIntOrNull() ?: 0

            if (sections in 1..5) {

                return true

            } else {

                notify(getString(R.string.frag_cc_sections_invalid))
            }
        }

        return false
    }
}