package org.phenoapps.prospector.data.models

import DEVICE_TYPE_NIR
import androidx.annotation.Keep
import androidx.room.*
import org.phenoapps.prospector.utils.DateUtil

@Keep

@Entity(tableName = "scans",
        indices = [Index(value = ["eid", "name"], unique = false)],
        foreignKeys = [
            ForeignKey(
                entity = Sample::class,
                parentColumns = ["eid", "name"],
                childColumns = ["eid", "name"],
                onUpdate = ForeignKey.CASCADE,
                onDelete = ForeignKey.CASCADE)
        ])
data class Scan(

        @ColumnInfo(name = "eid", index = true)
        var eid: Long,

        @ColumnInfo(name = "name", index = true)
        var name: String,

        @ColumnInfo(name = "date")
        var date: String = DateUtil().getTime(),

        @ColumnInfo(name = "deviceType")
        var deviceType: String = DEVICE_TYPE_NIR,

        @ColumnInfo(name = "color")
        var color: String? = null,

        @ColumnInfo(name = "deviceId")
        var deviceId: String? = null,

        @ColumnInfo(name = "alias")
        var alias: String? = null,

        @ColumnInfo(name = "operator")
        var operator: String? = null,

        @ColumnInfo(name = "lightSource")
        var lightSource: Int? = null,

        @ColumnInfo(name = "serial")
        var serial: String? = null,

        @ColumnInfo(name = "humidity")
        var humidity: String? = null,

        @ColumnInfo(name = "temperature")
        var temperature: String? = null,

        @ColumnInfo(name = "model")
        var model: String? = null,

        @ColumnInfo(name = "sid")
        @PrimaryKey(autoGenerate = true)
        var sid: Long? = null) {

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
