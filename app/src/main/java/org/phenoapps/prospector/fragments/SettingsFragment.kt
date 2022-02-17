package org.phenoapps.prospector.fragments

import DEVICE_INNO_SPECTRA
import DEVICE_IOT_LIST
import DEVICE_IP
import DEVICE_LINK_SQUARE
import DEVICE_MANUFACTURER
import DEVICE_PASSWORD
import DEVICE_PORT
import DEVICE_SSID
import DEVICE_TYPE
import OPERATOR
import android.os.Bundle
import android.text.InputType
import android.util.Log
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.preference.*
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.WithFragmentBindings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.phenoapps.prospector.R
import org.phenoapps.prospector.activities.MainActivity
import org.phenoapps.prospector.data.viewmodels.devices.LinkSquareViewModel
import org.phenoapps.prospector.utils.KeyUtil
import org.phenoapps.prospector.utils.LinkSquare
import org.phenoapps.prospector.utils.buildLinkSquareDeviceInfo
import org.phenoapps.prospector.utils.observeOnce

/**
 * Simple pref fragment that listens for connected devices and can scan subnets for IoT devices. (uses activity view model)
 *
 * TODO: update scan subnet with list of found devices for the user to choose from.
 */
@WithFragmentBindings
@AndroidEntryPoint
class SettingsFragment : PreferenceFragmentCompat(), CoroutineScope by MainScope() {

    private val mKeyUtil by lazy {
        KeyUtil(context)
    }

    //private val scope = CoroutineScope(Dispatchers.IO)

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {

        //val deviceViewModel = (activity as MainActivity).sDeviceViewModel

        setPreferencesFromResource(R.xml.preferences, rootKey)

        with(findPreference<EditTextPreference>(mKeyUtil.targetScans)) {

            //androidx sets input type of et preferences (doesn't work in xml)
            this?.setOnBindEditTextListener {
                it.inputType = InputType.TYPE_CLASS_NUMBER
            }

            this?.setOnPreferenceChangeListener { preference, newValue ->

                when (val target = newValue as? Int) {
                    is Int -> {
                        this.summary = getString(R.string.pref_workflow_target_scan_set_summary, target)
                    }
                    else -> {
                        this.text = ""
                        this.summary = getString(R.string.pref_workflow_target_summary)
                    }
                }

                true
            }
        }

        //sets pref summary to the inputted operator name
        findPreference<EditTextPreference>(OPERATOR)?.setOnPreferenceChangeListener { preference, newValue ->

            preference.summary = newValue as? String ?: ""

            true

        }

        findPreference<Preference>(DEVICE_LINK_SQUARE)?.let { devicePref ->

            devicePref.setOnPreferenceClickListener {

                findNavController().navigate(SettingsContainerFragmentDirections
                    .actionToLinksquareSettingsFragment())

                true
            }
        }

        findPreference<Preference>(DEVICE_INNO_SPECTRA)?.let { devicePref ->

            devicePref.setOnPreferenceClickListener {

                findNavController().navigate(SettingsContainerFragmentDirections
                    .actionToInnoSpectraSettingsFragment())

                true
            }
        }
    }

    override fun onResume() {
        super.onResume()

        (activity as? MainActivity)?.setToolbar(R.id.action_nav_settings)
    }
}
