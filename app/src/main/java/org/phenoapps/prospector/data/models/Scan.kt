package org.phenoapps.prospector.data.models

import androidx.annotation.Keep
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey

/**
 * Scans is the new Room SQL table that holds spectrometry frame data
 * from Linked Square devices.
 */
@Keep
@ForeignKey(
        entity = Experiment::class,
        parentColumns = ["eid"],
        childColumns = ["eid"])
@Entity(tableName = "scans",
        primaryKeys = ["eid", "sid"])
data class Scan(

        @ColumnInfo(name = "eid")
        val eid: Long,

        @ColumnInfo(name = "sid")
        var sid: String) {

    @ColumnInfo(name = "date")
    var date: String? = null

    @ColumnInfo(name = "deviceId")
    var deviceId: String? = null

    @ColumnInfo(name = "note")
    var note: String? = null

    override fun equals(other: Any?): Boolean {

        return when (other) {

            is Scan -> other.sid == this.sid && other.eid == this.eid

            else -> false
        }
    }

    override fun hashCode(): Int {

        return this.sid.hashCode()

    }

}
