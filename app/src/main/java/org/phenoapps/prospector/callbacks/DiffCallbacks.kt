package org.phenoapps.prospector.callbacks

import androidx.recyclerview.widget.DiffUtil
import org.phenoapps.prospector.adapter.ExperimentAdapter
import org.phenoapps.prospector.data.models.SampleScanCount
import org.phenoapps.prospector.data.models.Scan
import org.phenoapps.prospector.fragments.SampleListFragment

/**
 * DiffCallbacks are used in each adapter implementation.
 * They define equality/uniqueness of each item in a list.
 * Duplicate items will not show up if areItemsTheSame is true.
 */
class DiffCallbacks {

    companion object {

        class ExperimentDiffCallback : DiffUtil.ItemCallback<ExperimentAdapter.ExperimentListItem>() {

            override fun areItemsTheSame(oldItem: ExperimentAdapter.ExperimentListItem, newItem: ExperimentAdapter.ExperimentListItem): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: ExperimentAdapter.ExperimentListItem, newItem: ExperimentAdapter.ExperimentListItem): Boolean {
                return oldItem.id == newItem.id
            }
        }

        class SampleScanCountDiffCallback : DiffUtil.ItemCallback<SampleListFragment.IndexedSampleScanCount>() {

            override fun areItemsTheSame(oldItem: SampleListFragment.IndexedSampleScanCount, newItem: SampleListFragment.IndexedSampleScanCount): Boolean {
                return oldItem.index == newItem.index
            }

            override fun areContentsTheSame(oldItem: SampleListFragment.IndexedSampleScanCount, newItem: SampleListFragment.IndexedSampleScanCount): Boolean {
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
