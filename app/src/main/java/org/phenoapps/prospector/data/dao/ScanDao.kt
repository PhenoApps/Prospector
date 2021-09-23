package org.phenoapps.prospector.data.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Query
import org.phenoapps.prospector.data.models.Scan
import org.phenoapps.prospector.data.models.SpectralFrame

@Dao
interface ScanDao {

    @Query("SELECT * FROM scans ORDER BY date DESC")
    fun getScans(): LiveData<List<Scan>>

    @Query("SELECT * FROM spectral_frames")
    fun getFrames(): LiveData<List<SpectralFrame>>

    @Query("SELECT sf.* FROM spectral_frames as sf, experiments as e, scans as s WHERE e.eid = :eid and s.eid = :eid and s.sid = :sid and sf.sid = :sid")
    fun getSpectralValues(eid: Long, sid: Long): List<SpectralFrame>

    @Query("SELECT sf.* FROM spectral_frames as sf, experiments as e, scans as s, samples as sam WHERE e.eid = :eid and s.name = :sample and s.eid = :eid and sam.name = :sample and sf.sid = s.sid")
    fun getSpectralValues(eid: Long, sample: String): LiveData<List<SpectralFrame>>

    @Query("SELECT sf.* FROM spectral_frames as sf, experiments as e, scans as s WHERE e.eid = :eid and s.eid = :eid and s.sid = :sid and sf.sid = :sid")
    fun getSpectralValuesLive(eid: Long, sid: Long): LiveData<List<SpectralFrame>>

    @Query("SELECT * FROM scans as s WHERE s.name = :sample and s.eid = :eid  ORDER BY s.date DESC")
    fun getScans(eid: Long, sample: String): LiveData<List<Scan>>

    /**
     * Inserts
     */
    @Query("INSERT OR REPLACE INTO spectral_frames(sid, fid, spectralValues, lightSource) VALUES(:sid, :fid, :values, :light)")
    suspend fun insertFrame(sid: Long, fid: Int, values: String, light: Int)

    @Query("INSERT INTO scans (eid, name, date, deviceId, operator, deviceType, lightSource) VALUES (:eid, :name, :date, :deviceId, :operator, :deviceType, :lightSource)")
    suspend fun insertScan(eid: Long, name: String, date: String, deviceId: String, operator: String, deviceType: String, lightSource: Int): Long

    @Query("UPDATE scans SET color = :color WHERE eid = :eid AND sid = :scanId")
    fun updateScanColor(eid: Long, scanId: Long, color: String)

    /**
     * Deletes
     */
    @Query("DELETE FROM scans WHERE eid = :eid and name = :name")
    suspend fun deleteScans(eid: Long, name: String)

    @Query("DELETE FROM scans WHERE sid = :sid")
    suspend fun deleteScan(sid: Long)

}