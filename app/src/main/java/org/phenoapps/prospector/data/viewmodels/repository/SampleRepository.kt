package org.phenoapps.prospector.data.viewmodels.repository

import org.phenoapps.prospector.data.dao.SampleDao
import org.phenoapps.prospector.data.models.Sample
import javax.inject.Inject

class SampleRepository @Inject constructor(
            private val dao: SampleDao) {

    fun getDeviceTypeExports() = dao.getDeviceTypeExports()

    fun getSamplesLive(eid: Long) = dao.getSamplesLive(eid)

    fun getSampleScanCounts(eid: Long) = dao.getSampleScanCounts(eid)

    suspend fun getSamples(eid: Long): List<Sample> = dao.getSamples(eid)

    suspend fun deleteSample(eid: Long, name: String) = dao.deleteSample(eid, name)

    suspend fun insertSample(sample: Sample) = dao.insertSample(sample.eid, sample.name, sample.date, sample.note)

}