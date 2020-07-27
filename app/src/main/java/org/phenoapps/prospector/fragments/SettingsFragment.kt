package org.phenoapps.prospector.fragments

import DEVICE_IP
import DEVICE_PORT
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.preference.EditTextPreference

import androidx.preference.PreferenceFragmentCompat
import com.stratiotechnology.linksquareapi.LinkSquareAPI

import org.phenoapps.prospector.R
import org.phenoapps.prospector.data.viewmodels.DeviceViewModel
import org.phenoapps.prospector.utils.buildLinkSquareDeviceInfo


class SettingsFragment : PreferenceFragmentCompat() {

    private val sDeviceViewModel: DeviceViewModel by activityViewModels()

    private var mDeviceInfo: String = String()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        startObserver()

        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {

        setPreferencesFromResource(R.xml.preferences, rootKey)

        findPreference<EditTextPreference>(DEVICE_IP)?.setOnPreferenceChangeListener { preference, newValue ->

            Handler().postDelayed({

                sDeviceViewModel.reset()

                startObserver()

            }, 50)


            true

        }

        findPreference<EditTextPreference>(DEVICE_PORT)?.setOnPreferenceChangeListener { preference, newValue ->

            Handler().postDelayed({

                sDeviceViewModel.reset()

                startObserver()

            }, 50)

            true

        }
    }

    override fun onDestroyView() {

        sDeviceViewModel.connection(requireActivity()).removeObserver(connectionObserver)

        super.onDestroyView()
    }

    private val connectionObserver = Observer<Any> {

        findPreference<EditTextPreference>(DEVICE_IP)?.let { etPref ->

            it?.let { result ->

                when (result) {

                    is LinkSquareAPI.LSDeviceInfo -> {

                        mDeviceInfo = buildLinkSquareDeviceInfo(requireActivity(), result)

                        etPref.summary = mDeviceInfo

                    }

                    is String -> {

                        etPref.summary = "${requireActivity().getString(R.string.device_error_prefix)} $result"

                    }

                    is Int -> {

                        if (result == -1) {

                            etPref.summary = requireActivity().getString(R.string.connecting)

                        }
                    }
                }
            }
        }
    }

    private fun startObserver() {

        findPreference<EditTextPreference>(DEVICE_IP)?.let { etPref ->

            if (!sDeviceViewModel.isConnected()) {

                sDeviceViewModel.connection(requireActivity()).observe(viewLifecycleOwner, connectionObserver)

            } else etPref.summary = buildLinkSquareDeviceInfo(requireActivity(), sDeviceViewModel.getDeviceInfo())
        }
    }
}
