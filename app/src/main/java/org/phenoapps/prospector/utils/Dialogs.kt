package org.phenoapps.prospector.utils

import android.app.Activity
import android.view.View
import android.widget.ArrayAdapter
import androidx.appcompat.app.AlertDialog
import androidx.databinding.DataBindingUtil
import org.phenoapps.prospector.R
import org.phenoapps.prospector.databinding.DialogLayoutCreateScanBinding

class Dialogs {

    companion object {


        fun askForScan(activity: Activity, title: Int, button: Int, cancel: Int, function: () -> Unit): AlertDialog.Builder {

            val binding = DataBindingUtil.inflate<DialogLayoutCreateScanBinding>(activity.layoutInflater, R.layout.dialog_layout_create_scan, null, false)

            return AlertDialog.Builder(activity).apply {

                setTitle(title)

                setView(binding.root)

                setCancelable(false)

                setNegativeButton(cancel) { dialog, it ->

                    dialog.dismiss()

                }

                binding.progressView.isIndeterminate = true

                binding.progressView.visibility = View.VISIBLE
            }

        }

        fun showColorChooserDialog(adapter: ArrayAdapter<String>,
                               builder: AlertDialog.Builder,
                               title: String,
                               onSuccess: (String) -> Unit) {

            builder.setTitle(title)

            builder.setSingleChoiceItems(adapter, 0) { dialog, item ->

                onSuccess(adapter.getItem(item) ?: "")

                dialog.dismiss()

            }

            builder.create()

            builder.show()

        }

        /***
         * Generic dialog to run a function if the OK/Neutral button are pressed.
         * If the ok button is pressed the boolean parameter to the function is set to true, false otherwise.
         */
        fun booleanOption(builder: AlertDialog.Builder, title: String,
                          message: String,
                          positiveText: String, negativeText: String,
                          function: (Boolean) -> Unit) {

            builder.apply {

                setTitle(title)

                setMessage(message)

                setPositiveButton(positiveText) { _, _ ->

                    function(true)

                }

                setNegativeButton(negativeText) { _, _ ->

                    function(false)
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

        /**
         * Simple alert dialog to notify the user of a message.
         */
        fun largeNotify(builder: AlertDialog.Builder, title: String) {

            builder.apply {

                setPositiveButton("OK") { _, _ ->

                }
            }

            builder.setMessage(title)
            builder.show()
        }

        fun onOk(builder: AlertDialog.Builder, title: String, cancel: String, ok: String, function: (Boolean) -> Unit) {

            builder.apply {

                setCancelable(false)

                setNegativeButton(cancel) { _, _ ->

                    function(false)

                }

                setPositiveButton(ok) { _, _ ->

                    function(true)

                }

                setTitle(title)

                create()

                show()
            }
        }

        fun askForExportType(builder: AlertDialog.Builder, title: String, options: Array<String>, function: (String) -> Unit) {

            builder.setTitle(title)

            builder.setSingleChoiceItems(options, 0) { dialog, choice ->

                function(options[choice])

                dialog.dismiss()

            }

            builder.create()

            builder.show()
        }
    }
}