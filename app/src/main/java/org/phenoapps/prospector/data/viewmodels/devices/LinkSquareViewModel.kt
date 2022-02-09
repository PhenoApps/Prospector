package org.phenoapps.prospector.data.viewmodels.devices

import BULB_FRAMES
import DEVICE_IP
import DEVICE_PORT
import DEVICE_TYPE_LS1
import DEVICE_TYPE_NIR
import LED_FRAMES
import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import com.stratiotechnology.linksquareapi.LSFrame
import com.stratiotechnology.linksquareapi.LinkSquareAPI
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import org.phenoapps.prospector.interfaces.Spectrometer
import java.io.BufferedReader
import java.io.FileReader
import java.lang.IndexOutOfBoundsException
import javax.inject.Inject

@HiltViewModel
class LinkSquareViewModel @Inject constructor() : ViewModel(), Spectrometer {

    init {

        System.loadLibrary("LinkSquareAPI")

    }

    private val sDeviceScope = CoroutineScope(Dispatchers.IO)
    private val sDeviceConnectScope = CoroutineScope(Dispatchers.IO)

    override fun onCleared() {

        sDevice?.Close()

        sDeviceScope.cancel()

        super.onCleared()
    }

    private var sDevice: LinkSquareAPI? = LinkSquareAPI.getInstance().also {

        it?.Initialize()

    }

    override suspend fun connect(context: Context) {

        with (manager(context)) {

            val ip = getString(DEVICE_IP, "192.168.1.1") ?: "192.168.1.1"

            val port = (getString(DEVICE_PORT, "18630") ?: "18630").toInt()

            connect(ip, port)

        }
    }

    override fun reset(context: Context?) {

        sDevice?.Close()

        sDevice = LinkSquareAPI.getInstance().also {

            it?.Initialize()

        }
    }

    override fun getDeviceError() = sDevice?.GetLSError()
    override fun getDeviceInfo() = with(sDevice?.GetDeviceInfo()) {
        Spectrometer.DeviceInfo(
            this?.SWVersion ?: "?",
            this?.HWVersion ?: "?",
            this?.DeviceID ?: "-1",
            this?.Alias ?: "?",
            this?.OPMode ?: "?",
            when (this?.DeviceType ?: 0) {
                0 -> DEVICE_TYPE_LS1
                else -> DEVICE_TYPE_NIR
            }
        )
    }

    override fun disconnect(context: Context): Int? = sDevice?.Close()

    override fun isConnected() = sDevice?.IsConnected() ?: false

    override fun setEventListener(onClick: () -> Unit) = liveData(sDeviceScope.coroutineContext, 2000L) {

        sDevice.let { device ->

            device?.SetEventListener { eventType, _ ->

                when (eventType) {

                    LinkSquareAPI.EventType.Button -> {

                        onClick()

                    }
                    else -> {

                        Log.d("ProspectorLSDevice", eventType?.name ?: "?")

                    }
                }
            }
        }

        emit("DONE")
    }

    override fun scan(context: Context, manual: Boolean?): LiveData<List<LSFrame>?> = liveData {

        with (manager(context)) {

            val ledFrames = getInt(LED_FRAMES, 1)

            val bulbFrames = getInt(BULB_FRAMES, 1)

            val frames = scan(ledFrames, bulbFrames)

            emit(frames)
        }
    }

    private val mFrames = ArrayList<LSFrame>()
    private suspend fun scan(ledFrames: Int, bulbFrames: Int) = withContext<List<LSFrame>>(sDeviceScope.coroutineContext) {

        sDevice?.Scan(ledFrames, bulbFrames, mFrames)

        return@withContext mFrames

    }

    private suspend fun connect(ip: String, port: Int) = withContext(sDeviceConnectScope.coroutineContext) {

        sDevice?.Connect(ip, port)

    }

