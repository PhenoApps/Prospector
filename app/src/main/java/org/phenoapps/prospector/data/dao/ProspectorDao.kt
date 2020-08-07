package org.phenoapps.prospector.data.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Query
import org.phenoapps.prospector.data.models.Experiment
import org.phenoapps.prospector.data.models.Sample
import org.phenoapps.prospector.data.models.Scan
import org.phenoapps.prospector.data.models.SpectralFrame

@Dao
interface ProspectorDao {

    @Query("SELECT DISTINCT * FROM experiments ORDER BY date DESC")
    fun getExperiments(): LiveData<List<Experiment>>

    @Query("SELECT DISTINCT * FROM samples")
    fun getSamples(): LiveData<List<Sample>>

    @Query("SELECT * FROM scans ORDER BY date DESC")
    fun getScans(): LiveData<List<Scan>>

    @Query("SELECT * FROM spectral_frames")
    fun getFrames(): LiveData<List<SpectralFrame>>

    @Query("SELECT sf.* FROM spectral_frames as sf, experiments as e, scans as s WHERE e.eid = :eid and s.eid = :eid and s.sid = :sid and sf.sid = :sid")
    fun getSpectralValues(eid: Long, sid: Long): List<SpectralFrame>

    @Query("SELECT sf.* FROM spectral_frames as sf, experiments as e, scans as s WHERE e.eid = :eid and s.eid = :eid and s.sid = :sid and sf.sid = :sid")
    fun getSpectralValuesLive(eid: Long, sid: Long): LiveData<List<SpectralFrame>>

    @Query("SELECT DISTINCT * FROM samples WHERE samples.eid = :eid")
    fun getSamples(eid: Long): LiveData<List<Sample>>

    @Query("SELECT * FROM scans as s WHERE s.name = :sample and s.eid = :eid  ORDER BY s.date DESC")
    fun getScans(eid: Long, sample: String): LiveData<List<Scan>>

    /**
     * Inserts
     */
    @Query("INSERT INTO experiments (name, date) VALUES (:name, :date)")
    suspend fun insertExperiment(name: String, date: String): Long

    @Query("INSERT OR REPLACE INTO spectral_frames(sid, fid, spectralValues, lightSource) VALUES(:sid, :fid, :values, :light)")
    suspend fun insertFrame(sid: Long, fid: Int, values: String, light: Int)

    @Query("INSERT INTO scans (eid, name, date, deviceId, operator, deviceType, lightSource) VALUES (:eid, :name, :date, :deviceId, :operator, :deviceType, :lightSource)")
    suspend fun insertScan(eid: Long, name: String, date: String, deviceId: String, operator: String, deviceType: String, lightSource: Int): Long

    /** attempts at new sample creations shouldn't overwrite old sample rows **/
    @Query("INSERT OR IGNORE INTO samples (eid, name, date, note) VALUES (:eid, :name, :date, :note)")
    suspend fun insertSample(eid: Long, name: String, date: String, note: String)

    /**
     * Deletes
     */
    @Query("DELETE FROM scans WHERE eid = :eid and name = :name")
    suspend fun deleteScans(eid: Long, name: String)

    @Query("DELETE FROM scans WHERE sid = :sid")
    suspend fun deleteScan(sid: Long)

    @Query("DELETE FROM samples WHERE eid = :eid and name = :name")
    suspend fun deleteSample(eid: Long, name: String)

    @Query("DELETE FROM experiments WHERE eid = :eid")
    suspend fun deleteExperiment(eid: Long)
}