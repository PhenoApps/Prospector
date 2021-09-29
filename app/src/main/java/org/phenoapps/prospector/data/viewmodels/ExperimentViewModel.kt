package org.phenoapps.prospector.data.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import org.phenoapps.prospector.data.models.Experiment
import org.phenoapps.prospector.data.viewmodels.repository.ExperimentRepository
import javax.inject.Inject

@HiltViewModel
class ExperimentViewModel @Inject constructor(
    private val repo: ExperimentRepository): ViewModel() {

    fun getExperimentCounts() = repo.getExperimentCounts()

    fun insertExperimentAsync(exp: Experiment) = viewModelScope.async {

        return@async repo.insertExperiment(exp.name, exp.deviceType, exp.date)

    }

    fun deleteExperiment(eid: Long) = viewModelScope.launch {

        repo.deleteExperiment(eid)

    }

}