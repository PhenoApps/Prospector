package org.phenoapps.prospector.data

import androidx.lifecycle.LiveData
import org.phenoapps.prospector.data.dao.ExperimentScansDao
import org.phenoapps.prospector.data.models.Scan
import org.phenoapps.prospector.data.models.SpectralFrame

class ExperimentScansRepository
    private constructor(
            private val expScansDao: ExperimentScansDao) {



    fun getExperiments() = expScansDao.getExperiments()

    fun forceGetSpectralValues(eid: Long, sid: String): LiveData<List<SpectralFrame>> = expScansDao.forceGetSpectralValues(eid, sid)

    fun getScans(eid: Long) = expScansDao.getScans(eid)

    suspend fun getAll() = expScansDao.getAll()

    suspend fun getSpectralValues(eid: Long, sid: String) = expScansDao.getSpectralValues(eid, sid)

    suspend fun deleteExperiment(eid: Long) = expScansDao.deleteExperiment(eid)

    suspend fun insertExperiment(name: String, date: String) = expScansDao.insertExperiment(name, date)

    suspend fun insertFrame(sid: String, frame: SpectralFrame) = expScansDao.insertFrame(sid, frame.frameId, frame.count, frame.spectralValues, frame.lightSource)

    fun insertScan(scan: Scan) = expScansDao.insertScan(scan.eid, scan.sid, scan.date ?: "", scan.deviceId ?: "", scan.note ?: "")

    companion object {

        @Volatile private var instance: ExperimentScansRepository? = null

        fun getInstance(dao: ExperimentScansDao) =
                instance ?: synchronized(this) {
                    instance ?: ExperimentScansRepository(dao)
                        .also { instance = it }
                }
    }
}