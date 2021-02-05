package org.phenoapps.prospector.interfaces

/**
 * Interface used to communicate scan adapter with ScanListFragment
 */
interface GraphItemClickListener {

    fun onItemClicked(id: Long, color: String?)

    fun onItemLongClicked(id: Long, color: String?)
}