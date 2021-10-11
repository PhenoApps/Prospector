package org.phenoapps.prospector.fragments

import DEVICE_IOT_LIST
import DEVICE_IP
import DEVICE_PASSWORD
import DEVICE_PORT
import DEVICE_SSID
import DEVICE_TYPE
import OPERATOR
import android.os.Bundle
import android.text.InputType
import android.util.Log
import androidx.fragment.app.activityViewModels
import androidx.preference.*
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.WithFragmentBindings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.phenoapps.prospector.R
import org.phenoapps.prospector.activities.MainActivity
import org.phenoapps.prospector.data.viewmodels.DeviceViewModel
import org.phenoapps.prospector.data.viewmodels.MainActivityViewModel_Factory
import org.phenoapps.prospector.utils.KeyUtil
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

    private val sDeviceViewModel: DeviceViewModel by activityViewModels()

    private val mKeyUtil by lazy {
        KeyUtil(context)
    }

    private val scope = CoroutineScope(Dispatchers.IO)

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {

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

        //set iot device list to disabled, enable it when devcies are found
        with(findPreference<Preference>(DEVICE_IOT_LIST)) {

           this?.summary = getString(R.string.pref_iot_list_no_devices)

        }

        //sets pref summary to the inputted operator name
        findPreference<EditTextPreference>(OPERATOR)?.setOnPreferenceChangeListener { preference, newValue ->

            preference.summary = newValue as? String ?: ""

            true

        }

        //this is the connect IoT button which searches the subnet
        findPreference<Preference>(DEVICE_IOT_LIST)?.setOnPreferenceClickListener { pref ->

            pref.summary = getString(R.string.pref_device_iot_searching)

            sDeviceViewModel.scanArp().observeOnce(viewLifecycleOwner, {

                if (it == "fail") { //back down to the brute force network search

                    Log.d("LSSearch", "Starting brute force search.")

                    sDeviceViewModel.scanSubNet().observeOnce(viewLifecycleOwner, {

                        pref.summary = it

                        findPreference<EditTextPreference>(DEVICE_IP)?.text = it

                        scope.launch {

                            sDeviceViewModel.connect(requireContext())

                            buildDeviceSummary()

                        }

                    })

                } else { //use arp to find TI devices and connect if found

                    pref.summary = it

                    findPreference<EditTextPreference>(DEVICE_IP)?.text = it

                    scope.launch {

                        sDeviceViewModel.connect(requireContext())

                        buildDeviceSummary()

                    }
                }
            })

            true
        }

        //this is less of a device type and more of a device info placeholder
        findPreference<Preference>(DEVICE_TYPE)?.let { etPref ->

            buildDeviceSummary()
        }

//        //sets device alias
//        findPreference<EditTextPreference>(DEVICE_ALIAS)?.setOnPreferenceChangeListener { _, newValue ->
//
//            launch {
//
//                sDeviceViewModel.setAlias(newValue as? String ?: String())
//
//            }

//            true
//
//        }

        findPreference<EditTextPreference>(DEVICE_SSID)?.setOnPreferenceChangeListener { preference, newValue ->

            val mode = newValue as? Boolean ?: true

            preference.summary = if (mode) "AP" else "IoT"

            true

        }

        findPreference<EditTextPreference>(DEVICE_PASSWORD)?.setOnPreferenceChangeListener { _, _ ->

            setWLanInfo()

            true

        }

        findPreference<EditTextPreference>(DEVICE_SSID)?.setOnPreferenceChangeListener { _, _ ->

            setWLanInfo()

            true

        }

//        findPreference<ListPreference>(DEVICE_WIFI_MODE)?.setOnPreferenceChangeListener { preference, newValue ->
//
//            val mode = newValue as? Boolean ?: true
//
//            preference.summary = if (mode) "AP" else "IoT"
//
//            true
//
//        }

        findPreference<EditTextPreference>(DEVICE_IP)?.setOnPreferenceChangeListener { _, _ ->

            scope.launch {

                sDeviceViewModel.connect(requireContext())

                buildDeviceSummary()
            }

            true

        }

        findPreference<EditTextPreference>(DEVICE_PORT)?.setOnPreferenceChangeListener { _, _ ->

            scope.launch {

                sDeviceViewModel.connect(requireContext())

            }

            true

        }
    }

    private fun buildDeviceSummary() {

        scope.launch {

            val summary = when (sDeviceViewModel.isConnected()) {

                true -> buildLinkSquareDeviceInfo(requireContext(),
                    sDeviceViewModel.getDeviceInfo())

                else -> sDeviceViewModel.getDeviceError()

            }

            if (isAdded) {

                activity?.runOnUiThread {

                    findPreference<Preference>(DEVICE_TYPE)?.summary = summary
                }
            }
        }
    }

    private fun setWLanInfo() {

        val ssid = findPreference<EditTextPreference>(DEVICE_SSID)
        val password = findPreference<EditTextPreference>(DEVICE_PASSWORD)

        scope.launch {

            sDeviceViewModel.setWLanInfoAsync(
                ssid?.text ?: "",
                password?.text ?: "",
                DeviceViewModel.WPA).await()

        }
    }

    override fun onResume() {
        super.onResume()

        (activity as? MainActivity)?.setToolbar(R.id.action_nav_settings)
    }
}
