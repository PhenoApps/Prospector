package org.phenoapps.prospector.data

import org.phenoapps.prospector.data.dao.ProspectorDao
import org.phenoapps.prospector.data.models.Sample
import org.phenoapps.prospector.data.models.Scan
import org.phenoapps.prospector.data.models.SpectralFrame
import org.phenoapps.prospector.utils.DateUtil

class ProspectorRepository
    private constructor(
            private val dao: ProspectorDao) {

    fun getExperiments() = dao.getExperiments()
    fun getSamples() = dao.getSamples()
    fun getScans() = dao.getScans()
    fun getFrames() = dao.getFrames()
    fun getSamples(eid: Long) = dao.getSamples(eid)
    fun getSpectralValues(eid: Long, sid: Long): List<SpectralFrame> = dao.getSpectralValues(eid, sid)
    fun getSpectralValuesLive(eid: Long, sid: Long)= dao.getSpectralValuesLive(eid, sid)

    fun getScans(eid: Long, sid: String) = dao.getScans(eid, sid)

    suspend fun deleteScan(sid: Long) = dao.deleteScan(sid)
    suspend fun deleteScans(eid: Long, name: String) = dao.deleteScans(eid, name)
    suspend fun deleteSample(eid: Long, name: String) = dao.deleteSample(eid, name)

//    fun forceGetSpectralValues(eid: Long, sid: String): LiveData<List<SpectralFrame>> = dao.forceGetSpectralValues(eid, sid)

    //fun getScans(eid: Long) = dao.getScans(eid)

//
//    fun spectralFrames(eid: Long, sid: String) = dao.spectralFrames(eid, sid)

    //suspend fun getAll() = dao.getAll()

    suspend fun deleteExperiment(eid: Long) = dao.deleteExperiment(eid)

//    suspend fun deleteScan(eid: Long, sid: String) = dao.deleteScan(eid, sid)

    suspend fun insertExperiment(name: String, date: String): Long {

        var actualDate = date

        if (actualDate.isBlank()) {

            actualDate = DateUtil().getTime()

        }

        return dao.insertExperiment(name, actualDate)

    }

    suspend fun insertFrame(sid: Long, frame: SpectralFrame) = dao.insertFrame(sid, frame.frameId, frame.spectralValues, frame.lightSource)

    suspend fun insertSample(sample: Sample) = dao.insertSample(sample.eid, sample.name, sample.date, sample.note ?: "")

    suspend fun insertScan(scan: Scan): Long = dao.insertScan(scan.eid, scan.name, scan.date, scan.deviceId ?: "", scan.operator ?: "", scan.deviceType, scan.lightSource ?: -1)

    companion object {

        @Volatile private var instance: ProspectorRepository? = null

        fun getInstance(dao: ProspectorDao) =
                instance ?: synchronized(this) {
                    instance ?: ProspectorRepository(dao)
                        .also { instance = it }
                }
    }
}