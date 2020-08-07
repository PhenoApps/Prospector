package org.phenoapps.prospector.data.models

import androidx.annotation.Keep
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import org.json.JSONObject

@Keep
@Entity(tableName = "spectral_frames",
        foreignKeys = [
            ForeignKey(
                    entity = Scan::class,
                    parentColumns = ["sid"],
                    childColumns = ["sid"], onDelete = ForeignKey.CASCADE)],
        primaryKeys = ["fid", "sid"])
data class SpectralFrame(

        @ColumnInfo(name = "sid")
        var sid: Long,

        @ColumnInfo(name = "fid")
        var frameId: Int,

        @ColumnInfo(name = "spectralValues")
        var spectralValues: String,

        @ColumnInfo(name = "lightSource")
        var lightSource: Int) {

//        private fun String.toWavelengthJson() {
//
//                val output = JSONObject()
//
//                mapOf(this.split(" "))
//        }

    fun toJson(): JSONObject {

        return JSONObject().apply {

            put("lightSource", lightSource)

            put("spectralValues", spectralValues)

        }
    }
}