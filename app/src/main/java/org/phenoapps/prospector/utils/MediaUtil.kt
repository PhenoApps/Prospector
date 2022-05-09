package org.phenoapps.prospector.utils

import android.app.Activity
import android.content.Context
import android.media.MediaPlayer
import android.widget.Toast
import androidx.preference.PreferenceManager
import org.phenoapps.prospector.R
import org.phenoapps.prospector.activities.MainActivity

//wrapper for playing media that frees resources as well and checks preferences
class MediaUtil(private val act: Activity?) {

    companion object Resources {
        const val BARCODE_SCAN = R.raw.notification_simple
        const val BARCODE_SEARCH_FAIL = R.raw.alert_error
        const val SCAN_SUCCESS = R.raw.hero_simple_celebration
        const val SCAN_TARGET = R.raw.hero_decorative_celebration
    }

    private val mPrefs by lazy {
        PreferenceManager.getDefaultSharedPreferences(act)
    }

    private val mKeyUtil by lazy {
        KeyUtil(act)
    }

    //plays a resource with the given id
    fun play(resId: Int) {

        if (mPrefs.getBoolean(mKeyUtil.audioEnabled, false)) {

            try {

                val player = MediaPlayer.create(act, resId)

                if (player != null) {

                    player.setOnCompletionListener {
                        player.stop()
                        player.release()
                    }

                    player.start()
                }
            } catch (e: Exception) {

                (act as? MainActivity)?.notify(R.string.media_player_failed)

                e.printStackTrace()
            }
        }
    }
}