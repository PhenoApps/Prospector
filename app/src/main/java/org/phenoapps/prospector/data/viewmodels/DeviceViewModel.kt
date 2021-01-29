package org.phenoapps.prospector.data.viewmodels

import BULB_FRAMES
import DEVICE_IP
import DEVICE_PORT
import LED_FRAMES
import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import androidx.preference.PreferenceManager
import com.stratiotechnology.linksquareapi.LSFrame
import com.stratiotechnology.linksquareapi.LinkSquareAPI
import kotlinx.coroutines.*
import java.io.IOException
import java.net.InetAddress
import java.net.UnknownHostException

/**
 * TODO: Implement an interface for spectrometers then generalize this to devices other than LS.
 */
class DeviceViewModel : ViewModel() {

    init {

        System.loadLibrary("LinkSquareAPI")

    }

    private val sDeviceScope = CoroutineScope(Dispatchers.IO)

    override fun onCleared() {

        sDevice?.Close()

        super.onCleared()
    }

    private fun manager(context: Context) = PreferenceManager.getDefaultSharedPreferences(context)

    private var sDevice: LinkSquareAPI? = LinkSquareAPI.getInstance().also {

        it?.Initialize()

    }

    suspend fun connect(context: Context) {

        with (manager(context)) {

            val ip = getString(DEVICE_IP, "192.168.1.1") ?: "192.168.1.1"

            val port = (getString(DEVICE_PORT, "18630") ?: "18630").toInt()

            connect(ip, port)

        }
    }

    fun close(context: Context) {

        sDevice?.Close()

    }

    fun reset(context: Context) {

        sDevice?.Close()

        sDevice = LinkSquareAPI.getInstance().also {

            it?.Initialize()

        }
    }

    fun getDeviceError() = sDevice?.GetLSError()

    fun getDeviceInfo() = sDevice?.GetDeviceInfo()

    suspend fun disconnect() = sDevice?.Close()

    fun isConnected() = sDevice?.IsConnected() ?: false

    fun setEventListener(onClick: () -> Unit) = liveData(sDeviceScope.coroutineContext, 2000L) {

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

    fun isConnectedLive() = liveData(sDeviceScope.coroutineContext) {

        var status = false

        while(true) {

            val nextStatus = sDevice?.IsConnected() ?: false

            if (status != nextStatus) {

                status = nextStatus

                emit(status)

                delay(2000)

            }
        }
    }


    fun connection(context: Context) = liveData(sDeviceScope.coroutineContext, 500L) {

        val connecting = -1

        var result: Int? = LinkSquareAPI.RET_ERR

        //emit(connecting)

        while (result == LinkSquareAPI.RET_ERR) {

            with (manager(context)) {

                val ip = getString(DEVICE_IP, "192.168.1.1") ?: "192.168.1.1"

                val port = (getString(DEVICE_PORT, "18630") ?: "18630").toInt()

                result = connect(ip, port)

                emit(when (result) {

                    LinkSquareAPI.RET_OK -> {

                        getDeviceInfo()

                    }
                    else -> {

                        getDeviceError()

                    }
                })
            }
        }
    }

    fun scan(context: Context) = liveData<List<LSFrame>> {

        with (manager(context)) {

            val ledFrames = getInt(LED_FRAMES, 1)

            val bulbFrames = getInt(BULB_FRAMES, 1)

            val frames = scan(ledFrames, bulbFrames)

            emit(frames)

        }

    }

    private suspend fun scan(ledFrames: Int, bulbFrames: Int) = withContext(sDeviceScope.coroutineContext) {

        val frames = ArrayList<LSFrame>()

        sDevice?.Scan(ledFrames, bulbFrames, frames)

        return@withContext frames

    }

    suspend fun connect(ip: String, port: Int) = withContext(sDeviceScope.coroutineContext) {

        sDevice?.Connect(ip, port)

    }

    //adapted from https://stackoverflow.com/questions/13198669/any-way-to-discover-android-devices-on-your-network
    /**
     * Searches through all ip-address on the subnet 192.168.0 and attempts to connect.
     */
    fun scanSubNet(context: Context, subnet: String) = liveData(sDeviceScope.coroutineContext) {

        for (i in 2..255) {

            try {

                if (InetAddress.getByName("$subnet.$i").isReachable(250)) {

                    if (sDevice?.Connect("$subnet.$i", 18630) == 1) {

                        emit("$subnet.$i")

                        break

                    }
                }

            } catch (e: UnknownHostException) {

                e.printStackTrace()

            } catch (e: IOException) {

                e.printStackTrace()
            }
        }
    }

    //0: Open, 1: WEP, 2:WPA/WPA2
    suspend fun setWLanInfo(ssid: String, pass: String, config: Int): Deferred<Boolean> = withContext(sDeviceScope.coroutineContext) {

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
        val OPEN = 0
        val WEP = 1
        val WPA = 2

    }
}