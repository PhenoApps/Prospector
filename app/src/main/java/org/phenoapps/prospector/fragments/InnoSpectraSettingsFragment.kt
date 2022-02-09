package org.phenoapps.prospector.fragments

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.fragment.app.setFragmentResultListener
import androidx.navigation.fragment.findNavController
import androidx.preference.*
import com.ISCSDK.ISCNIRScanSDK
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.WithFragmentBindings
import kotlinx.coroutines.*
import org.phenoapps.prospector.R
import org.phenoapps.prospector.activities.MainActivity
import org.phenoapps.prospector.data.viewmodels.devices.InnoSpectraViewModel
import org.phenoapps.prospector.utils.InnoSpectraUtil
import org.phenoapps.prospector.utils.KeyUtil

@WithFragmentBindings
@AndroidEntryPoint
class InnoSpectraSettingsFragment : PreferenceFragmentCompat(), CoroutineScope by MainScope() {

    companion object {
        const val TAG = "ISSettingsFrag"
    }

    data class DeviceInformation(val manufacturer: String, val modelNum: String, val serialNum: String, val hardwareRev: String, val tivRev: String)
    data class DeviceStatus(val battery: String, val totalLampTime: String, val temperature: String, val humidity: String)

    private val mKeyUtil by lazy {
        KeyUtil(context)
    }

    private val scope = CoroutineScope(Dispatchers.IO)

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {

        setPreferencesFromResource(R.xml.inno_spectra_preferences, rootKey)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val deviceViewModel = (activity as MainActivity).sDeviceViewModel

        if (deviceViewModel is InnoSpectraViewModel) {

            findPreference<Preference>(mKeyUtil.innoInformation)?.let { pref ->

                if (deviceViewModel.isConnected()) {

                    context?.let { ctx ->

                        scope.launch {

                            while (deviceViewModel.getDeviceInfo(ctx).isEmpty()) {
                                Log.d(TAG, "Waiting for device info...")
                            }

                            activity?.runOnUiThread {
                                pref.summary = deviceViewModel.getDeviceInfo(ctx)
                            }
                        }
                    }
                }
            }

            findPreference<Preference>(mKeyUtil.innoStatus)?.let { pref ->

                if (deviceViewModel.isConnected()) {

                    context?.let { ctx ->

                        scope.launch {

                            while (deviceViewModel.getDeviceStatus(ctx).isEmpty()) {
                                Log.d(TAG, "Waiting for device status.")
                            }

                            activity?.runOnUiThread {
                                pref.summary = deviceViewModel.getDeviceStatus(ctx)
                            }
                        }
                    }
                }
            }

            findPreference<Preference>(mKeyUtil.innoConfigRestore)?.setOnPreferenceClickListener {

                try {

                    InnoSpectraUtil.factoryReset()

                    deviceViewModel.refreshConfigs()

                    val prefs = PreferenceManager.getDefaultSharedPreferences(context)
                    prefs.edit().putInt(mKeyUtil.innoConfig, 0).apply()

                    scope.launch {

                        deviceViewModel.reset(context)

                        while (!deviceViewModel.isConnected()) {
                            delay(1000)
                        }

                        activity?.runOnUiThread {

                            setupScanConfigs()

                        }
                    }

                    Toast.makeText(context, R.string.frag_settings_inno_spectra_reset_app, Toast.LENGTH_SHORT).show()

                } catch (e: Exception) {

                    e.printStackTrace()

                    Log.d(TAG, "Something went wrong with factory reset.")
                }

                true

            }

            setFragmentResultListener("new_config") { _, _ ->

                setupScanConfigs()

            }

            findPreference<Preference>(mKeyUtil.innoCreateNew)?.let { pref ->

                pref.setOnPreferenceClickListener {

                    findNavController().navigate(InnoSpectraSettingsFragmentDirections
                        .actionToNewConfigCreator())

                    true
                }
            }

            setupScanConfigs()
        }
    }

    private fun setupScanConfigs() {

        val deviceViewModel = (activity as MainActivity).sDeviceViewModel as InnoSpectraViewModel

        val configList = findPreference<ListPreference>(mKeyUtil.innoActive)

        configList?.isEnabled = false

        scope.launch {

            var size = deviceViewModel.getScanConfigSize()
            while (size == -1
                || deviceViewModel.getScanConfigs().size != size
                || deviceViewModel.getActiveConfig() == null) {
                size = deviceViewModel.getScanConfigSize()
            }

            activity?.runOnUiThread {

                val configs = deviceViewModel.getScanConfigs()

                if (configs.isNotEmpty()) {

                    configList?.let { pref ->

                        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
                        var active = configs.indexOf(configs.find { it.scanConfigIndex == prefs.getInt(mKeyUtil.innoConfig, 0) })
                        if (active < 0) active = 0

                        pref.summary = configs[active].configName
                        pref.entries = configs.map { config -> config.configName }.toTypedArray()
                        pref.entryValues = configs.map { config -> config.scanConfigIndex.toString() }.toTypedArray()
                        pref.setValueIndex(active)

                        pref.setOnPreferenceChangeListener { _, newValue ->

                            val index = (newValue as String).toInt()

                            prefs.edit().putInt(mKeyUtil.innoConfig, index).apply()

                            pref.summary = configs.find { it.scanConfigIndex == index }?.configName

                            deviceViewModel.setActiveConfig(index)

                            true
                        }
                    }

                    configList?.isEnabled = true

                }
            }
        }
    }

    override fun onResume() {
        super.onResume()

        (activity as? MainActivity)?.setToolbar(R.id.action_nav_settings)

    }
}
