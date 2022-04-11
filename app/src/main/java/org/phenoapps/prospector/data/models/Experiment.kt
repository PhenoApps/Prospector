package org.phenoapps.prospector.data.models

import androidx.annotation.Keep
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import org.phenoapps.prospector.utils.DateUtil

@Keep
@Entity(tableName = "experiments")
data class Experiment(

        @ColumnInfo(name = "name", index = true)
        var name: String,

        @ColumnInfo(name = "deviceType")
        var deviceType: String,

        @ColumnInfo(name = "note")
        var note: String? = null,

        @ColumnInfo(name = "config")
        var config: String? = null,

        @ColumnInfo(name = "eid")
        @PrimaryKey(autoGenerate = true)
        var eid: Long? = null) {

    @ColumnInfo(name = "date")
    var date = DateUtil().getTime()
}
