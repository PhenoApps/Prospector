package org.phenoapps.prospector.data.viewmodels

import BULB_FRAMES
import DEVICE_IP
import DEVICE_PORT
import LED_FRAMES
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import androidx.preference.PreferenceManager
import com.stratiotechnology.linksquareapi.LSFrame
import com.stratiotechnology.linksquareapi.LinkSquareAPI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * TODO: Implement an interface for spectrometers then generalize this to devices other than LS.
 */
class DeviceViewModel : ViewModel() {

    init {

        System.loadLibrary("LinkSquareAPI")

    }

    private fun manager(context: Context) = PreferenceManager.getDefaultSharedPreferences(context)

    private var sDevice: LinkSquareAPI = LinkSquareAPI.getInstance().also {

        it.Initialize()

    }

    fun reset() {

        sDevice.Close()

        sDevice = LinkSquareAPI.getInstance().also {

            it.Initialize()

        }
    }

    fun onClick(context: Context, onClick: () -> Unit) {

        with (manager(context)) {

            sDevice?.let { device ->

                device.SetEventListener { eventType, i ->

                    when (eventType) {

                        LinkSquareAPI.EventType.Button -> {

                            onClick()

                        }
                    }
                }
            }

        }

    }

    fun getDeviceError() = sDevice?.GetLSError()

    fun getDeviceInfo() = sDevice?.GetDeviceInfo()

    fun disconnect() = sDevice?.Close()

    fun isConnected() = sDevice?.IsConnected() ?: false

    fun connection(context: Context) = liveData {

        val connecting = -1

        with (manager(context)) {

            val ip = getString(DEVICE_IP, "192.168.1.1") ?: "192.168.1.1"

            val port = (getString(DEVICE_PORT, "18630") ?: "18630").toInt()

            emit(connecting)

            val result = connect(ip, port)

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

    fun scan(context: Context) = liveData<List<LSFrame>> {

        with (manager(context)) {

            val ledFrames = getInt(LED_FRAMES, 8)

            val bulbFrames = getInt(BULB_FRAMES, 8)

            val frames = scan(ledFrames, bulbFrames)

            emit(frames)

        }

    }

    private suspend fun scan(ledFrames: Int, bulbFrames: Int) = withContext(Dispatchers.IO) {

        val frames = ArrayList<LSFrame>()

        sDevice?.Scan(ledFrames, bulbFrames, frames)

        return@withContext frames

    }

    private suspend fun connect(ip: String, port: Int) = withContext(Dispatchers.IO) {

        return@withContext sDevice?.Connect(ip, port)

    }

}