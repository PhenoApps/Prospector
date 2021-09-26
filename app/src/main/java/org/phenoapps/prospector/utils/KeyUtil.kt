package org.phenoapps.prospector.utils

import android.content.Context
import org.phenoapps.prospector.R
import kotlin.properties.ReadOnlyProperty

/**
 * Utility class for easily accessing preference keys.
 * Converts keys.xml into string fields to be accessed within a context.
 */
class KeyUtil(private val ctx: Context?) {

    private fun key(id: Int): ReadOnlyProperty<Any?, String> =
        ReadOnlyProperty { _, _ -> ctx?.getString(id)!! }

    val lastSelectedGraph by key(R.string.key_pref_last_graph_selected)

    val targetScans by key(R.string.key_pref_target_scan)

    val audioEnabled by key(R.string.key_pref_audio_enabled)

    val sampleScanEnabled by key(R.string.key_pref_workflow_new_sample_by_scan)

}