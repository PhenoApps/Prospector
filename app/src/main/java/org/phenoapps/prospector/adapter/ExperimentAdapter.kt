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
import org.phenoapps.prospector.data.models.Experiment
import org.phenoapps.prospector.databinding.ListItemExperimentBinding
import org.phenoapps.prospector.fragments.ExperimentListFragmentDirections

class ExperimentAdapter(
        val context: Context
) : ListAdapter<Experiment, ExperimentAdapter.ViewHolder>(DiffCallbacks.Companion.ExperimentDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
                DataBindingUtil.inflate(
                        LayoutInflater.from(parent.context),
                        R.layout.list_item_experiment, parent, false
                )
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        getItem(position).let { experiment ->

            with(holder) {

                itemView.tag = experiment.eid

                bind(experiment)
            }
        }
    }

    class ViewHolder(private val binding: ListItemExperimentBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(experiment: Experiment) {

            with(binding) {

                clickListener = View.OnClickListener {

                    experiment.eid?.let { eid ->

                        Navigation.findNavController(binding.root).navigate(
                                ExperimentListFragmentDirections.actionToScanList(eid))

                    }

                }

                this.experiment = experiment

                executePendingBindings()
            }
        }
    }
}