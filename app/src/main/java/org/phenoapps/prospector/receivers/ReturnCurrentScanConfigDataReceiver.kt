package org.phenoapps.prospector.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.ISCSDK.ISCNIRScanSDK
import org.phenoapps.prospector.interfaces.NanoEventListener

class ReturnCurrentScanConfigDataReceiver() : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent) {

        Log.d("Nano", "Current scan config data received")

        //val config = intent.getByteArrayExtra(ISCNIRScanSDK.EXTRA_CURRENT_CONFIG_DATA)

        ISCNIRScanSDK.StartScan()
    }
}