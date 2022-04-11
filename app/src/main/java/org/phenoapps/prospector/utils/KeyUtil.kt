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

    val returnFromNewConfig by key(R.string.key_pref_return_from_new_config)

    val lastSelectedGraph by key(R.string.key_pref_last_graph_selected)

    val lastConnectedDeviceId by key(R.string.key_pref_last_connected_device_id)

    val targetScans by key(R.string.key_pref_target_scan)

    val audioEnabled by key(R.string.key_pref_audio_enabled)

    val sampleScanEnabled by key(R.string.key_pref_workflow_new_sample_by_scan)

    //keys for device manufacturer type
    val deviceMaker by key(R.string.key_device_maker)

    //keys for operator update timer
    val operator by key(R.string.key_operator)
    val verifyOperator by key(R.string.key_pref_profile_verify_operator)
    val argOpenOperatorSettings by key(R.string.key_arg_update_person)
    val firstInstall by key(R.string.key_pref_first_install)
    val lastTimeAppOpened by key(R.string.key_pref_last_app_open)

    //keys for inno spectra preferences
    val innoInformation by key(R.string.key_pref_inno_spectra_device_info)
    val innoStatus by key(R.string.key_pref_inno_spectra_device_status)
    val innoActive by key(R.string.key_pref_inno_spectra_active_config)
    val innoCreateNew by key(R.string.key_pref_inno_spectra_new_config)
    val innoConfigRestore by key(R.string.key_pref_inno_spectra_restore_configs)
    val innoConfig by key(R.string.key_pref_inno_spectra_config)

    //storage definer key
    val workflowDirectoryDefiner by key(R.string.key_pref_workflow_root_directory)

    //database preference settings keys
    val exportDatabase by key(R.string.key_pref_database_export)
    val importDatabase by key(R.string.key_pref_database_import)
    val database by key(R.string.key_pref_database)
    val deleteDatabase by key(R.string.key_pref_database_delete)

}