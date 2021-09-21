package org.phenoapps.prospector.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.navigation.Navigation
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import org.phenoapps.prospector.R
import org.phenoapps.prospector.callbacks.DiffCallbacks
import org.phenoapps.prospector.data.models.SampleScanCount
import org.phenoapps.prospector.databinding.ListItemSampleBinding
import org.phenoapps.prospector.fragments.SampleListFragmentDirections
import org.phenoapps.prospector.interfaces.SampleListClickListener

class SampleAdapter(
        private val context: Context,
        private val listener: SampleListClickListener
) : ListAdapter<SampleScanCount, SampleAdapter.ViewHolder>(DiffCallbacks.Companion.SampleScanCountDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
                DataBindingUtil.inflate(
                        LayoutInflater.from(parent.context),
                        R.layout.list_item_sample, parent, false
                )
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        getItem(position).let { sample ->

            with(holder) {

                itemView.tag = sample.name

                itemView.setOnLongClickListener {
                    listener.onListItemLongClicked(sample)
                    true
                }

                bind(sample, position)
            }
        }
    }

    inner class ViewHolder(private val binding: ListItemSampleBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(sample: SampleScanCount, listPosition: Int) {

            with(binding) {

                position = listPosition + 1

                clickListener = View.OnClickListener {

                    Navigation.findNavController(binding.root).navigate(
                            SampleListFragmentDirections.actionToScanList(sample.eid, sample.name))

                }

                this.scanCount = context.resources
                        .getQuantityString(R.plurals.numberOfScans, sample.count, sample.count)

                this.sample = sample

            }
        }
    }
}