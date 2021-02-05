package org.phenoapps.prospector.fragments

import DEVICE_ALIAS
import DEVICE_IOT_LIST
import DEVICE_IP
import DEVICE_PASSWORD
import DEVICE_PORT
import DEVICE_SSID
import DEVICE_TYPE
import OPERATOR
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import kotlinx.coroutines.*
import org.phenoapps.prospector.R
import org.phenoapps.prospector.activities.MainActivity
import org.phenoapps.prospector.data.viewmodels.DeviceViewModel
import org.phenoapps.prospector.utils.buildLinkSquareDeviceInfo
import org.phenoapps.prospector.utils.observeOnce

/**
 * Simple pref fragment that listens for connected devices and can scan subnets for IoT devices. (uses activity view model)
 *
 * TODO: update scan subnet with list of found devices for the user to choose from.
 */
class SettingsFragment : PreferenceFragmentCompat(), CoroutineScope by MainScope() {

    private val sDeviceViewModel: DeviceViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        startObserver()

        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {

        setPreferencesFromResource(R.xml.preferences, rootKey)

        //set iot device list to disabled, enable it when devcies are found
        with(findPreference<Preference>(DEVICE_IOT_LIST)) {

           this?.summary = getString(R.string.pref_iot_list_no_devices)

        }

        //sets pref summary to the inputted operator name
        findPreference<EditTextPreference>(OPERATOR)?.setOnPreferenceChangeListener { preference, newValue ->

            preference.summary = newValue as? String ?: ""

            true

        }

        //sets device alias
        findPreference<EditTextPreference>(DEVICE_ALIAS)?.setOnPreferenceChangeListener { preference, newValue ->

            launch {

                sDeviceViewModel.setAlias(newValue as? String ?: String())

            }

            Handler().postDelayed({

                startObserver()

            }, 100)


            true

        }

        findPreference<EditTextPreference>(DEVICE_SSID)?.setOnPreferenceChangeListener { preference, newValue ->

            val mode = newValue as? Boolean ?: true

            preference.summary = if (mode) "AP" else "IoT"

            true

        }

        findPreference<EditTextPreference>(DEVICE_PASSWORD)?.setOnPreferenceChangeListener { preference, newValue ->

            startDeviceDiscovery()

            true

        }

        findPreference<EditTextPreference>(DEVICE_SSID)?.setOnPreferenceChangeListener { preference, newValue ->

            startDeviceDiscovery()

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

        findPreference<EditTextPreference>(DEVICE_IP)?.setOnPreferenceChangeListener { preference, newValue ->

            launch {

                sDeviceViewModel.connect(requireContext())

            }

            true

        }

        findPreference<EditTextPreference>(DEVICE_PORT)?.setOnPreferenceChangeListener { preference, newValue ->

            launch {

                sDeviceViewModel.connect(requireContext())

            }

            true

        }
    }

    private fun startDeviceDiscovery() {

        val ssid = findPreference<EditTextPreference>(DEVICE_SSID)
        val password = findPreference<EditTextPreference>(DEVICE_PASSWORD)

        launch {

            val result = sDeviceViewModel.setWLanInfoAsync(
                    ssid?.text ?: "",
                    password?.text ?: "",
                    DeviceViewModel.WPA).await()

//            activity?.runOnUiThread {
//
//                Toast.makeText(context, "Password ${if (result) "saved" else "failed"}", Toast.LENGTH_SHORT).show()
//            }
        }
    }

    private fun startObserver() {

        //this is the connect IoT button which searches the subnet
        findPreference<Preference>(DEVICE_IOT_LIST)?.setOnPreferenceClickListener { pref ->

            pref.summary = getString(R.string.pref_device_iot_searching)

            sDeviceViewModel.scanSubNet().observeOnce(viewLifecycleOwner, {

                pref.summary = it

                findPreference<EditTextPreference>(DEVICE_IP)?.text = it

            })

            true
        }

        //this is less of a device type and more of a device info placeholder
        findPreference<Preference>(DEVICE_TYPE)?.let { etPref ->

            launch {

                withContext(Dispatchers.IO) {

                    val summary = when (sDeviceViewModel.isConnected()) {

                        true -> buildLinkSquareDeviceInfo(requireContext(),
                                (activity as? MainActivity)?.sDeviceViewModel?.getDeviceInfo())

                        else -> (activity as? MainActivity)?.sDeviceViewModel?.getDeviceError()

                    }

                    if (isAdded) {

                        activity?.runOnUiThread {

                            etPref.summary = summary
                        }
                    }
                }
            }
        }
    }
}
