package org.phenoapps.prospector.data.viewmodels.factory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import org.phenoapps.prospector.data.ProspectorRepository
import org.phenoapps.prospector.data.viewmodels.ExperimentSamplesViewModel

class ExperimentSamplesViewModelFactory(
        private val repo: ProspectorRepository) : ViewModelProvider.Factory {

    @SuppressWarnings("UNCHECKED_CAST")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {

        return ExperimentSamplesViewModel(repo) as T
    }

}