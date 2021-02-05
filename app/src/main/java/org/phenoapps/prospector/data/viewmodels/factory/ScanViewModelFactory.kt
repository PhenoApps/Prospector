package org.phenoapps.prospector.data.viewmodels.factory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import org.phenoapps.prospector.data.viewmodels.ScanViewModel
import org.phenoapps.prospector.data.viewmodels.repository.ExperimentRepository
import org.phenoapps.prospector.data.viewmodels.repository.ScanRepository

class ScanViewModelFactory(
        private val experimentRepository: ExperimentRepository,
        private val repo: ScanRepository) : ViewModelProvider.Factory {

    @SuppressWarnings("UNCHECKED_CAST")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {

        return ScanViewModel(experimentRepository, repo) as T
    }

}