    //adapted from https://stackoverflow.com/questions/13198669/any-way-to-discover-android-devices-on-your-network
    /**
     * Searches through all ip-address on the subnet 192.168.0 and attempts to connect.
     */
    @Suppress("BlockingMethodInNonBlockingContext")
    fun scanSubNet() = liveData(sDeviceScope.coroutineContext) {

        for (i in 0..255) {

            for (j in 2..255) {

                val address = "192.168.$i.$j"

                if (sDevice?.Connect(address, 18630) == 1) {

                    emit(address)

                    break

                }
            }
        }
    }

    fun scanArp() = liveData(sDeviceScope.coroutineContext) {

        var reader: BufferedReader? = null

        try {

            reader = BufferedReader(FileReader("/proc/net/arp"))
            var line: String? = reader.readLine()
            while(line != null) {

                line = reader.readLine()

                if (line != null) {

                    val tokens = line.split(" ").filter { it.isNotBlank() }

                    if (isTexasInstrument(tokens[3])) {
                        emit(tokens[0])
                        break
                    }
                }
            }

        } catch (e: Exception) {

            e.printStackTrace()

        } finally {

            reader?.close()
        }

        emit("fail")

    }

    //0: Open, 1: WEP, 2:WPA/WPA2
    suspend fun setWLanInfoAsync(ssid: String, pass: String, config: Int): Deferred<Boolean> = withContext(sDeviceScope.coroutineContext) {

        async {

            try {

                sDevice?.SetWLanInfo(ssid, pass, config.toByte())

                true

            } catch (e: Exception) {

                e.printStackTrace()

                false

            }
        }
    }

    suspend fun setAlias(alias: String) = withContext(sDeviceScope.coroutineContext) {

        sDevice?.SetAlias(alias)
    }

