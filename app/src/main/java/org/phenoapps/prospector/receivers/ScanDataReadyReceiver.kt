package org.phenoapps.prospector.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.ISCSDK.ISCNIRScanSDK
import org.phenoapps.prospector.interfaces.NanoEventListener
import org.phenoapps.prospector.interfaces.Spectrometer
import kotlin.math.log10

class ScanDataReadyReceiver(private val listener: NanoEventListener) : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {

        Log.d("Nano", "Scan Data Ready")

        //reference_calibration = ISCNIRScanSDK.ReferenceCalibration.currentCalibration[0]

        val spectrumData = ISCNIRScanSDK.ScanResults(
            ISCNIRScanSDK.Interpret_wavelength,
            ISCNIRScanSDK.Interpret_intensity,
            ISCNIRScanSDK.Interpret_uncalibratedIntensity,
            ISCNIRScanSDK.Interpret_length
        )

        val size = spectrumData.length
        val reflectanceData = FloatArray(size)
        val intensityData = FloatArray(size)
        for (i in 0 until size) {

            //val x = ISCNIRScanSDK.ScanResults.getSpatialFreq(context, spectrumData.wavelength[i])
            val intensity = spectrumData.uncalibratedIntensity[i]
            //val absorbance = -1* log10(spectrumData.uncalibratedIntensity[i] / spectrumData.intensity[i].toDouble())
            val reflectance = spectrumData.uncalibratedIntensity[i] / spectrumData.intensity[i]
            //val wavelength = spectrumData.wavelength[i]
            //val reference = spectrumData.intensity[i]

            reflectanceData[i] = reflectance.toFloat()
            intensityData[i] = intensity.toFloat()
        }

        listener.onScanDataReady(Spectrometer.Frame(
            length = size,
            lightSource = 0,
            frameNo = 1,
            deviceType = 3,
            data = reflectanceData,
            raw_data = intensityData))
    }
}