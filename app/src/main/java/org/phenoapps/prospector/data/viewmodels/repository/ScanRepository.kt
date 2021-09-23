package org.phenoapps.prospector.data.viewmodels.repository

import org.phenoapps.prospector.data.dao.ScanDao
import org.phenoapps.prospector.data.models.Scan
import org.phenoapps.prospector.data.models.SpectralFrame
import javax.inject.Inject

class ScanRepository @Inject constructor(
            private val dao: ScanDao) {

    fun getSpectralValues(eid: Long, sid: Long): List<SpectralFrame> = dao.getSpectralValues(eid, sid)

    fun getSpectralValues(eid: Long, sample: String) = dao.getSpectralValues(eid, sample)

    fun getSpectralValuesLive(eid: Long, sid: Long)= dao.getSpectralValuesLive(eid, sid)

    fun getScans(eid: Long, sid: String) = dao.getScans(eid, sid)

    suspend fun deleteScan(sid: Long) = dao.deleteScan(sid)

    suspend fun deleteScans(eid: Long, name: String) = dao.deleteScans(eid, name)

    suspend fun insertFrame(sid: Long, frame: SpectralFrame) = dao.insertFrame(sid, frame.frameId, frame.spectralValues, frame.lightSource)

    suspend fun insertScan(scan: Scan): Long = dao.insertScan(scan.eid, scan.name, scan.date, scan.deviceId ?: "", scan.operator ?: "", scan.deviceType, scan.lightSource ?: -1)

    fun updateScanColor(eid: Long, scanId: Long, color: String) = dao.updateScanColor(eid, scanId, color)

}