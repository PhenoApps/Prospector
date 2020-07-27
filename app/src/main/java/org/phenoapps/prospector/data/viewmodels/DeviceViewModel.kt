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
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext

/**
 * TODO: Implement an interface for spectrometers then generalize this to devices other than LS.
 */
class DeviceViewModel() : ViewModel() {

    init {

        System.loadLibrary("LinkSquareAPI")

    }

    private fun manager(context: Context) = PreferenceManager.getDefaultSharedPreferences(context)

    private val sDevice: LinkSquareAPI? by lazy {

        LinkSquareAPI.getInstance().also {

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

    fun getDeviceInfo() = sDevice?.GetDeviceInfo()

    fun disconnect() = sDevice?.Close()

    fun connection(context: Context) = liveData<Int?> {

        with (manager(context)) {

            val ip = getString(DEVICE_IP, "192.168.1.1") ?: "192.168.1.1"

            val port = (getString(DEVICE_PORT, "18630") ?: "18630").toInt()

            val result = connect(ip, port).await()

            emit(result)

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

    private suspend fun scan(ledFrames: Int, bulbFrames: Int) = withContext(coroutineContext) {

        val frames = ArrayList<LSFrame>()

        sDevice?.Scan(ledFrames, bulbFrames, frames)

        return@withContext frames

    }

    private suspend fun connect(ip: String, port: Int) = withContext(coroutineContext) {

        async {

            sDevice?.Connect(ip, port)

        }

    }

}