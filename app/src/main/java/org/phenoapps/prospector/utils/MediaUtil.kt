package org.phenoapps.prospector.utils

import android.content.Context
import android.media.MediaPlayer
import android.widget.Toast
import androidx.preference.PreferenceManager
import org.phenoapps.prospector.R

//wrapper for playing media that frees resources as well and checks preferences
class MediaUtil(private val ctx: Context?) {

    companion object Resources {
        const val BARCODE_SCAN = R.raw.notification_simple
        const val BARCODE_SEARCH_FAIL = R.raw.alert_error
        const val SCAN_SUCCESS = R.raw.hero_simple_celebration
        const val SCAN_TARGET = R.raw.hero_decorative_celebration
    }

    private val mPrefs by lazy {
        PreferenceManager.getDefaultSharedPreferences(ctx)
    }

    private val mKeyUtil by lazy {
        KeyUtil(ctx)
    }

    //plays a resource with the given id
    fun play(resId: Int) {

        if (mPrefs.getBoolean(mKeyUtil.audioEnabled, false)) {

            try {

                val player = MediaPlayer.create(ctx, resId)

                if (player != null) {

                    player.setOnCompletionListener {
                        player.stop()
                        player.release()
                    }

                    player.start()
                }
            } catch (e: Exception) {

                Toast.makeText(ctx, R.string.media_player_failed, Toast.LENGTH_SHORT).show()

                e.printStackTrace()
            }
        }
    }
}