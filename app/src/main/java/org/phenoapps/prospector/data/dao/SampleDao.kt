package org.phenoapps.prospector.data.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Query
import org.phenoapps.prospector.data.models.DeviceTypeExport
import org.phenoapps.prospector.data.models.Sample
import org.phenoapps.prospector.data.models.SampleScanCount

@Dao
interface SampleDao {

    /** View queries **/
    @Query("SELECT * FROM DeviceTypeExport")
    fun getDeviceTypeExports(): LiveData<List<DeviceTypeExport>>

    @Query("SELECT DISTINCT * FROM SampleScanCount WHERE eid = :eid")
    fun getSampleScanCounts(eid: Long): LiveData<List<SampleScanCount>>

    @Query("SELECT DISTINCT * FROM samples")
    fun getSamples(): LiveData<List<Sample>>

    @Query("SELECT DISTINCT * FROM samples WHERE samples.eid = :eid")
    fun getSamplesLive(eid: Long): LiveData<List<Sample>>

    @Query("SELECT DISTINCT * FROM samples WHERE samples.eid = :eid")
    suspend fun getSamples(eid: Long): List<Sample>

    /**
     * Inserts
     */
    /** attempts at new sample creations shouldn't overwrite old sample rows **/
    @Query("INSERT OR IGNORE INTO samples (eid, name, date, note) VALUES (:eid, :name, :date, :note)")
    suspend fun insertSample(eid: Long, name: String, date: String, note: String)

    /**
     * Deletes
     */
    @Query("DELETE FROM samples WHERE eid = :eid and name = :name")
    suspend fun deleteSample(eid: Long, name: String)

}