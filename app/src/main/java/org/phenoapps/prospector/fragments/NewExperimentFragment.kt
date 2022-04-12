package org.phenoapps.prospector.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.appcompat.view.ContextThemeWrapper
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.WithFragmentBindings
import kotlinx.coroutines.*
import org.phenoapps.prospector.R
import org.phenoapps.prospector.activities.MainActivity
import org.phenoapps.prospector.data.models.Experiment
import org.phenoapps.prospector.data.viewmodels.ExperimentViewModel
import org.phenoapps.prospector.data.viewmodels.devices.InnoSpectraViewModel
import org.phenoapps.prospector.databinding.FragmentNewExperimentBinding

/**
 * A simple data collection fragment that creates experiment models and inserts them into the db.
 */
@WithFragmentBindings
@AndroidEntryPoint
class NewExperimentFragment : Fragment(), CoroutineScope by MainScope() {

    private enum class DeviceIndex(value: Int) {
        LSNIR(0), LS(1), NANO(2)
    }

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

        mBinding?.deviceTypeSpinner?.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                mBinding?.configSpinner?.visibility = if (position == DeviceIndex.NANO.ordinal) {
                    setupScanConfigs()
                    View.VISIBLE
                } else View.GONE
                mBinding?.fragNewExpLoadingTv?.visibility = if (position == DeviceIndex.NANO.ordinal) {
                    View.VISIBLE
                } else View.GONE
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}

        }

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

        val configName = configSpinner.selectedItem as? String

        if (experimentName.isNotBlank()) {

            launch {

                sViewModel.insertExperimentAsync(Experiment(experimentName,
                    deviceType = deviceType, note = experimentNotes, config = configName)).await()

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

    override fun onResume() {
        super.onResume()

        (activity as? MainActivity)?.setToolbar(R.id.action_nav_data)

    }

    private fun setupScanConfigs() {

        activity?.let { act ->

            mBinding?.fragNewExpLoadingTv?.visibility = View.VISIBLE
            mBinding?.fragNewExpPb?.visibility = View.VISIBLE
            mBinding?.newExperimentSaveButton?.isEnabled = false

            var deviceViewModel = (act as MainActivity).sDeviceViewModel

            if (deviceViewModel !is InnoSpectraViewModel) {
                act.switchInnoSpectra()
                deviceViewModel = act.sDeviceViewModel as InnoSpectraViewModel
            }

            launch {

                var size = deviceViewModel.getScanConfigSize()
                var current = 0
                while (size == -1
                    || deviceViewModel.getScanConfigs().size != size
                    || deviceViewModel.getActiveConfig() == null) {
                    size = deviceViewModel.getScanConfigSize()
                    current = deviceViewModel.getScanConfigs().size

                    act.runOnUiThread {
                        mBinding?.fragNewExpPb?.max = size
                        mBinding?.fragNewExpPb?.progress = current
                    }

                    delay(1000)
                }

                act.runOnUiThread {

                    mBinding?.fragNewExpLoadingTv?.visibility = View.GONE
                    mBinding?.fragNewExpPb?.visibility = View.GONE
                    mBinding?.newExperimentSaveButton?.isEnabled = true

                    val configs = deviceViewModel.getScanConfigs()

                    if (configs.isNotEmpty()) {

                        val items = configs.map { it.configName }

                        mBinding?.configSpinner?.adapter = ArrayAdapter(act, android.R.layout.simple_spinner_item, items)

                        mBinding?.configSpinner?.invalidate()
                    }
                }
            }
        }
    }
}