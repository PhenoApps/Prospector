package org.phenoapps.prospector.data.viewmodels.repository

import org.phenoapps.prospector.data.dao.ExperimentDao
import org.phenoapps.prospector.utils.DateUtil

class ExperimentRepository
    private constructor(
            private val dao: ExperimentDao) {

    fun getExperiments() = dao.getExperiments()

    fun getExperimentCounts() = dao.getExperimentCounts()

    suspend fun deleteExperiment(eid: Long) = dao.deleteExperiment(eid)

    suspend fun insertExperiment(name: String, deviceType: String, date: String): Long {

        var actualDate = date

        if (actualDate.isBlank()) {

            actualDate = DateUtil().getTime()

        }

        return dao.insertExperiment(name, deviceType, actualDate)

    }

    companion object {

        @Volatile private var instance: ExperimentRepository? = null

        fun getInstance(dao: ExperimentDao) =
                instance ?: synchronized(this) {
                    instance ?: ExperimentRepository(dao)
                        .also { instance = it }
                }
    }
}