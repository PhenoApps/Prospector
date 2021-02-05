package org.phenoapps.prospector.data.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.async
import org.phenoapps.prospector.data.models.Sample
import org.phenoapps.prospector.data.viewmodels.repository.ExperimentRepository
import org.phenoapps.prospector.data.viewmodels.repository.SampleRepository

class SampleViewModel(
        experimentRepo: ExperimentRepository,
        private val repo: SampleRepository): ViewModel() {

    //non-live
    fun getSamples(eid: Long): List<Sample> = repo.getSamples(eid)

    //live data
    fun getSamplesLive(eid: Long) = repo.getSamplesLive(eid)
    fun getSampleScanCounts(eid: Long) = repo.getSampleScanCounts(eid)
    val experiments = experimentRepo.getExperiments()
    val deviceTypeExports = repo.getDeviceTypeExports()

    suspend fun deleteSample(eid: Long, name: String) = repo.deleteSample(eid, name)

    fun insertSampleAsync(sample: Sample) = viewModelScope.async { return@async repo.insertSample(sample) }


}