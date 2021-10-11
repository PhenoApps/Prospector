package org.phenoapps.prospector.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.appcompat.app.AlertDialog
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import org.phenoapps.prospector.R
import org.phenoapps.prospector.callbacks.DiffCallbacks
import org.phenoapps.prospector.databinding.ListItemScanBinding
import org.phenoapps.prospector.fragments.ScanListFragment
import org.phenoapps.prospector.interfaces.GraphItemClickListener
import org.phenoapps.prospector.utils.DateUtil
import org.phenoapps.prospector.utils.Dialogs

class ScansAdapter(val context: Context, private val listener: GraphItemClickListener) : ListAdapter<ScanListFragment.ScanFrames,
        ScansAdapter.ScanGraphViewHolder>(DiffCallbacks.Companion.ScanFrameDiffCallback()) {

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

                itemView.tag = scan.sid

                bind(scan) {

                    listener.onItemClicked(scan.sid, scan.fid, scan.color)

                }
            }
        }
    }

    inner class ScanGraphViewHolder(
            private val binding: ListItemScanBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(scan: ScanListFragment.ScanFrames, onClick: View.OnClickListener) {

            with(binding) {

                if (itemView.tag == scan.sid) {

                    nameView.setOnLongClickListener {

                        val adapter = ArrayAdapter<String>(context, android.R.layout.simple_list_item_1)

                        adapter.addAll("red", "blue", "green")
                        Dialogs.showColorChooserDialog(adapter, AlertDialog.Builder(context),
                                context.getString(R.string.frag_scan_list_dialog_choose_color_title)) {

                            listener.onItemLongClicked(scan.sid, scan.fid, it)

                        }

                        true
                    }

                    this.onClick = onClick

                    this.date = DateUtil().displayScanTime(scan.date)

                }
            }
        }
    }
}