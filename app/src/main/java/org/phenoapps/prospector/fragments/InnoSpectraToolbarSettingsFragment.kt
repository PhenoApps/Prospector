package org.phenoapps.prospector.fragments

import android.os.Bundle
import android.view.View
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.WithFragmentBindings
import org.phenoapps.prospector.NavigationRootDirections
import org.phenoapps.prospector.R
import org.phenoapps.prospector.activities.MainActivity
import java.util.*

@WithFragmentBindings
@AndroidEntryPoint
class InnoSpectraToolbarSettingsFragment : ConnectionFragment(R.layout.fragment_inno_settings) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with (view.findViewById<Toolbar>(R.id.toolbar)) {
            setNavigationOnClickListener {
                findNavController().popBackStack()
            }

            setOnMenuItemClickListener {
                if (it.itemId == R.id.action_connection) {
                    findNavController().navigate(
                        NavigationRootDirections
                        .actionToConnectInstructions())
                }
                true
            }
        }
    }
}
