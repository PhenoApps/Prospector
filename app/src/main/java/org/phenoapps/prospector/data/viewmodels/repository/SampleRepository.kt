package org.phenoapps.prospector.data.viewmodels.repository

import org.phenoapps.prospector.data.dao.SampleDao
import org.phenoapps.prospector.data.models.Sample

class SampleRepository
    private constructor(
            private val dao: SampleDao) {

    fun getDeviceTypeExports() = dao.getDeviceTypeExports()

    fun getSamplesLive(eid: Long) = dao.getSamplesLive(eid)

    fun getSampleScanCounts(eid: Long) = dao.getSampleScanCounts(eid)

    fun getSamples(eid: Long): List<Sample> = dao.getSamples(eid)

    suspend fun deleteSample(eid: Long, name: String) = dao.deleteSample(eid, name)

    suspend fun insertSample(sample: Sample) = dao.insertSample(sample.eid, sample.name, sample.date, sample.note)

    companion object {

        @Volatile private var instance: SampleRepository? = null

        fun getInstance(dao: SampleDao) =
                instance ?: synchronized(this) {
                    instance ?: SampleRepository(dao)
                        .also { instance = it }
                }
    }
}