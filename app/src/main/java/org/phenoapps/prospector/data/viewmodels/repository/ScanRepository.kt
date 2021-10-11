package org.phenoapps.prospector.data.viewmodels.repository

import org.phenoapps.prospector.data.dao.ScanDao
import org.phenoapps.prospector.data.models.Scan
import org.phenoapps.prospector.data.models.SpectralFrame
import javax.inject.Inject

class ScanRepository @Inject constructor(
            private val dao: ScanDao) {

    fun getSpectralValues(eid: Long, sid: Long, fid: Int): List<SpectralFrame> = dao.getSpectralValues(eid, sid, fid)

    fun getSpectralValues(eid: Long, sample: String, lightSource: Int) = dao.getSpectralValues(eid, sample, lightSource)

    fun getSpectralValues(eid: Long, sample: String) = dao.getSpectralValues(eid, sample)

    fun getSpectralValuesLive(eid: Long, sid: Long)= dao.getSpectralValuesLive(eid, sid)

    fun getFrames(eid: Long, sid: String) = dao.getFrames(eid, sid)

    suspend fun deleteScan(sid: Long) = dao.deleteScan(sid)

    suspend fun deleteFrame(sid: Long, fid: Int) = dao.deleteFrame(sid, fid)

    suspend fun deleteScans(eid: Long, name: String) = dao.deleteScans(eid, name)

    suspend fun insertFrame(sid: Long, frame: SpectralFrame) = dao.insertFrame(sid, frame.frameId, frame.spectralValues, frame.lightSource)

    suspend fun insertScan(scan: Scan): Long = dao.insertScan(scan.eid, scan.name, scan.date, scan.deviceId ?: "", scan.operator ?: "", scan.deviceType, scan.lightSource ?: -1)

    suspend fun updateFrameColor(sid: Long, fid: Int, color: String) = dao.updateFrameColor(sid, fid, color)

}