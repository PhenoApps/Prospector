package org.phenoapps.prospector.utils

import android.content.Context
import com.stratiotechnology.linksquareapi.LinkSquareAPI
import org.phenoapps.prospector.R

fun buildLinkSquareDeviceInfo(context: Context, data: LinkSquareAPI.LSDeviceInfo?): String {

    data?.let { info ->

        val aliasHeader = context.getString(R.string.alias_header)
        val deviceIdHeader = context.getString(R.string.device_id_header)
        val deviceTypeHeader = context.getString(R.string.device_type_header)
        val hwVersion = context.getString(R.string.hw_version_header)
        val opMode = context.getString(R.string.op_mode_header)
        val swVersion = context.getString(R.string.sw_version_header)

        return """
            ${aliasHeader}: ${info.Alias}      
            ${deviceIdHeader}: ${info.DeviceID}          
            ${deviceTypeHeader}: ${info.DeviceType}       
            ${hwVersion}: ${info.HWVersion}        
            ${opMode}: ${info.OPMode}        
            ${swVersion}: ${info.SWVersion}
        """.trimIndent()

    }

    return "None"
}