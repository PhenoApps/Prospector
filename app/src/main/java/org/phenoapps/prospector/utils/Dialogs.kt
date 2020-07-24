package org.phenoapps.prospector.utils

import android.app.Activity
import android.widget.SeekBar
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.addTextChangedListener
import androidx.databinding.DataBindingUtil
import androidx.preference.PreferenceManager
import org.phenoapps.prospector.R
import org.phenoapps.prospector.databinding.DialogLayoutCreateNameBinding
import org.phenoapps.prospector.fragments.SettingsFragment
import java.util.*

class Dialogs {

    companion object {

        fun askForName(activity: Activity, title: Int, button: Int, function: (String) -> Unit) {

            val binding = DataBindingUtil.inflate<DialogLayoutCreateNameBinding>(activity.layoutInflater, R.layout.dialog_layout_create_name, null, false)

            val prefs = PreferenceManager.getDefaultSharedPreferences(activity)

            val uuid = prefs.getBoolean(SettingsFragment.AUTO_SCAN_NAME, false)

            binding.checkBox.setOnCheckedChangeListener { buttonView, isChecked ->

                if (isChecked) {

                    prefs.edit().putBoolean(SettingsFragment.AUTO_SCAN_NAME, true).apply()

                    val newUuid = UUID.randomUUID().toString()

                    binding.name = newUuid

                    binding.editText.setText(newUuid)

                } else {

                    prefs.edit().putBoolean(SettingsFragment.AUTO_SCAN_NAME, false).apply()

                    binding.name = "shortcuts make long delays"

                    binding.editText.setText("")

                }

            }

            binding.checkBox.isChecked = uuid

            binding.editText.addTextChangedListener {

                if (!it.isNullOrBlank()) {

                    binding.name = it.toString()

                    binding.executePendingBindings()

                }

            }

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