package org.phenoapps.prospector.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import org.phenoapps.prospector.interfaces.NanoEventListener

class ReturnReadActivateStatusReceiver() : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent) {

        Log.d("Nano", "Return read activate status received")

    }
}