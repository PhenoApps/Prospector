package org.phenoapps.prospector.data.viewmodels

import BULB_FRAMES
import DEVICE_IP
import DEVICE_PORT
import LED_FRAMES
import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import androidx.preference.PreferenceManager
import com.stratiotechnology.linksquareapi.LSFrame
import com.stratiotechnology.linksquareapi.LinkSquareAPI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/**
 * TODO: Implement an interface for spectrometers then generalize this to devices other than LS.
 */
class DeviceViewModel : ViewModel() {

    init {

        System.loadLibrary("LinkSquareAPI")

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

    fun reset(context: Context) {

        sDevice?.Close()

        sDevice = LinkSquareAPI.getInstance().also {

            it?.Initialize()

        }

        connection(context)
    }

    fun getDeviceError() = sDevice?.GetLSError()

    fun getDeviceInfo() = sDevice?.GetDeviceInfo()

    suspend fun disconnect() = sDevice?.Close()

    fun isConnected() = sDevice?.IsConnected() ?: false

    fun setEventListener(onClick: () -> Unit) = liveData(Dispatchers.IO, 2000L) {

        sDevice.let { device ->

            device?.SetEventListener(object : LinkSquareAPI.LinkSquareAPIListener {

                override fun LinkSquareEventCallback(eventType: LinkSquareAPI.EventType?, var2: Int) {
                    when (eventType) {

                        LinkSquareAPI.EventType.Button -> {

                            onClick()

                        }
                        else -> {

                            Log.d("ProspectorLSDevice", eventType?.name)

                        }
                    }
                }
            })
        }

        emit("DONE")
    }

    fun connection(context: Context) = liveData(Dispatchers.IO, 2000L) {

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

            delay(1000L)
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

    private suspend fun scan(ledFrames: Int, bulbFrames: Int) = withContext(Dispatchers.IO) {

        val frames = ArrayList<LSFrame>()

        sDevice?.Scan(ledFrames, bulbFrames, frames)

        return@withContext frames

    }

    private suspend fun connect(ip: String, port: Int) = withContext(viewModelScope.coroutineContext + Dispatchers.IO + Job()) {

        sDevice?.Connect(ip, port)

    }

}