package org.phenoapps.prospector.interfaces

/**
 * Interface used to communicate scan adapter with ScanListFragment
 */
interface GraphItemClickListener {

    fun onItemClicked(sid: Long, fid: Int, color: String?)

    fun onItemLongClicked(sid: Long, fid: Int, color: String?)
}