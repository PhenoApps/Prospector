package org.phenoapps.prospector.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import org.phenoapps.prospector.R
import org.phenoapps.prospector.adapter.models.TextIcon


class TextIconAdapter(context: Context, private val data: List<TextIcon>,
                      private val resource: Int = R.layout.list_item_text_icon) :
    ArrayAdapter<TextIcon?>(context, resource, data) {

    @SuppressLint("ViewHolder")
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {

        val view = LayoutInflater.from(context).inflate(resource, parent, false)

        val icon = view.findViewById<ImageView>(R.id.list_item_text_icon_iv)
        val text = view.findViewById<TextView>(R.id.list_item_text_icon_tv)

        val model = data[position]

        icon.setImageDrawable(AppCompatResources.getDrawable(context, model.icon))
        text.text = model.text

        return view
    }
}