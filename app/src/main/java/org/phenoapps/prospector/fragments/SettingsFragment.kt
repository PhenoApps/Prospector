package org.phenoapps.prospector.fragments

import android.os.Bundle

import androidx.preference.PreferenceFragmentCompat

import org.phenoapps.prospector.R


class SettingsFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)
    }

    companion object {

        const val packageId = "org.phenoapps.prospector"

        const val OPERATOR = "$packageId.OPERATOR"

        const val LED_FRAMES = "$packageId.LED_FRAMES"

        const val BULB_FRAMES = "$packageId.BULB_FRAMES"

        const val AUTO_SCAN_NAME = "$packageId.AUTO_NAMED_SCAN"
    }
}
