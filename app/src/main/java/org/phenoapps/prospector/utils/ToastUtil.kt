package org.phenoapps.prospector.utils

import android.content.Context
import android.view.Gravity
import android.widget.Toast

class ToastUtil {
    companion object {
        private fun showToast(context: Context, text: String, length: Int = Toast.LENGTH_SHORT,
                 gravity: Int = Gravity.CENTER, xOffset: Int = 0, yOffset: Int = 72) {
            val toast = Toast.makeText(context, text, length)
            toast.setGravity(gravity, xOffset, yOffset)
            toast.show()
        }

        fun show(context: Context?, id: Int, length: Int = Toast.LENGTH_SHORT,
                 gravity: Int = Gravity.CENTER, xOffset: Int = 0, yOffset: Int = 72) {
            context?.let { ctx ->
                showToast(ctx, ctx.getString(id), length, gravity, xOffset, yOffset)
            }
        }

        fun show(context: Context?, text: String, length: Int = Toast.LENGTH_SHORT,
                 gravity: Int = Gravity.CENTER, xOffset: Int = 0, yOffset: Int = 72) {
            context?.let { ctx ->
                showToast(ctx, text, length, gravity, xOffset, yOffset)
            }
        }
    }
}