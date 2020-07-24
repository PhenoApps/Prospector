package org.phenoapps.prospector.data.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.phenoapps.prospector.data.ExperimentScansRepository
import org.phenoapps.prospector.data.models.Experiment
import org.phenoapps.prospector.data.models.Scan
import org.phenoapps.prospector.data.models.SpectralFrame

class ExperimentScansViewModel(
        private val repo: ExperimentScansRepository): ViewModel() {

    val experiments = repo.getExperiments()

    fun getScans(eid: Long) = repo.getScans(eid)

    val scans = liveData {

        val data = repo.getAll()

        emit(data)

    }

    fun forceSpectralValues(eid: Long, sid: String): LiveData<List<SpectralFrame>> = repo.forceGetSpectralValues(eid, sid)

    fun getSpectralValues(eid: Long, sid: String) = repo.getSpectralValues(eid, sid)

    fun spectralFrames(eid: Long, sid: String) = repo.spectralFrames(eid, sid)

    fun insertScan(scan: Scan) = repo.insertScan(scan)

    fun insertFrame(sid: String, frame: SpectralFrame) = viewModelScope.launch {

        repo.insertFrame(sid, frame)

    }

    fun insertExperiment(exp: Experiment) = viewModelScope.launch {

        repo.insertExperiment(exp.name, exp.date)

    }

    fun deleteExperiment(eid: Long) = viewModelScope.launch {

        repo.deleteExperiment(eid)

    }

    fun deleteScan(scan: Scan) = viewModelScope.launch {

        repo.deleteScan(scan.eid, scan.sid)

    }

}