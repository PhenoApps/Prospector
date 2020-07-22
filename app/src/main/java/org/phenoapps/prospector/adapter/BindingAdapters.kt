package org.phenoapps.prospector.adapter

import android.widget.ImageView
import androidx.databinding.BindingAdapter
import org.phenoapps.prospector.utils.AsyncLoadBarcode

@BindingAdapter("setQRCode")
fun bindQRCodeImage(view: ImageView, code: String?) {

    code?.let {

        view.tag = code

        AsyncLoadBarcode(view, code).execute(code)

    }
}