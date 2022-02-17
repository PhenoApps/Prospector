package org.phenoapps.prospector.data.viewmodels.repository

import org.phenoapps.prospector.data.dao.ExperimentDao
import org.phenoapps.prospector.utils.DateUtil
import javax.inject.Inject

class ExperimentRepository @Inject constructor(
    private val dao: ExperimentDao) {

    fun getExperiments() = dao.getExperiments()

    fun getExperimentCounts() = dao.getExperimentCounts()

    suspend fun deleteExperiment(eid: Long) = dao.deleteExperiment(eid)

    suspend fun insertExperiment(name: String, deviceType: String, date: String, config: String? = null): Long {

        var actualDate = date

        if (actualDate.isBlank()) {

            actualDate = DateUtil().getTime()

        }

        return dao.insertExperiment(name, deviceType, actualDate, config)

    }

}