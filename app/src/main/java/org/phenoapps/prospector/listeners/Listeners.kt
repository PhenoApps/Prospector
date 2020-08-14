package org.phenoapps.prospector.listeners

import ALPHA_ASC
import ALPHA_DESC
import DATE_ASC
import DATE_DESC
import android.content.Context
import android.view.View
import android.widget.Button
import android.widget.ToggleButton
import androidx.core.content.ContextCompat
import org.phenoapps.icons.R as icons

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

/**
 * Returned integer states:
 * 0 -> Alphabetic Descending
 * 1 -> Alphabetic Ascending
 * 2 -> Date Descending
 * 3 -> Date Ascending
 */
open class SortToggleClickListener(private val context: Context, private val function: (Int) -> Unit) : View.OnClickListener {

    val sortAlphaAsc by lazy { ContextCompat.getDrawable(context, icons.drawable.ic_sort_by_alpha_white_ascending_18dp) }
    val sortAlphaDesc by lazy { ContextCompat.getDrawable(context, icons.drawable.ic_sort_by_alpha_white_descending_18dp) }
    val sortDateAsc by lazy { ContextCompat.getDrawable(context, icons.drawable.ic_sort_by_date_ascending_18dp) }
    val sortDateDesc by lazy { ContextCompat.getDrawable(context, icons.drawable.ic_sort_by_date_descending_18dp) }

    override fun onClick(v: View?) {

        (v as? Button)?.let { btn ->

            when (btn.background) {

                sortAlphaAsc -> {

                    btn.setBackgroundResource(icons.drawable.ic_sort_by_date_descending_18dp)

                    function(DATE_DESC)
                }

                sortAlphaDesc -> {

                    btn.setBackgroundResource(icons.drawable.ic_sort_by_alpha_white_ascending_18dp)

                    function(ALPHA_ASC)
                }

                sortDateAsc -> {

                    btn.setBackgroundResource(icons.drawable.ic_sort_by_alpha_white_descending_18dp)

                    function(ALPHA_DESC)
                }

                sortDateDesc -> {

                    btn.setBackgroundResource(icons.drawable.ic_sort_by_date_ascending_18dp)

                    function(DATE_ASC)

                }
            }
        }
    }
}