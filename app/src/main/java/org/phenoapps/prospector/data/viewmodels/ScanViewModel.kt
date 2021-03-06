package org.phenoapps.prospector.data.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import org.phenoapps.prospector.data.models.Scan
import org.phenoapps.prospector.data.models.SpectralFrame
import org.phenoapps.prospector.data.viewmodels.repository.ExperimentRepository
import org.phenoapps.prospector.data.viewmodels.repository.ScanRepository
import javax.inject.Inject

@HiltViewModel
class ScanViewModel @Inject constructor(
        experimentRepo: ExperimentRepository,
        private val repo: ScanRepository): ViewModel() {

    val experiments = experimentRepo.getExperiments()

    //non-live
    fun getSpectralValues(eid: Long, sid: Long): List<SpectralFrame> = repo.getSpectralValues(eid, sid)

    //live data
    fun getScans(eid: Long, sample: String) = repo.getScans(eid, sample)
    fun getSpectralValuesLive(eid: Long, sid: Long) = repo.getSpectralValuesLive(eid, sid)

    suspend fun deleteScan(scan: Scan) = scan.sid?.let { id -> repo.deleteScan(id) }

    suspend fun deleteScans(eid: Long, name: String) = repo.deleteScans(eid, name)

    suspend fun updateScanColor(eid: Long, scanId: Long, color: String) = viewModelScope.launch { repo.updateScanColor(eid, scanId, color) }

    fun insertScanAsync(scan: Scan): Deferred<Long> = viewModelScope.async { return@async repo.insertScan(scan) }

    fun insertFrame(sid: Long, frame: SpectralFrame) = viewModelScope.launch {

        repo.insertFrame(sid, frame)

    }

}