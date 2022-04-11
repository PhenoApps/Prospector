package org.phenoapps.prospector.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.ISCSDK.ISCNIRScanSDK
import org.phenoapps.prospector.interfaces.NanoEventListener

/**
 * After download reference calibration  matrix will notify and save(ISCNIRScanSDK.SetCurrentTime()must be called)
 */
class RefDataReadyReceiver(private val listener: NanoEventListener) : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {

        val refCoeff = intent.getByteArrayExtra(ISCNIRScanSDK.EXTRA_REF_COEF_DATA)
        val refMatrix = intent.getByteArrayExtra(ISCNIRScanSDK.EXTRA_REF_MATRIX_DATA)

        val refCal: ArrayList<ISCNIRScanSDK.ReferenceCalibration> = ArrayList()
        refCal.add(ISCNIRScanSDK.ReferenceCalibration(refCoeff, refMatrix))
        ISCNIRScanSDK.ReferenceCalibration.writeRefCalFile(context, refCal)

        Log.d("Nano", "Reference Data Ready")

        listener.onRefDataReady()
    }
}