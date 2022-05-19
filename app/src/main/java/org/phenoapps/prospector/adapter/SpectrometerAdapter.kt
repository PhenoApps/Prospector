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
import org.phenoapps.prospector.databinding.ListItemExperimentBinding
import org.phenoapps.prospector.databinding.ListItemSpectrometerBinding
import org.phenoapps.prospector.fragments.ExperimentListFragmentDirections
import org.phenoapps.prospector.interfaces.OnModelClickListener
import org.phenoapps.prospector.utils.DateUtil

/**
 * The adapter class used in the experiment list fragment recycler view.
 * Nothing special here, experiments can be clicked to navigate to their respective samples list fragment.
 */
class SpectrometerAdapter(
        private val listener: OnModelClickListener
) : ListAdapter<SpectrometerAdapter.SpectrometerListItem, SpectrometerAdapter.ViewHolder>(DiffCallbacks.Companion.SpectrometerDiffCallback()) {

    data class SpectrometerListItem(
        val id: Int,
        val title: String,
        val icon: Int) {
        override fun equals(other: Any?): Boolean {
            return if (other is SpectrometerListItem) {
                other.title == title
            } else false
        }

        override fun hashCode(): Int {
            var result = id
            result = 31 * result + title.hashCode()
            result = 31 * result + icon
            return result
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            DataBindingUtil.inflate<ListItemSpectrometerBinding?>(
                    LayoutInflater.from(parent.context),
                    R.layout.list_item_spectrometer, parent, false
            ).also {
                it.root.setOnClickListener { view ->
                    listener.onClickModel(view.tag)
                }
            }
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        getItem(position).let { spectrometer ->

            with(holder) {

                itemView.tag = spectrometer

                bind(spectrometer)
            }
        }
    }

    inner class ViewHolder(private val binding: ListItemSpectrometerBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(spectrometer: SpectrometerListItem) {

            with(binding) {

                this.title = spectrometer.title

                this.drawable = spectrometer.icon
            }
        }
    }
}