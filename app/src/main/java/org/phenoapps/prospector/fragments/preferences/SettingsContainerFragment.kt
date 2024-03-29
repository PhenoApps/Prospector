package org.phenoapps.prospector.fragments.preferences

import android.os.Bundle
import android.view.View
import androidx.appcompat.widget.Toolbar
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import org.phenoapps.prospector.R
import org.phenoapps.prospector.fragments.ConnectionFragment

@AndroidEntryPoint
class SettingsContainerFragment: ConnectionFragment(R.layout.fragment_settings) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<Toolbar>(R.id.toolbar)?.setNavigationOnClickListener {
            findNavController().popBackStack()
        }
    }
}