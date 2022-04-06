package org.phenoapps.prospector.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.ISCSDK.ISCNIRScanSDK
import org.phenoapps.prospector.interfaces.NanoEventListener

class GetActiveScanConfigReceiver(private val listener: NanoEventListener) : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {

        Log.d("Nano", "Active Scan Config Received")

        val config = intent.getByteArrayExtra(ISCNIRScanSDK.EXTRA_ACTIVE_CONF)

        config?.let { it ->

            if (it.isNotEmpty()) {

                listener.onGetActiveConfig(it[0].toInt())

            }
        }
    }
}