package org.phenoapps.prospector.callbacks

import androidx.recyclerview.widget.DiffUtil
import org.phenoapps.prospector.data.models.Experiment
import org.phenoapps.prospector.data.models.Sample
import org.phenoapps.prospector.data.models.Scan

/**
 * DiffCallbacks are used in each adapter implementation.
 * They define equality/uniqueness of each item in a list.
 * Duplicate items will not show up if areItemsTheSame is true.
 */
class DiffCallbacks {

    companion object {

        class ExperimentDiffCallback : DiffUtil.ItemCallback<Experiment>() {

            override fun areItemsTheSame(oldItem: Experiment, newItem: Experiment): Boolean {
                return oldItem.eid == newItem.eid
            }

            override fun areContentsTheSame(oldItem: Experiment, newItem: Experiment): Boolean {
                return oldItem.eid == newItem.eid
            }
        }

        class SampleDiffCallback : DiffUtil.ItemCallback<Sample>() {

            override fun areItemsTheSame(oldItem: Sample, newItem: Sample): Boolean {
                return oldItem.name == newItem.name
            }

            override fun areContentsTheSame(oldItem: Sample, newItem: Sample): Boolean {
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
