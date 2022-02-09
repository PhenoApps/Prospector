package org.phenoapps.prospector.data.viewmodels.devices

import DEVICE_TYPE_NANO
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import androidx.preference.PreferenceManager
import com.ISCSDK.ISCNIRScanSDK
import com.ISCSDK.ISCNIRScanSDK.*
import com.stratiotechnology.linksquareapi.LSFrame
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.phenoapps.prospector.R
import org.phenoapps.prospector.fragments.InnoSpectraNewConfigFragment
import org.phenoapps.prospector.fragments.InnoSpectraSettingsFragment
import org.phenoapps.prospector.interfaces.NanoEventListener
import org.phenoapps.prospector.interfaces.Spectrometer
import org.phenoapps.prospector.receivers.DeviceInfoReceiver
import org.phenoapps.prospector.utils.KeyUtil
import java.io.ByteArrayOutputStream
import java.io.UnsupportedEncodingException
import java.lang.IllegalArgumentException
import java.util.*
import javax.inject.Inject
import kotlin.collections.ArrayList

@HiltViewModel
class InnoSpectraViewModel @Inject constructor() : ViewModel(), Spectrometer, NanoEventListener {

    private var mBluetoothManager: BluetoothManager? = null

    private var mBluetoothDevice: BluetoothDevice? = null

    //service connection to the bluetooth Gatt service, built when connect is called with a context
    private var mServiceConnection: ServiceConnection? = null

    //nano sdk that is set when service is established
    private var mNanoSdk: NanoConnection? = null

    data class NanoConnection(val sdk: ISCNIRScanSDK, val device: NanoDevice)

    //track if device is currently connected
    private var mConnected = false

    //track if we have the ref data ready (need this before scanning)
    private var mRefDataReady = false

    //wrapper class for handling IS receivers
    private var mNanoReceiver: InnoSpectraBase? = null

    //live data that receivers will produce and listeners will consume
    private var mSpectralData: Spectrometer.Frame? = null

    private var OnDeviceButtonClicked: (() -> Unit)? = null

    private var mIsScanning = false

    private var mUiScan = false

    private var mDeviceInfo: Spectrometer.DeviceInfo? = null

    private var mDeviceStatus: InnoSpectraSettingsFragment.DeviceStatus? = null

    private var mConfigs = ArrayList<ScanConfiguration>()

    private var mConfigSize: Int? = null

    private var mActiveIndex: Int? = null

    private var mConfigSaved: Boolean? = null

