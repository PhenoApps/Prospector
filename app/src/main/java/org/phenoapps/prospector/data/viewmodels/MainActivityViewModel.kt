package org.phenoapps.prospector.data.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import org.phenoapps.prospector.data.models.Experiment
import org.phenoapps.prospector.data.models.Sample
import org.phenoapps.prospector.data.models.Scan
import org.phenoapps.prospector.data.models.SpectralFrame
import org.phenoapps.prospector.data.viewmodels.repository.ExperimentRepository
import org.phenoapps.prospector.data.viewmodels.repository.SampleRepository
import org.phenoapps.prospector.data.viewmodels.repository.ScanRepository
import javax.inject.Inject

/**
 * required repos/functions used for loading sample data
 */
@HiltViewModel
class MainActivityViewModel @Inject constructor(
    private val experimentRepo: ExperimentRepository,
    private val sampleRepo: SampleRepository,
    private val scanRepo: ScanRepository): ViewModel() {

    fun insertExperimentAsync(exp: Experiment) = viewModelScope.async {

        return@async experimentRepo.insertExperiment(exp.name, exp.deviceType, exp.date, exp.config)

    }

    fun insertSampleAsync(sample: Sample) = viewModelScope.async { return@async sampleRepo.insertSample(sample) }

    fun insertScanAsync(scan: Scan): Deferred<Long> = viewModelScope.async { return@async scanRepo.insertScan(scan) }

    fun insertFrame(sid: Long, frame: SpectralFrame) = viewModelScope.launch {

        scanRepo.insertFrame(sid, frame)

    }

}