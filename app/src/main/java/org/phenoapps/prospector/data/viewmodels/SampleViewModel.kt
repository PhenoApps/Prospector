package org.phenoapps.prospector.data.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import org.phenoapps.prospector.data.models.Sample
import org.phenoapps.prospector.data.viewmodels.repository.ExperimentRepository
import org.phenoapps.prospector.data.viewmodels.repository.SampleRepository
import javax.inject.Inject

@HiltViewModel
class SampleViewModel @Inject constructor(
        experimentRepo: ExperimentRepository,
        private val repo: SampleRepository): ViewModel() {

    //non-live
    suspend fun getSamples(eid: Long): List<Sample> = viewModelScope.async {

        return@async repo.getSamples(eid)

    }.await()

    //live data
    fun getSamplesLive(eid: Long) = repo.getSamplesLive(eid)
    fun getSampleScanCounts(eid: Long) = repo.getSampleScanCounts(eid)
    val experiments = experimentRepo.getExperiments()
    val deviceTypeExports = repo.getDeviceTypeExports()

    suspend fun deleteSample(eid: Long, name: String) = repo.deleteSample(eid, name)

    fun insertSampleAsync(sample: Sample) = viewModelScope.async { return@async repo.insertSample(sample) }


}