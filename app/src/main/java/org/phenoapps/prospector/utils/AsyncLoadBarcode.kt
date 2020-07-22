package org.phenoapps.prospector.utils

import android.graphics.Bitmap
import android.graphics.Color

import android.os.AsyncTask
import android.widget.ImageView
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel


//TODO: chaneylc rendered barcodes are not whats printed

class AsyncLoadBarcode(val imageView: ImageView, val TAG: String) : AsyncTask<String?, Void?, Bitmap?>() {

    override fun doInBackground(vararg codes: String?): Bitmap? {

        val code = codes.first()

        if (code?.isNotBlank() != false) {

            val bitmatrix = QRCodeWriter()
                    .encode(code, BarcodeFormat.QR_CODE, 256, 256,
                            mapOf(EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.Q))

            val bmp = Bitmap.createBitmap(bitmatrix.width, bitmatrix.height, Bitmap.Config.RGB_565)

            for (x in 0 until bitmatrix.width) {

                for (y in 0 until bitmatrix.height) {

                    bmp.setPixel(x, y, if (bitmatrix.get(x, y)) Color.BLACK else Color.WHITE)
                }
            }

            return bmp

        }

        return null

    }

    override fun onPostExecute(bitmap: Bitmap?) {

        if (imageView.tag == TAG) {

            bitmap?.let {

                imageView.setImageBitmap(it)

            }
        }
    }
}