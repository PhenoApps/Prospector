package org.phenoapps.prospector.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.ISCSDK.ISCNIRScanSDK
import org.phenoapps.prospector.interfaces.NanoEventListener

class SpectrumCalCoeffReadyReceiver() : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent) {

        Log.d("Nano", "Spectrum Calibration Coefficients ready")

        ISCNIRScanSDK.GetDeviceInfo()
    }
}