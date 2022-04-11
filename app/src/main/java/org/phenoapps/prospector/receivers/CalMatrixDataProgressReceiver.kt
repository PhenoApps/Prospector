package org.phenoapps.prospector.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import org.phenoapps.prospector.interfaces.NanoEventListener

class CalMatrixDataProgressReceiver() : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {

        Log.d("Nano", "Calibration Matrix Data Progress Received")

    }
}