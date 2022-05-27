package org.phenoapps.prospector.fragments.preferences.indigo

import android.annotation.SuppressLint
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.os.Build
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.lifecycle.liveData
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.delay
import org.phenoapps.adapters.bluetooth.BluetoothDeviceAdapter
import org.phenoapps.fragments.bluetooth.BluetoothListFragment
import org.phenoapps.models.bluetooth.BluetoothDeviceModel
import org.phenoapps.prospector.R

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
class DeviceSearchFragment: BluetoothListFragment() {

    companion object {
        const val LIVE_DATA_DELAY_MS = 2000L
    }

    private var mDeviceSet = hashSetOf<BluetoothDeviceModel>()

    private val mScanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            super.onScanResult(callbackType, result)

            result?.device?.let { d ->

                if (d.address !in mDeviceSet.map { it.device.address }) {

                    //Toast.makeText(context, "Found new device: ${d.address}.", Toast.LENGTH_SHORT).show()

                    mDeviceSet.add(BluetoothDeviceModel(d))
                }
            }
        }
    }

    override fun onRecyclerReady() {

        liveDeviceData().observe(viewLifecycleOwner) { devices ->

            (mRecyclerView.adapter as? BluetoothDeviceAdapter)
                ?.submitList(devices.toList())

        }
    }

    private fun liveDeviceData() = liveData<Set<BluetoothDeviceModel>> {

        while (true) {

            delay(LIVE_DATA_DELAY_MS)

            emit(mDeviceSet)

        }
    }

    @SuppressLint("MissingPermission") //handled with advisor
    override fun onPause() {
        super.onPause()

        advisor.withNearby { adapter ->

            adapter.cancelDiscovery()

            adapter.bluetoothLeScanner.stopScan(mScanCallback)

        }
    }

    @SuppressLint("MissingPermission") //handled with advisor
    override fun onResume() {

        advisor.withNearby { adapter ->

            adapter.startDiscovery()

            adapter.bluetoothLeScanner.startScan(mScanCallback)
        }

        super.onResume()
    }

    override fun onItemClicked(model: Any) {

        if (model is BluetoothDeviceModel) {

            val deviceSelected = context?.getString(R.string.pref_indigo_device_search_selected, model.device.address) ?: model.device.address

            Toast.makeText(context, deviceSelected, Toast.LENGTH_SHORT).show()

            mPrefs.edit().putString(mKeys.argBluetoothDeviceAddress, model.device.address).apply()

            findNavController().popBackStack()
        }
    }
}