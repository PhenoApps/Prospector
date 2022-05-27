package org.phenoapps.prospector.interfaces

import com.ISCSDK.ISCNIRScanSDK
import org.phenoapps.prospector.fragments.preferences.InnoSpectraSettingsFragment
import org.phenoapps.prospector.receivers.DeviceInfoReceiver
import org.phenoapps.viewmodels.spectrometers.Frame

interface NanoEventListener {

    fun onNotifyReceived() = Unit

    fun onRefDataReady() = Unit

    fun onScanDataReady(spectral: Frame) = Unit

    fun onScanStarted() = Unit

    fun onGetDeviceInfo(info: DeviceInfoReceiver.NanoDeviceInfo) = Unit

    fun onGetConfig(config: ISCNIRScanSDK.ScanConfiguration) = Unit

    fun onGetActiveConfig(index: Int) = Unit

    fun onConfigSaveStatus(status: Boolean) = Unit

    fun onGetConfigSize(size: Int) = Unit

    fun onGetUuid(uuid: String) = Unit

    fun onGetDeviceStatus(status: InnoSpectraSettingsFragment.DeviceStatus) = Unit
}