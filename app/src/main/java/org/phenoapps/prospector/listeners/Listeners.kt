package org.phenoapps.prospector.listeners

import android.view.View
import android.widget.ToggleButton

open class ToggleClickListener(private val function: (Boolean) -> Unit) : View.OnClickListener {

    override fun onClick(v: View?) {

        (v as? ToggleButton)?.let { toggle ->

            val state = toggle.text

            function(when (state) {

                toggle.textOn -> true

                else -> false

            })
        }
    }
}