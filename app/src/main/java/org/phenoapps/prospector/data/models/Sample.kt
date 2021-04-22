package org.phenoapps.prospector.data.models

import androidx.annotation.Keep
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import org.phenoapps.prospector.utils.DateUtil

@Keep

@Entity(tableName = "samples",
        primaryKeys = ["eid", "name"],
        foreignKeys = [ForeignKey(
                onDelete = ForeignKey.CASCADE,
                entity = Experiment::class,
                parentColumns = ["eid"], childColumns = ["eid"])])
data class Sample(

        @ColumnInfo(name = "eid", index = true)
        val eid: Long,

        @ColumnInfo(name = "name", index = true)
        var name: String,

        @ColumnInfo(name = "date")
        var date: String = DateUtil().getTime(),

        @ColumnInfo(name = "note")
        var note: String = String()) {

    override fun equals(other: Any?): Boolean {

        return when (other) {

            is Sample -> other.name == this.name && other.eid == this.eid

            else -> false
        }
    }

    override fun hashCode(): Int {

        return (name+date).hashCode()

    }

}
