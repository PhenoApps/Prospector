package org.phenoapps.prospector.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.appcompat.app.AlertDialog
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import org.phenoapps.prospector.R
import org.phenoapps.prospector.callbacks.DiffCallbacks
import org.phenoapps.prospector.data.models.Scan
import org.phenoapps.prospector.databinding.ListItemScanBinding
import org.phenoapps.prospector.interfaces.GraphItemClickListener
import org.phenoapps.prospector.utils.Dialogs

class ScansAdapter(val context: Context, private val listener: GraphItemClickListener) : ListAdapter<Scan,
        ScansAdapter.ScanGraphViewHolder>(DiffCallbacks.Companion.ScanDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ScanGraphViewHolder {

        return ScanGraphViewHolder(

            DataBindingUtil.inflate(

                LayoutInflater.from(parent.context),

                R.layout.list_item_scan, parent, false

            )
        )
    }

    override fun onBindViewHolder(holder: ScanGraphViewHolder, position: Int) {

        getItem(position).let { scan ->

            with(holder) {

                val id = (scan.sid ?: -1L).toString()

                itemView.tag = id

                itemView.setOnLongClickListener {

                    val adapter = ArrayAdapter<String>(context, android.R.layout.simple_list_item_1)

                    adapter.addAll("red", "blue", "green")
                    Dialogs.showColorChooserDialog(adapter, AlertDialog.Builder(context),
                        context.getString(R.string.frag_scan_list_dialog_choose_color_title)) {

                        listener.onItemLongClicked(id.toLong(), it)

                    }
                    true

                }

                itemView.setOnClickListener {

                    listener.onItemClicked(id.toLong(), scan.color)

                }

                bind(id, scan.date)
            }
        }
    }

    inner class ScanGraphViewHolder(
            private val binding: ListItemScanBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(tag: String, date: String) {

            with(binding) {

                if (itemView.tag == tag) {

                    this.date = date

                }
            }
        }
    }
}