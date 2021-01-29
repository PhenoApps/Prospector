package org.phenoapps.prospector.data.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import org.phenoapps.prospector.data.ProspectorRepository
import org.phenoapps.prospector.data.models.Experiment
import org.phenoapps.prospector.data.models.Sample
import org.phenoapps.prospector.data.models.Scan
import org.phenoapps.prospector.data.models.SpectralFrame

class ExperimentSamplesViewModel(
        private val repo: ProspectorRepository): ViewModel() {

    val deviceTypeExports = repo.getDeviceTypeExports()
    val experiments = repo.getExperiments()
    val experimentCounts = repo.getExperimentCounts()
    val samples = repo.getSamples()
    val scans = repo.getScans()
    val frames = repo.getFrames()

    //non-live
    fun getSpectralValues(eid: Long, sid: Long): List<SpectralFrame> = repo.getSpectralValues(eid, sid)
    fun getSamples(eid: Long): List<Sample> = repo.getSamples(eid)

    //live data
    fun getSamplesLive(eid: Long) = repo.getSamplesLive(eid)
    fun getSampleScanCounts(eid: Long) = repo.getSampleScanCounts(eid)
    fun getScans(eid: Long, sample: String) = repo.getScans(eid, sample)
    fun getSpectralValuesLive(eid: Long, sid: Long) = repo.getSpectralValuesLive(eid, sid)

    suspend fun deleteScan(scan: Scan) = scan.sid?.let { id -> repo.deleteScan(id) }
    suspend fun deleteScans(eid: Long, name: String) = repo.deleteScans(eid, name)
    suspend fun deleteSample(eid: Long, name: String) = repo.deleteSample(eid, name)

//    fun getScans(eid: Long) = repo.getScans(eid)
//
//    val scans = liveData {
//
//        val data = repo.getAll()
//
//        emit(data)
//
//    }

//
//
//    fun spectralFrames(eid: Long, sid: String) = repo.spectralFrames(eid, sid)

    suspend fun updateScanColor(eid: Long, scanId: Long, color: String) = viewModelScope.launch { repo.updateScanColor(eid, scanId, color) }

    fun insertScan(scan: Scan): Deferred<Long> = viewModelScope.async { return@async repo.insertScan(scan) }

    fun insertSample(sample: Sample) = viewModelScope.async { return@async repo.insertSample(sample) }

    fun insertFrame(sid: Long, frame: SpectralFrame) = viewModelScope.launch {

        repo.insertFrame(sid, frame)

    }

    fun insertExperiment(exp: Experiment) = viewModelScope.async {

        return@async repo.insertExperiment(exp.name, exp.date)

    }

    fun deleteExperiment(eid: Long) = viewModelScope.launch {

        repo.deleteExperiment(eid)

    }

//    fun deleteScan(scan: Scan) = viewModelScope.launch {
//
//        repo.deleteScan(scan.eid, scan.sid)
//
//    }

}