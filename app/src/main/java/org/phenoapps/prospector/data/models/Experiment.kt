package org.phenoapps.prospector.data.models

import androidx.annotation.Keep
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Keep
@Entity(tableName = "experiments")
data class Experiment(

        @ColumnInfo(name = "name")
        var name: String,

        @ColumnInfo(name = "eid")
        @PrimaryKey(autoGenerate = true)
        var eid: Long? = null) {

    @ColumnInfo(name = "date")
    var date: String = String()
}
