package org.phenoapps.prospector.data.models

import androidx.annotation.Keep
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import org.phenoapps.prospector.utils.DateUtil

@Keep

@Entity(tableName = "scans",
        foreignKeys = [
            ForeignKey(
                entity = Sample::class,
                parentColumns = ["eid", "name"],
                childColumns = ["eid", "name"], onDelete = ForeignKey.CASCADE)
        ])
data class Scan(

        @ColumnInfo(name = "eid")
        var eid: Long,

        @ColumnInfo(name = "name")
        var name: String,

        @ColumnInfo(name = "sid")
        @PrimaryKey(autoGenerate = true)
        var sid: Long? = null) {

    @ColumnInfo(name = "date")
    var date = DateUtil().getTime()

    @ColumnInfo(name = "deviceType")
    var deviceType: String = "LinkSquare"

    @ColumnInfo(name = "deviceId")
    var deviceId: String? = null

    @ColumnInfo(name = "operator")
    var operator: String? = null

    @ColumnInfo(name = "lightSource")
    var lightSource: Int? = null

    override fun equals(other: Any?): Boolean {

        return when (other) {

            is Scan -> other.sid == this.sid

            else -> false
        }
    }

    override fun hashCode(): Int {

        return this.sid.hashCode()

    }
}
