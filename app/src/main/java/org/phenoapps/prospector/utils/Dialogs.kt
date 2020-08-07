package org.phenoapps.prospector.utils

import AUTO_SCAN_NAME
import android.app.Activity
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.databinding.DataBindingUtil
import androidx.preference.PreferenceManager
import com.google.zxing.ResultPoint
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeResult
import com.journeyapps.barcodescanner.DecoratedBarcodeView
import org.phenoapps.prospector.R
import org.phenoapps.prospector.databinding.DialogLayoutCreateNameBinding
import org.phenoapps.prospector.databinding.DialogLayoutCreateScanBinding

class Dialogs {

    companion object {

        private fun startBarcode(view: DecoratedBarcodeView, callback: () -> BarcodeCallback) {

            view.barcodeView.apply {

                cameraSettings.isContinuousFocusEnabled = true

                cameraSettings.isAutoTorchEnabled = true

                cameraSettings.isAutoFocusEnabled = true

                cameraSettings.isBarcodeSceneModeEnabled = true

                this.resume()

                decodeSingle(callback())

            }

        }

        fun askForScan(activity: Activity, title: Int, button: Int, cancel: Int, function: () -> Unit): AlertDialog.Builder {

            val binding = DataBindingUtil.inflate<DialogLayoutCreateScanBinding>(activity.layoutInflater, R.layout.dialog_layout_create_scan, null, false)

            return AlertDialog.Builder(activity).apply {

                setTitle(title)

                setView(binding.root)

                setCancelable(false)

                setNegativeButton(cancel) { dialog, it ->

                    dialog.dismiss()

                }

            }

        }

        fun askForName(activity: Activity, title: Int, button: Int, negative: Int, function: (String, String) -> Unit) {

            val binding = DataBindingUtil.inflate<DialogLayoutCreateNameBinding>(activity.layoutInflater, R.layout.dialog_layout_create_name, null, false)

            val prefs = PreferenceManager.getDefaultSharedPreferences(activity)

            val uuid = prefs.getBoolean(AUTO_SCAN_NAME, false)

            with (binding.toggleButton) {

                setOnClickListener {

                    binding.barcodeView.visibility = when (text) {

                        textOff -> {

                            binding.barcodeView.pause()

                            View.GONE
                        }

                        else -> {

                            startBarcode(binding.barcodeView) {

                                object : BarcodeCallback {

                                    override fun barcodeResult(result: BarcodeResult) {

                                        if (result.text == null) return

                                        binding.editText.setText(result.text.toString())

                                    }

                                    override fun possibleResultPoints(resultPoints: MutableList<ResultPoint>?) {

                                    }
                                }
                            }

                            View.VISIBLE
                        }
                    }
                }
            }

            val builder = AlertDialog.Builder(activity).apply {

                setTitle(title)

                setView(binding.root)

                setNegativeButton(negative) { dialog, it ->

                    dialog.dismiss()

                }

                setPositiveButton(button) { dialog, it ->

                    binding.barcodeView.pause()

                    val text = binding.editText.text.toString()

                    if (text.isNotBlank()) {

                        function(text, binding.noteText.text.toString())

                    } else dialog.dismiss()

                }

                setCancelable(false)

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