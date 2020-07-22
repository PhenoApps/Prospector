package org.phenoapps.prospector.data.models

import androidx.annotation.Keep
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey

@Keep
@ForeignKey(
        entity = Scan::class,
        parentColumns = ["sid"],
        childColumns = ["sid"])
@Entity(tableName = "spectral_frames",
        primaryKeys = ["fid", "sid"])
data class SpectralFrame(

        @ColumnInfo(name = "sid")
        var sid: String,

        @ColumnInfo(name = "fid")
        var frameId: Int,

        @ColumnInfo(name = "count")
        var count: Int,

        @ColumnInfo(name = "spectralValues")
        var spectralValues: String,

        @ColumnInfo(name = "lightSource")
        var lightSource: Int)