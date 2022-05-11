package org.phenoapps.prospector.callbacks

import androidx.recyclerview.widget.DiffUtil
import org.phenoapps.prospector.adapter.ExperimentAdapter
import org.phenoapps.prospector.fragments.SampleListFragment
import org.phenoapps.prospector.fragments.ScanListFragment

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

        class ScanFrameDiffCallback : DiffUtil.ItemCallback<ScanListFragment.ScanFrames>() {

            override fun areItemsTheSame(oldItem: ScanListFragment.ScanFrames, newItem: ScanListFragment.ScanFrames): Boolean {
                return oldItem.sid == newItem.sid && oldItem.fid == newItem.fid
            }

            override fun areContentsTheSame(oldItem: ScanListFragment.ScanFrames, newItem: ScanListFragment.ScanFrames): Boolean {
                return oldItem.sid == newItem.sid && oldItem.fid == newItem.fid
            }
        }
    }
}
