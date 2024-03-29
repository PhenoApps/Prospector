package org.phenoapps.prospector.fragments.preferences

import android.os.Bundle
import android.view.View
import androidx.appcompat.widget.Toolbar
import androidx.navigation.findNavController
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.WithFragmentBindings
import org.phenoapps.prospector.NavigationRootDirections
import org.phenoapps.prospector.R
import org.phenoapps.prospector.fragments.ConnectionFragment

@AndroidEntryPoint
class DatabaseToolbarSettingsFragment : ConnectionFragment(R.layout.fragment_database_settings) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with (view.findViewById<Toolbar>(R.id.toolbar)) {

            setTitle(R.string.database_title)

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
