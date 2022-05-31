package org.phenoapps.prospector.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.ISCSDK.ISCNIRScanSDK
import com.stratiotechnology.linksquareapi.LSFrame
import org.phenoapps.interfaces.spectrometers.Spectrometer.Companion.DEVICE_TYPE_NANO
import org.phenoapps.prospector.interfaces.NanoEventListener
import org.phenoapps.viewmodels.spectrometers.Frame

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

            val x = ISCNIRScanSDK.ScanResults.getSpatialFreq(context, spectrumData.wavelength[i])
            val intensity = spectrumData.uncalibratedIntensity[i]
            //val absorbance = -1* log10(spectrumData.uncalibratedIntensity[i] / spectrumData.intensity[i].toDouble())
            val reflectance = spectrumData.uncalibratedIntensity[i] / spectrumData.intensity[i]
            //val wavelength = spectrumData.wavelength[i]
            //val reference = spectrumData.intensity[i]

            reflectanceData[i] = x.toFloat()//reflectance.toFloat()
            intensityData[i] = intensity.toFloat()
        }

        //TODO replace device type with string
        listener.onScanDataReady(Frame(
            length = size,
            lightSource = 0,
            frameIndex = 1,
            deviceType = DEVICE_TYPE_NANO,
            data = reflectanceData,
            rawData = intensityData.joinToString(" ") { "$it" })
        )
    }
}