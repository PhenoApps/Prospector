package org.phenoapps.prospector.fragments.preferences.indigo

import android.os.Bundle
import androidx.navigation.fragment.findNavController
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import org.phenoapps.prospector.R
import org.phenoapps.prospector.utils.KeyUtil

class IndigoSettingsFragment: PreferenceFragmentCompat() {

    private val keys by lazy {
        KeyUtil(context)
    }

    private val libKeys by lazy {
        org.phenoapps.utils.KeyUtil(context)
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.indigo_preferences, rootKey)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setupIndigoSearchPref()
    }

    private fun setupIndigoSearchPref() {
        findPreference<Preference>(keys.indigoSearchDevice)?.let { pref ->
            pref.setOnPreferenceClickListener {

                findNavController().navigate(IndigoSettingsContainerFragmentDirections
                    .actionIndigoSettingsToDeviceSearch())

                true
            }

            val summary = preferenceManager.sharedPreferences.getString(libKeys.argBluetoothDeviceAddress, null)
            if (summary != null) {
                pref.summary = summary
            }
        }
    }

    override fun onResume() {
        super.onResume()
        setupIndigoSearchPref()
    }
}