    private fun buildServiceConnection(context: Context): ServiceConnection = object: ServiceConnection {

        override fun onServiceConnected(name: ComponentName, service: IBinder) {

            (service as? LocalBinder)?.service?.let { sdk ->

                sdk.initialize()

                mBluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
                val adapter = mBluetoothManager?.adapter
                val scanner = adapter?.bluetoothLeScanner
                scanner?.startScan(object: ScanCallback() {

                    override fun onScanResult(callbackType: Int, result: ScanResult?) {
                        super.onScanResult(callbackType, result)

                        result?.device?.let { device ->

                            val nanoName = getStringPref(context, SharedPreferencesKeys.DeviceFilter, "NIR")

                            device.name?.let { name ->

                                if (name.contains(nanoName)) {

                                    result.scanRecord?.let { record ->

                                        mBluetoothDevice = device

                                        val nanoDevice = NanoDevice(device, result.rssi, record.bytes)

                                        if (sdk.connect(nanoDevice.nanoMac)) {

                                            scanner.stopScan(this)

                                            mConnected = true

                                            mNanoSdk = NanoConnection(sdk, nanoDevice)

                                            val configIndex = PreferenceManager.getDefaultSharedPreferences(context)
                                                .getInt(KeyUtil(context).innoConfig, 0)

                                            setActiveConfig(configIndex)
                                        }
                                    }
                                }
                            }
                        }
                    }
                })
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            mConnected = false
        }
    }

    override suspend fun connect(context: Context) {

        buildServiceConnection(context).also { conn ->

            mServiceConnection = conn

            val gattService = Intent(context, ISCNIRScanSDK::class.java)

            context.bindService(gattService, conn, Context.BIND_AUTO_CREATE)

            mNanoReceiver = InnoSpectraBase(this).also {
                it.register(context)
            }
        }
    }

    override fun disconnect(context: Context): Int {

        mNanoSdk?.sdk?.disconnect()
        mNanoSdk?.sdk?.close()

        try {
            mServiceConnection?.let { conn ->

                context.unbindService(conn)

            }
        } catch (e: IllegalArgumentException) {
            e.printStackTrace()
        }

        mNanoReceiver?.unregister(context)

        refreshConfigs()
        mConnected = false
        mRefDataReady = false
        mBluetoothDevice = null

        return 1
    }

    override fun isConnected() = if (mBluetoothDevice != null) {

        mBluetoothManager?.getConnectionState(mBluetoothDevice,
            BluetoothProfile.GATT_SERVER) == BluetoothProfile.STATE_CONNECTED

    } else false

    private fun isRefDataReady() = mRefDataReady

    override fun reset(context: Context?) {

        context?.let { ctx ->

            disconnect(ctx)

            viewModelScope.launch {

                connect(ctx)

            }
        }
    }

    override fun getDeviceError(): String { return "None" }

    override fun getDeviceInfo() = mDeviceInfo ?: Spectrometer.DeviceInfo("?", "?", "?", "?", "?", DEVICE_TYPE_NANO)

    override fun setEventListener(onClick: () -> Unit): LiveData<String> = liveData {

        OnDeviceButtonClicked = onClick

        emit("DONE")

    }

    override fun scan(context: Context, manual: Boolean?): LiveData<List<LSFrame>?> = liveData {

        if (isConnected() && isRefDataReady()) {

            mUiScan = !(manual ?: false)

            //pressing Nano button automatically starts a scan, no need to call jni
            if (manual != true) {
                //jni call
                StartScan()
            }

            //wait for spectral data from the receiver
            while (mSpectralData == null) {

                delay(500)

            }

            mSpectralData?.let { frame ->

                mIsScanning = false

                ControlPhysicalButton(PhysicalButton.Unlock)

                //reset data to null for next scan
                mSpectralData = null

                mUiScan = false

                emit(listOf(frame).map {
                    LSFrame().apply {
                        this.lightSource = it.lightSource
                        this.length = it.length
                        this.frameNo = it.frameNo
                        this.deviceType = it.deviceType
                        this.raw_data = it.raw_data
                        this.data = it.data
                    }
                })
            }

        } else emit(null)
    }

    override fun onNotifyReceived() {

        if (isConnected()) {

           // ControlPhysicalButton(PhysicalButton.Lock)

            //jni call
            SetCurrentTime()

            GetDeviceInfo()

        }
    }

    override fun onRefDataReady() {

        if (isConnected()) {

            mRefDataReady = true

            //ControlPhysicalButton(PhysicalButton.Unlock)

            GetDeviceInfo()
        }
    }

    override fun onScanStarted() {

        if (isConnected() && !mIsScanning) {

            mIsScanning = true

            if (!mUiScan) {

                ControlPhysicalButton(PhysicalButton.Lock)

                OnDeviceButtonClicked?.invoke()

            }
        }
    }

    override fun onScanDataReady(spectral: Spectrometer.Frame) {

        if (isConnected()) {

            mSpectralData = spectral

        }
    }

    override fun onGetUuid(uuid: String) {

        mDeviceInfo?.let { info ->
//            mDeviceInfo = Spectrometer.DeviceInfo(
//                info.softwareVersion,
//                info.hardwareVersion,
//                uuid,
//                info.alias,
//                info.opMode,
//                info.deviceType
//            )
        }

        GetDeviceStatus()

    }

    override fun onGetDeviceInfo(info: DeviceInfoReceiver.NanoDeviceInfo) {

        mDeviceInfo = with(info) {
            Spectrometer.DeviceInfo(
                this.spec,
                this.hardware,
                this.model,
                "",
                "",
                DEVICE_TYPE_NANO
            )
        }

        GetUUID()
    }

    override fun onGetConfig(config: ScanConfiguration) {

        if (mConfigs.isEmpty() || !mConfigs.any { config.scanConfigIndex == it.scanConfigIndex }) {

            mConfigs.add(config)

            if (mConfigs.size == mConfigSize) {

                GetActiveConfig()
            }
        }
    }

    fun hasActiveScan() = mActiveIndex != null

    override fun onGetActiveConfig(index: Int) {

        mActiveIndex = index
    }

    override fun onGetDeviceStatus(status: InnoSpectraSettingsFragment.DeviceStatus) {

        mDeviceStatus = status

        GetScanConfig()
    }

    override fun onGetConfigSize(size: Int) {

        mConfigSize = size
    }

    override fun onConfigSaveStatus(status: Boolean) {

        mConfigSaved = status

        GetScanConfig()
        //requestStoredConfigurationList()
    }

    fun getConfigSaved() = mConfigSaved

    fun resetConfigSaved() {

        mConfigSaved = null

    }

    /**
     * Save the config byte array as a string in user preferences.
     */
    fun addConfig(config: InnoSpectraNewConfigFragment.Config) {

        val nameSize = config.name.length
        val bytes = config.name.toByteArray()
        for (i in 0..nameSize) {
            ScanConfigInfo.configName[i] = if (i == nameSize) 0
            else bytes[i]
        }
        ScanConfigInfo.write_scanType = 2
        val serialNum = "12345678"
        val serialBytes = serialNum.toByteArray()
        val serialNumSize = serialNum.length
        for (i in 0 until serialNumSize) {
            ScanConfigInfo.scanConfigSerialNumber[i] = serialBytes[i]
        }
        ScanConfigInfo.write_scanConfigIndex = 255
        ScanConfigInfo.write_numSections = config.sections.size.toByte()
        ScanConfigInfo.write_numRepeat = config.repeats

        config.sections.forEachIndexed { i, section ->
            ScanConfigInfo.sectionScanType[i] = section.type.toByte()
            ScanConfigInfo.sectionWavelengthStartNm[i] = section.start.toInt()
            ScanConfigInfo.sectionWavelengthEndNm[i] = section.end.toInt()
            ScanConfigInfo.sectionNumPatterns[i] = section.resolution
            ScanConfigInfo.sectionWidthPx[i] = section.width.toInt().toByte()
            ScanConfigInfo.sectionExposureTime[i] = section.exposure.toInt()
        }

        ScanConfig(WriteScanConfiguration(ScanConfigInfo()), ScanConfig.SAVE)
    }

    fun refreshConfigs() {

        mActiveIndex = null
        mConfigSize = null
        mConfigs.clear()

    }

    fun getScanConfigs(): List<ScanConfiguration> {

        return mConfigs.toList()
    }

    fun getActiveConfig(): Int? {

        return mActiveIndex
    }

    fun setActiveConfig(index: Int) {

        SetActiveConfig(byteArrayOf(index.toByte(), (index/256).toByte()))

    }

    fun getScanConfigSize(): Int {

        return mConfigSize ?: -1
    }

    fun getDeviceStatus(context: Context): String {

        val temperatureHeader = context.getString(R.string.header_temperature)
        val humidityHeader = context.getString(R.string.header_humidity)
        val totalLampHeader = context.getString(R.string.header_total_lamp_time)
        val batteryHeader = context.getString(R.string.header_battery)

        if (mDeviceStatus == null) return ""

        return """
            $temperatureHeader: ${mDeviceStatus?.temperature}
            $humidityHeader: ${mDeviceStatus?.humidity}
            $totalLampHeader: ${mDeviceStatus?.totalLampTime}
            $batteryHeader: ${mDeviceStatus?.battery}
        """.trimIndent()
    }

    fun getDeviceInfo(context: Context): String {

        val softwareHeader = context.getString(R.string.header_software_version)
        val hardwareHeader = context.getString(R.string.header_hardware_version)
        val deviceIdHeader = context.getString(R.string.header_device_id)

        if (mDeviceInfo == null) return ""

        return """
            $softwareHeader: ${mDeviceInfo?.softwareVersion}
            $hardwareHeader: ${mDeviceInfo?.hardwareVersion}
            $deviceIdHeader: ${mDeviceInfo?.deviceId}
        """.trimIndent()
    }

}