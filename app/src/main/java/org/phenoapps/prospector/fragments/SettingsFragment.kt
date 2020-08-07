package org.phenoapps.prospector.fragments

import DEVICE_IP
import DEVICE_PORT
import DEVICE_TYPE
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.preference.EditTextPreference
import androidx.preference.ListPreference
import androidx.preference.PreferenceFragmentCompat
import kotlinx.coroutines.*
import org.phenoapps.prospector.R
import org.phenoapps.prospector.data.viewmodels.DeviceViewModel
import org.phenoapps.prospector.utils.buildLinkSquareDeviceInfo


class SettingsFragment : PreferenceFragmentCompat(), CoroutineScope by MainScope() {

    private val sDeviceViewModel: DeviceViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        startObserver()

        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {

        setPreferencesFromResource(R.xml.preferences, rootKey)

        findPreference<EditTextPreference>(DEVICE_IP)?.setOnPreferenceChangeListener { preference, newValue ->

            launch {

                sDeviceViewModel.connect(requireContext())

            }

            Handler().postDelayed({

                startObserver()

            }, 100)


            true

        }

        findPreference<EditTextPreference>(DEVICE_PORT)?.setOnPreferenceChangeListener { preference, newValue ->

            launch {

                sDeviceViewModel.connect(requireContext())

            }

            Handler().postDelayed({

                startObserver()

            }, 100)

            true

        }
    }

    private fun startObserver() {

        findPreference<ListPreference>(DEVICE_TYPE)?.let { etPref ->

            launch {

                withContext(Dispatchers.IO) {

                    val connected = sDeviceViewModel.isConnected()

                    val summary = when (connected) {

                        true -> buildLinkSquareDeviceInfo(requireContext(), sDeviceViewModel.getDeviceInfo())

                        else -> sDeviceViewModel.getDeviceError()

                    }

                    if (isAdded) {

                        this@SettingsFragment.requireActivity().runOnUiThread {

                            etPref.summary = summary
                        }
                    }
                }
            }
        }
    }
}
