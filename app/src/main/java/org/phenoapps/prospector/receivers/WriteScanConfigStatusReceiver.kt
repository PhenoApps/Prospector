package org.phenoapps.prospector.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.ISCSDK.ISCNIRScanSDK
import org.phenoapps.prospector.R
import org.phenoapps.prospector.interfaces.NanoEventListener
import org.phenoapps.prospector.utils.ToastUtil

class WriteScanConfigStatusReceiver(private val listener: NanoEventListener) : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent) {

        intent.getByteArrayExtra(ISCNIRScanSDK.RETURN_WRITE_SCAN_CONFIG_STATUS)?.let { status ->
            if (status[0].toInt() == 1) {
                if (status[1].toInt() == 1) {
                    listener.onConfigSaveStatus(true)
                } else {
                    ToastUtil.show(context, R.string.inno_spectra_save_failed)
                    listener.onConfigSaveStatus(false)
                }
            } else if (status[0].toInt() == -1) {
                listener.onConfigSaveStatus(false)
                ToastUtil.show(context, R.string.inno_spectra_save_failed)
            } else if (status[0].toInt() == -2) {
                listener.onConfigSaveStatus(false)
                ToastUtil.show(context, R.string.inno_spectra_hardware_incompatible)
            } else if (status[0].toInt() == -3) {
                listener.onConfigSaveStatus(false)
                ToastUtil.show(context, R.string.inno_spectra_function_locked)
            }
        }
    }
}