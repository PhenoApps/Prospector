package org.phenoapps.prospector.fragments.preferences.indigo

import android.graphics.Color
import org.phenoapps.fragments.toolbar.PopOnBackToolbarFragment

class IndigoSettingsContainerFragment: PopOnBackToolbarFragment() {

    override val containedFragment = IndigoSettingsFragment()

    override val topToolbarColor: Int = Color.parseColor("#03A9F4")
}