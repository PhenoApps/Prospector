package org.phenoapps.prospector.adapter

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat.startActivity
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import org.phenoapps.prospector.R
import org.phenoapps.prospector.activities.ScanActivity
import org.phenoapps.prospector.callbacks.DiffCallbacks
import org.phenoapps.prospector.data.models.Experiment
import org.phenoapps.prospector.databinding.ListItemExperimentBinding

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

                bind(View.OnClickListener {

                    val intent = Intent(context, ScanActivity::class.java)

                    intent.putExtra("experiment", experiment.eid)

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