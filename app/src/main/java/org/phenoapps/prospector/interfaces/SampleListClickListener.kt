package org.phenoapps.prospector.interfaces

import org.phenoapps.prospector.fragments.SampleListFragment

interface SampleListClickListener {
    fun onListItemLongClicked(sample: SampleListFragment.IndexedSampleScanCount) = Unit
}