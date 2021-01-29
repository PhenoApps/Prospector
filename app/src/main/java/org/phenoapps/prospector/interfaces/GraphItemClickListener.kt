package org.phenoapps.prospector.interfaces

interface GraphItemClickListener {

    fun onItemClicked(id: Long, color: String?)

    fun onItemLongClicked(id: Long, color: String?)
}