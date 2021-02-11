package org.phenoapps.prospector.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.view.ContextThemeWrapper
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.WithFragmentBindings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.phenoapps.prospector.R
import org.phenoapps.prospector.data.models.Experiment
import org.phenoapps.prospector.data.viewmodels.ExperimentViewModel
import org.phenoapps.prospector.databinding.FragmentNewExperimentBinding

/**
 * A simple data collection fragment that creates experiment models and inserts them into the db.
 */
@WithFragmentBindings
@AndroidEntryPoint
class NewExperimentFragment : Fragment(), CoroutineScope by MainScope() {

    private val sViewModel: ExperimentViewModel by viewModels()

    private val sOnNewExpClick = View.OnClickListener {

        mBinding?.insertExperiment()

    }

    private val sOnCancelClick = View.OnClickListener {

        findNavController().popBackStack()
    }

    private var mBinding: FragmentNewExperimentBinding? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        val contextThemeWrapper = ContextThemeWrapper(activity, R.style.AppTheme)

        val localInflater = inflater.cloneInContext(contextThemeWrapper)

        mBinding = DataBindingUtil.inflate(localInflater, R.layout.fragment_new_experiment, null, false)

        setupButtons()

        setHasOptionsMenu(true)

        return mBinding?.root
    }

    /*
    Loads experiment name and notes data from UI and uses coroutines to inesrt new data.
    Shows an error if the name is not entered.
     */
    private fun FragmentNewExperimentBinding.insertExperiment() {

        val experimentName = experimentNameEditText.text.toString()
        val experimentNotes = experimentNoteEditText.text.toString()
        val deviceType = deviceTypeSpinner.selectedItem as? String ?: ""

        val newExpString: String = getString(R.string.dialog_new_experiment_prefix)
        val newExpError: String = getString(R.string.dialog_new_experiment_error)

        if (experimentName.isNotBlank()) {

            launch {

                sViewModel.insertExperimentAsync(Experiment(experimentName,
                        deviceType = deviceType, note = experimentNotes)).await()

                activity?.runOnUiThread {

                    mBinding?.let { ui ->

                        Snackbar.make(ui.root,
                                "$newExpString $experimentName.", Snackbar.LENGTH_SHORT).show()

                        experimentNameEditText.text.clear()

                        experimentNoteEditText.text.clear()

                        findNavController().popBackStack()

                    }
                }
            }

        } else {

            displayToast(newExpError)

        }
    }

    private fun displayToast(text: String) {

        activity?.runOnUiThread {

            mBinding?.let { ui ->

                Snackbar.make(ui.root, text, Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupButtons() {

        mBinding?.onSaveClick = sOnNewExpClick

        mBinding?.onCancelClick = sOnCancelClick

    }
}