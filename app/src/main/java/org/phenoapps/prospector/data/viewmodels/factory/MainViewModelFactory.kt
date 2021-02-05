package org.phenoapps.prospector.data.viewmodels.factory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import org.phenoapps.prospector.data.viewmodels.MainActivityViewModel
import org.phenoapps.prospector.data.viewmodels.repository.ExperimentRepository
import org.phenoapps.prospector.data.viewmodels.repository.SampleRepository
import org.phenoapps.prospector.data.viewmodels.repository.ScanRepository

class MainViewModelFactory(
        private val experimentRepository: ExperimentRepository,
        private val sampleRepository: SampleRepository,
        private val repo: ScanRepository) : ViewModelProvider.Factory {

    @SuppressWarnings("UNCHECKED_CAST")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {

        return MainActivityViewModel(experimentRepository, sampleRepository, repo) as T
    }

}