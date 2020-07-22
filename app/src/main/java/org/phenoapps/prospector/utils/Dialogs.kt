package org.phenoapps.prospector.utils

import android.app.Activity
import android.widget.SeekBar
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.addTextChangedListener
import androidx.databinding.DataBindingUtil
import org.phenoapps.prospector.R
import org.phenoapps.prospector.databinding.DialogLayoutCreateNameBinding

class Dialogs {

    companion object {

        fun askForName(activity: Activity, layoutId: Int, title: Int, button: Int, function: (String) -> Unit) {

            val binding = DataBindingUtil.inflate<DialogLayoutCreateNameBinding>(activity.layoutInflater, layoutId, null, false)

            binding.name = "shortcuts make long delays"

            binding.numBulbText.text = "${activity.getString(R.string.num_bulb)}: 0"

            binding.numLedText.text = "${activity.getString(R.string.num_led)}: 0"

            binding.editText.addTextChangedListener {

                if (!it.isNullOrBlank()) {

                    binding.name = it.toString()

                    binding.executePendingBindings()

                }

            }

            binding.numLedFrames.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {

                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {

                    val text = "${activity.getString(R.string.num_led)}: $progress"

                    binding.numLedText.text = text

                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {}

                override fun onStopTrackingTouch(seekBar: SeekBar?) {}

            })

            binding.numBulbFrames.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {

                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {

                    val text = "${activity.getString(R.string.num_bulb)}: $progress"

                    binding.numBulbText.text = text

                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {}

                override fun onStopTrackingTouch(seekBar: SeekBar?) {}

            })

            val builder = AlertDialog.Builder(activity).apply {

                setTitle(title)

                setView(binding.root)

                setPositiveButton(button) { dialog, it ->

                    function(binding.editText.text.toString())

                }

                create()

                show()
            }

        }

        /***
         * Generic dialog to run a function if the OK/Neutral button are pressed.
         * If the ok button is pressed the boolean parameter to the function is set to true, false otherwise.
         */
        fun booleanOption(builder: AlertDialog.Builder, title: String,
                          positiveText: String, negativeText: String,
                          neutralText: String, function: (Boolean) -> Unit) {

            builder.apply {

                setTitle(title)

                setPositiveButton(positiveText) { _, _ ->

                    function(true)

                }

                setNeutralButton(neutralText) { _, _ ->

                    function(false)

                }

                setNegativeButton(negativeText) { _, _ ->

                }

                show()
            }
        }

        /**
         * Simple alert dialog to notify the user of a message.
         */
        fun notify(builder: AlertDialog.Builder, title: String) {

            builder.apply {

                setPositiveButton("OK") { _, _ ->

                }
            }

            builder.setTitle(title)
            builder.show()
        }

        fun onOk(builder: AlertDialog.Builder, title: String, cancel: String, ok: String, function: () -> Unit) {

            builder.apply {

                setNegativeButton(cancel) { _, _ ->

                }

                setPositiveButton(ok) { _, _ ->

                    function()

                }

                setTitle(title)

                show()
            }
        }
    }
}