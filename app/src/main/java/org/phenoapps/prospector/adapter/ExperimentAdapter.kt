package org.phenoapps.prospector.adapter

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat.startActivity
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import org.phenoapps.prospector.R
import org.phenoapps.prospector.Selection_Scan
import org.phenoapps.prospector.databinding.ListItemExperimentBinding
import org.phenoapps.prospector.models.Experiment

class ExperimentAdapter(
        val context: Context
) : ListAdapter<Experiment, ExperimentAdapter.ViewHolder>(ExperimentDiffCallback()) {

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

                itemView.tag = experiment

                bind(View.OnClickListener {

                    val intent = Intent(context, Selection_Scan::class.java)

                    intent.putExtra("experiment", experiment.name)

                    startActivity(context, intent, null)

                }, experiment)
            }
        }
    }

    class ViewHolder(private val binding: ListItemExperimentBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(listener: View.OnClickListener, experiment: Experiment) {

            with(binding) {

                clickListener = listener

                this.experiment = experiment

                executePendingBindings()
            }
        }
    }
}

private class ExperimentDiffCallback : DiffUtil.ItemCallback<Experiment>() {

    override fun areItemsTheSame(oldItem: Experiment, newItem: Experiment): Boolean {
        return oldItem.name == newItem.name
    }

    override fun areContentsTheSame(oldItem: Experiment, newItem: Experiment): Boolean {
        return oldItem.name == newItem.name
    }
}