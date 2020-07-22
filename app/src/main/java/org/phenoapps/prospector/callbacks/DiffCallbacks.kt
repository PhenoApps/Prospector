package org.phenoapps.prospector.callbacks

import androidx.recyclerview.widget.DiffUtil
import org.phenoapps.prospector.data.models.Experiment
import org.phenoapps.prospector.data.models.Scan

class DiffCallbacks {

    companion object {

        class ExperimentDiffCallback : DiffUtil.ItemCallback<Experiment>() {

            override fun areItemsTheSame(oldItem: Experiment, newItem: Experiment): Boolean {
                return oldItem.name == newItem.name
            }

            override fun areContentsTheSame(oldItem: Experiment, newItem: Experiment): Boolean {
                return oldItem.name == newItem.name
            }
        }

        class ScanDiffCallback : DiffUtil.ItemCallback<Scan>() {

            override fun areItemsTheSame(oldItem: Scan, newItem: Scan): Boolean {
                return oldItem.sid == newItem.sid
            }

            override fun areContentsTheSame(oldItem: Scan, newItem: Scan): Boolean {
                return oldItem.sid == newItem.sid
            }
        }
    }

}
