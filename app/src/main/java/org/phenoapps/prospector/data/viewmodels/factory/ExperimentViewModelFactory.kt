package org.phenoapps.prospector.data.viewmodels.factory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import org.phenoapps.prospector.data.viewmodels.ExperimentViewModel
import org.phenoapps.prospector.data.viewmodels.repository.ExperimentRepository

class ExperimentViewModelFactory(
        private val repo: ExperimentRepository) : ViewModelProvider.Factory {

    @SuppressWarnings("UNCHECKED_CAST")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {

        return ExperimentViewModel(repo) as T
    }

}