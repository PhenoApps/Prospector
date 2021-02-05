package org.phenoapps.prospector.data.viewmodels.factory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import org.phenoapps.prospector.data.viewmodels.SampleViewModel
import org.phenoapps.prospector.data.viewmodels.repository.ExperimentRepository
import org.phenoapps.prospector.data.viewmodels.repository.SampleRepository

class SampleViewModelFactory(
        private val expRepo: ExperimentRepository,
        private val repo: SampleRepository) : ViewModelProvider.Factory {

    @SuppressWarnings("UNCHECKED_CAST")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {

        return SampleViewModel(expRepo, repo) as T
    }

}