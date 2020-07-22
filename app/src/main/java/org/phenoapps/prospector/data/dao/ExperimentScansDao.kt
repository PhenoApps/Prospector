package org.phenoapps.prospector.data.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Query
import org.phenoapps.prospector.data.models.Experiment
import org.phenoapps.prospector.data.models.ExperimentScans
import org.phenoapps.prospector.data.models.ScanSpectralValues
import org.phenoapps.prospector.data.models.SpectralFrame

@Dao
interface ExperimentScansDao {

    @Query("SELECT * FROM experiments ORDER BY date DESC")
    fun getExperiments(): LiveData<List<Experiment>>

    @Query("SELECT * FROM experimentscans as es WHERE es.eid = :eid ORDER BY scanDate DESC")
    fun getScans(eid: Long): LiveData<List<ExperimentScans>>

    @Query("SELECT * FROM experimentscans ORDER BY expDate DESC")
    suspend fun getAll(): List<ExperimentScans>

    @Query("SELECT * FROM scanspectralvalues as sf WHERE sf.eid = :eid and sf.sid = :sid ORDER BY sf.frameId ASC")
    suspend fun getSpectralValues(eid: Long, sid: String): List<ScanSpectralValues>

    @Query("INSERT OR REPLACE INTO experiments (name, date) VALUES (:name, :date)")
    suspend fun insertExperiment(name: String, date: String)

    @Query("INSERT OR REPLACE INTO spectral_frames(sid, fid, count, spectralValues, lightSource) VALUES(:sid, :fid, :count, :values, :light)")
    suspend fun insertFrame(sid: String, fid: Int, count: Int, values: String, light: Int)

    @Query("INSERT OR REPLACE INTO scans (eid, sid, date, deviceId, note) VALUES (:eid, :sid, :date, :deviceId, :note)")
    fun insertScan(eid: Long, sid: String, date: String, deviceId: String, note: String): Long

    @Query("DELETE FROM experiments WHERE eid = :eid")
    suspend fun deleteExperiment(eid: Long)

//    @Query("SELECT * FROM scanspectralvalues as sf WHERE sf.eid = :eid and sf.sid = :sid ORDER BY sf.frameId ASC")
//    fun forceGetSpectralValues(eid: Long, sid: Long): List<ScanSpectralValues>
//
    @Query("SELECT sf.* FROM spectral_frames as sf, scans as s WHERE s.eid = :eid and s.sid = :sid and sf.sid = :sid ORDER BY sf.fid ASC")
    fun forceGetSpectralValues(eid: Long, sid: String): LiveData<List<SpectralFrame>>

}