    companion object {

        //protocol types
        const val OPEN = 0
        const val WEP = 1
        const val WPA = 2

        /**
         * LinkSquare devices use a texas instrument wifi chip.
         * Instead of brute force searching the network (which is still a fallback)
         * Use arp to search for all connected devices to the network, and see if
         * any of their mac addresses are registered with texas instrument.
         * This function checks the parameter address with the registered prefixes.
         */
        fun isTexasInstrument(address: String): Boolean {

            //get the prefix of the address parameter
            val prefix = try {
                address.substring(0, 8).uppercase()
            } catch (e: IndexOutOfBoundsException) {
                return false
            }

            //check all registered TI prefixes
            return prefix in setOf(
                "00:12:37", "00:12:4B", "00:12:D1", "00:12:D2",
                "00:17:83", "00:17:E3", "00:17:E4", "00:17:E5",
                "00:17:E6", "00:17:E7", "00:17:E8", "00:17:E9",
                "00:17:EA", "00:17:EB", "00:17:EC", "00:18:2F",
                "00:18:30", "00:18:31", "00:18:32", "00:18:33",
                "00:18:34", "00:1A:B6", "00:21:BA", "00:22:A5",
                "00:23:D4", "00:24:BA", "00:35:FF", "00:81:F9",
                "04:79:B7", "04:A3:16", "04:E4:51", "04:EE:03",
                "08:00:28", "0C:1C:57", "0C:61:CF", "0C:AE:7D",
                "0C:B2:B7", "0C:EC:80", "10:08:2C", "10:2E:AF",
                "10:CE:A9", "14:42:FC", "18:04:ED", "18:45:16",
                "18:62:E4", "18:93:D7", "1C:45:93", "1C:BA:8C",
                "1C:DF:52", "20:91:48", "20:C3:8F", "20:CD:39",
                "20:D7:78", "24:71:89", "24:76:25", "24:7D:4D",
                "24:9F:89", "28:EC:9A", "2C:6B:7D", "2C:AB:33",
                "30:45:11", "30:E2:83", "34:03:DE", "34:08:E1",
                "34:14:B5", "34:15:13", "34:2A:F1", "34:84:E4",
                "34:B1:F7", "38:0B:3C", "38:81:D7", "38:D2:69",
                "3C:2D:B7", "3C:7D:B1", "3C:A3:08", "3C:E4:B0",
                "40:06:A0", "40:2E:71", "40:5F:C2", "40:98:4E",
                "40:BD:32", "44:C1:5C", "44:EA:D8", "48:70:1E",
                "4C:24:98", "4C:3F:D3", "50:33:8B", "50:51:A9",
                "50:56:63", "50:65:83", "50:72:24", "50:8C:B1",
                "50:F1:4A", "54:4A:16", "54:6C:0E", "54:7D:CD",
                "58:7A:62", "58:93:D8", "5C:31:3E", "1C:E2:CC",
                "5C:6B:32", "5C:F8:21", "60:64:05", "60:77:71",
                "60:98:66", "60:B6:E1", "60:E8:5B", "64:33:DB",
                "64:69:4E", "64:7B:D4", "64:9C:8E", "64:CF:D9",
                "68:47:49", "68:9E:19", "68:C9:0B", "6C:79:B8",
                "6C:C3:74", "6C:EC:EB", "70:86:C1", "70:B9:50",
                "70:E5:6E", "70:FF:76", "74:D2:85", "74:D6:EA",
                "74:DA:EA", "74:E1:82", "78:04:73", "78:A5:04",
                "78:C5:E5", "78:DB:2F", "78:DE:E4", "7C:01:0A",
                "7C:38:66", "7C:66:9D", "7C:8E:E4", "7C:EC:79",
                "80:30:DC", "80:6F:B0", "80:F5:B5", "84:7E:40",
                "84:DD:20", "84:EB:18", "88:33:14", "88:3F:4A",
                "88:4A:EA", "88:C2:55", "8C:8B:83", "90:59:AF",
                "90:70:65", "90:9A:77", "90:D7:EB", "90:E2:02",
                "94:88:54", "94:A9:A8", "94:E3:6D", "98:07:2D",
                "98:59:45", "98:5D:AD", "98:7B:F3", "98:84:E3",
                "98:F0:7B", "9C:1D:58", "A0:6C:65", "A0:E6:F8",
                "A0:F6:FD", "A4:06:E9", "A4:34:F1", "A4:D5:78",
                "A4:DA:32", "A8:10:87", "A8:1B:6A", "A8:63:F2",
                "A8:E2:C1", "A8:E7:7D", "AC:1F:0F", "AC:4D:16",
                "B0:10:A0", "B0:7E:11", "B0:91:22", "B0:B1:13",
                "B0:B4:48", "B0:D5:CC", "B4:10:7B", "B4:52:A9",
                "B4:99:4C", "B4:BC:7C", "B4:EE:D4", "B8:80:4F",
                "B8:FF:FE", "BC:0D:A5", "BC:6A:29", "C0:E4:22",
                "C4:64:E3", "C4:BE:84", "C4:ED:BA", "C4:F3:12",
                "C8:3E:99", "C8:A0:30", "C8:DF:84", "C8:FD:19",
                "CC:33:31", "CC:78:AB", "CC:8C:E3", "D0:03:EB",
                "D0:07:90", "D0:2E:AB", "D0:37:61", "D0:39:72",
                "D0:5F:B8", "D0:8C:B5", "D0:B5:C2", "D0:FF:50",
                "D4:36:39", "D4:94:A1", "D4:F5:13", "D8:54:3A",
                "D8:71:4D", "D8:95:2F", "D8:A9:8B", "D8:DD:FD",
                "E0:62:34", "E0:7D:EA", "E0:C7:9D", "E0:D7:BA",
                "E0:E5:CF", "E4:15:F6", "E4:E1:12", "E8:EB:11",
                "EC:11:27", "EC:24:B8", "F0:45:DA", "F0:5E:CD",
                "F0:B5:D1", "F0:C7:7F", "F0:F8:F2", "F4:5E:AB",
                "F4:60:77", "F4:84:4C", "F4:B8:5E", "F4:E1:1E",
                "F4:FC:32", "F8:30:02", "F8:33:31", "F8:36:9B",
                "F8:8A:5E", "FC:0F:4B", "FC:45:C3", "FC:69:47", "FC:A8:9B"
            )
        }
    }
}