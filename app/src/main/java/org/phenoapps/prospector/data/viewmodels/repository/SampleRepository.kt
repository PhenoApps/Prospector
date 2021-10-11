package org.phenoapps.prospector.data.viewmodels.repository

import org.phenoapps.prospector.data.dao.SampleDao
import org.phenoapps.prospector.data.models.Sample
import javax.inject.Inject

class SampleRepository @Inject constructor(
            private val dao: SampleDao) {

    fun getDeviceTypeExports(eid: Long) = dao.getDeviceTypeExports(eid)

    fun getSamplesLive(eid: Long) = dao.getSamplesLive(eid)

    fun getSampleFramesCount(eid: Long) = dao.getSampleFramesCount(eid)

    suspend fun deleteSample(eid: Long, name: String) = dao.deleteSample(eid, name)

    suspend fun insertSample(sample: Sample) = dao.insertSample(sample.eid, sample.name, sample.date, sample.note)

    suspend fun update(eid: Long, oldName: String, name: String, note: String) = dao.update(eid, oldName, name, note)
}