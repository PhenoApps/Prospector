package org.phenoapps.prospector.interfaces

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.LiveData
import androidx.preference.PreferenceManager
import com.stratiotechnology.linksquareapi.LSFrame

interface Spectrometer {

    data class Frame(var length: Int,
        var lightSource: Byte = 0,
        var frameNo: Int,
        var deviceType: Int,
        var data: FloatArray,
        var raw_data: FloatArray)

    data class DeviceInfo(val softwareVersion: String,
        val hardwareVersion: String,
        val deviceId: String,
        val alias: String,
        val opMode: String,
        val deviceType: String)

    fun manager(context: Context): SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

    suspend fun connect(context: Context)

    fun disconnect(context: Context): Int?

    fun isConnected(): Boolean

    fun reset(context: Context?) = Unit

    fun getDeviceError(): String?

    fun getDeviceInfo(): DeviceInfo

    fun setEventListener(onClick: () -> Unit): LiveData<String>

    fun scan(context: Context, manual: Boolean? = false): LiveData<List<LSFrame>?>
}