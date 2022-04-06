package org.phenoapps.prospector.fragments

import android.os.Handler
import android.os.HandlerThread
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import org.phenoapps.prospector.R
import org.phenoapps.prospector.activities.MainActivity

open class ConnectionFragment(id: Int) : Fragment(id) {

    private val mConnectionHandlerThread = HandlerThread("connection check")

    private fun startTimer() {
        Handler(mConnectionHandlerThread.looper).postDelayed({
            val deviceViewModel = (activity as MainActivity).sDeviceViewModel
            activity?.runOnUiThread {

                if (isAdded) {
                    with(view?.findViewById<Toolbar>(R.id.toolbar)) {

                        this?.menu?.findItem(R.id.action_connection)
                            ?.setIcon(
                                if (deviceViewModel?.isConnected() == true) R.drawable.ic_vector_link
                                else R.drawable.ic_vector_difference_ab
                            )

                    }
                }
            }

            startTimer()

        }, 1500)
    }

    private fun initiate() {

        if (!mConnectionHandlerThread.isAlive) {
            mConnectionHandlerThread.start()
            mConnectionHandlerThread.looper
        }

        startTimer()
    }

    override fun onResume() {
        super.onResume()

        initiate()
    }

    override fun onDestroy() {
        super.onDestroy()

        mConnectionHandlerThread.quit()

    }
}