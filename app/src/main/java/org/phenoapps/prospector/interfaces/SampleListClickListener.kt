package org.phenoapps.prospector.interfaces

import org.phenoapps.prospector.data.models.SampleScanCount

interface SampleListClickListener {
    fun onListItemLongClicked(sample: SampleScanCount) = Unit
}