package org.phenoapps.prospector.data.viewmodels.factory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import org.phenoapps.prospector.data.ExperimentScansRepository
import org.phenoapps.prospector.data.viewmodels.ExperimentScansViewModel

class ExperimentScanViewModelFactory(
        private val repo: ExperimentScansRepository) : ViewModelProvider.Factory {

    @SuppressWarnings("UNCHECKED_CAST")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {

        return ExperimentScansViewModel(repo) as T
    }

}