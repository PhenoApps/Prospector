package org.phenoapps.prospector.data.viewmodels.devices

import android.content.*
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.ISCSDK.ISCNIRScanSDK
import org.phenoapps.prospector.interfaces.NanoEventListener
import org.phenoapps.prospector.receivers.*

class InnoSpectraBase(listener: NanoEventListener) {

    private val receiverDataReady = ScanDataReadyReceiver(listener)
    private val receiverScanStarted = ScanStartedReceiver(listener)
    private val receiverDeviceInfo = DeviceInfoReceiver(listener)
    private val receiverUuid = GetUuidReceiver(listener)
    private val receiverActiveScanConfig = GetActiveScanConfigReceiver(listener)
    private val receiverGetDeviceStatus = GetDeviceStatusReceiver(listener)
    private val receiverRefCoeffDataProgress = RefCoeffDataProgressReceiver()
    private val receiverCalMatrixDataProgress = CalMatrixDataProgressReceiver()
    private val receiverRefDataReady = RefDataReadyReceiver(listener)
    private val receiverNotifyComplete = NotifyCompleteReceiver(listener)
    private val receiverScanConf = ScanConfigReceiver(listener)
    private val receiverSpecCoeff = SpectrumCalCoeffReadyReceiver()
    private val receiverReturnReadActivateStatus = ReturnReadActivateStatusReceiver()
    private val receiverReturnCurrentScanConfig = ReturnCurrentScanConfigDataReceiver()
    private val receiverScanConfSize = ScanConfigSizeReceiver(listener)
    private val receiverReturnActivateStatus = ReturnActivateStatusReceiver()
    private val receiverWriteConfig = WriteScanConfigStatusReceiver(listener)

    fun register(context: Context) {

        with (LocalBroadcastManager.getInstance(context)) {
            registerReceiver(receiverWriteConfig, IntentFilter(ISCNIRScanSDK.ACTION_RETURN_WRITE_SCAN_CONFIG_STATUS))
            registerReceiver(receiverUuid, IntentFilter(ISCNIRScanSDK.SEND_DEVICE_UUID))
            registerReceiver(receiverDataReady, IntentFilter(ISCNIRScanSDK.SCAN_DATA))
            registerReceiver(receiverScanStarted, IntentFilter(ISCNIRScanSDK.ACTION_SCAN_STARTED))
            registerReceiver(receiverActiveScanConfig, IntentFilter(ISCNIRScanSDK.SEND_ACTIVE_CONF))
            registerReceiver(receiverDeviceInfo,  IntentFilter(ISCNIRScanSDK.ACTION_INFO))
            registerReceiver(receiverGetDeviceStatus, IntentFilter(ISCNIRScanSDK.ACTION_STATUS))
            registerReceiver(receiverRefCoeffDataProgress, IntentFilter(ISCNIRScanSDK.ACTION_REQ_CAL_COEFF))
            registerReceiver(receiverCalMatrixDataProgress, IntentFilter(ISCNIRScanSDK.ACTION_REQ_CAL_MATRIX))
            registerReceiver(receiverRefDataReady, IntentFilter(ISCNIRScanSDK.REF_CONF_DATA))
            registerReceiver(receiverNotifyComplete, IntentFilter(ISCNIRScanSDK.ACTION_NOTIFY_DONE))
            registerReceiver(receiverSpecCoeff, IntentFilter(ISCNIRScanSDK.SPEC_CONF_DATA))
            registerReceiver(receiverReturnReadActivateStatus, IntentFilter(ISCNIRScanSDK.ACTION_RETURN_READ_ACTIVATE_STATE))
            registerReceiver(receiverReturnCurrentScanConfig, IntentFilter(ISCNIRScanSDK.RETURN_CURRENT_CONFIG_DATA))
            registerReceiver(receiverScanConf, IntentFilter(ISCNIRScanSDK.SCAN_CONF_DATA))
            registerReceiver(receiverScanConfSize, IntentFilter(ISCNIRScanSDK.SCAN_CONF_SIZE))
            registerReceiver(receiverReturnActivateStatus, IntentFilter(ISCNIRScanSDK.ACTION_RETURN_ACTIVATE))
        }
    }

    fun unregister(context: Context) {

        with(LocalBroadcastManager.getInstance(context)) {
            unregisterReceiver(receiverWriteConfig)
            unregisterReceiver(receiverUuid)
            unregisterReceiver(receiverDataReady)
            unregisterReceiver(receiverScanStarted)
            unregisterReceiver(receiverActiveScanConfig)
            unregisterReceiver(receiverDeviceInfo)
            unregisterReceiver(receiverGetDeviceStatus)
            unregisterReceiver(receiverRefCoeffDataProgress)
            unregisterReceiver(receiverCalMatrixDataProgress)
            unregisterReceiver(receiverRefDataReady)
            unregisterReceiver(receiverNotifyComplete)
            unregisterReceiver(receiverScanConf)
            unregisterReceiver(receiverScanConfSize)
            unregisterReceiver(receiverSpecCoeff)
            unregisterReceiver(receiverReturnReadActivateStatus)
            unregisterReceiver(receiverReturnCurrentScanConfig)
            unregisterReceiver(receiverReturnActivateStatus)
        }
    }
}