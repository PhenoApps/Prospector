package org.phenoapps.prospector.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.view.ContextThemeWrapper
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.WithFragmentBindings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.phenoapps.prospector.R
import org.phenoapps.prospector.activities.MainActivity
import org.phenoapps.prospector.data.models.Sample
import org.phenoapps.prospector.data.viewmodels.SampleViewModel
import org.phenoapps.prospector.databinding.FragmentNewSampleBinding
import org.phenoapps.prospector.utils.Dialogs
import org.phenoapps.prospector.utils.SnackbarQueue
import org.phenoapps.prospector.utils.observeOnce
import java.lang.IllegalStateException
import java.util.*

/**
 * A simple data collection fragment that creates sample models and inserts them into the db.
 * Barcodes can be scanned using the BarcodeScanFragment, which is used to populate the sample name.
 */
@WithFragmentBindings
@AndroidEntryPoint
class NewSampleFragment : Fragment(), CoroutineScope by MainScope() {

    private var mExpId: Long = -1L

    private val sViewModel: SampleViewModel by viewModels()

    private val argUpdateName by lazy {
        arguments?.getString("name")
    }

    private val argUpdateNote by lazy {
        arguments?.getString("note")
    }

    private val sOnBarcodeScanClick = View.OnClickListener {

        setFragmentResultListener("BarcodeResult") { _, bundle ->

            val code = bundle.getString("barcode_result", "") ?: ""

            mBinding?.sampleNameEditText?.setText(code)

        }

        findNavController().navigate(NewSampleFragmentDirections
                .actionToBarcodeScanner())

    }


    /**
     * When the save button is clicked check if we are updating an old sample or inserting a new one.
     */
    private val sOnSaveClick = View.OnClickListener {

        mBinding?.checkInsert()

    }

    private val sOnCancelClick = View.OnClickListener {

        findNavController().popBackStack()

    }

    private var mBinding: FragmentNewSampleBinding? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        mExpId = arguments?.getLong("experiment", -1L) ?: -1L

        val contextThemeWrapper = ContextThemeWrapper(activity, R.style.AppTheme)

        val localInflater = inflater.cloneInContext(contextThemeWrapper)

        mBinding = DataBindingUtil.inflate(localInflater, R.layout.fragment_new_sample, null, true)

        mBinding?.let { binding ->

            setupButtons()

            binding.setupToolbar()

            //possible arguments sent if the sample is being edited
            argUpdateName?.let { oldName ->
                binding.sampleNameEditText.setText(oldName)
            }

            argUpdateNote?.let { oldNote ->
                binding.sampleNoteEditText.setText(oldNote)
            }

            startObservers()
        }

        setHasOptionsMenu(true)

        return mBinding?.root
    }

    private fun FragmentNewSampleBinding.clearUi() {

        activity?.let { _ ->

            sampleNameEditText.text.clear()

            sampleNoteEditText.text.clear()

        }
    }

    /**
     * LinkSquare API live data listener that responds to on-device button clicks.
     */
    private fun attachDeviceButtonPressListener() {

        try {

            with (activity as? MainActivity) {

                this?.sDeviceViewModel?.setEventListener {

                    activity?.runOnUiThread {

                        if (sDeviceViewModel.isConnected()) {

                            mBinding?.checkInsert(true)

                        }
                    }

                }?.observe(viewLifecycleOwner, {})
            }

        } catch (e: IllegalStateException) {

            Log.d(tag, "Failed to connect LS")
        }
    }

    /**
     * Uses the ui to check if the insert is valid and not a duplicate
     * Used when a device button is pressed or the UI submit button is pressed.
     * Start scan will naviate to the scan list when the sample is inserted.
     */
    private fun FragmentNewSampleBinding.checkInsert(startScan: Boolean = false) {

        activity?.let { act ->

            val name = sampleNameEditText.text.toString()
            val notes = sampleNoteEditText.text.toString()

            //updated and new names should never be blank
            if (name.isNotBlank()) {

                //this is an argument passed to the fragment if it is null then we aren't updating
                val oldName = argUpdateName ?: ""

                //next check if the name would be a duplicate if created
                //don't allow updating to a duplicated name
                //if this happens then a dialog is run which will ask to reset the ui or
                // go to the scan list for the duplicated sample name
                sViewModel.getSamplesLive(mExpId).observeOnce(viewLifecycleOwner) { data ->

                    val sample = data.find { it.name == name }

                    //insert the sample if we can't find a duplicate and we aren't updating
                    if (sample == null && oldName.isBlank()) {

                        insertSample(name, notes, startScan)

                    } else { //otherwise check for update and ask the user about duplicate

                        if (oldName.isNotBlank()) {

                            //check that the new name doesn't match any other sample names
                            //if the sample exists with the same name this is a note update
                            if (sample == null || sample.name == oldName) {

                                updateSample(name, notes)

                            } else {

                                (act as MainActivity).notify(getString(R.string.dialog_new_sample_error))
                            }

                        } else { //found duplicate on insert

                            act.runOnUiThread {

                                Dialogs.booleanOption(AlertDialog.Builder(act),
                                    act.getString(R.string.frag_new_sample_dialog_duplicate_title),
                                    act.getString(R.string.frag_new_sample_dialog_duplicate_message),
                                    act.getString(R.string.frag_new_sample_dialog_add_to_sample),
                                    act.getString(R.string.frag_new_sample_dialog_new_sample)) {

                                    if (it) {

                                        findNavController()
                                            .navigate(NewSampleFragmentDirections
                                                .actionToScanList(mExpId, name))

                                    } else {

                                        clearUi()

                                    }
                                }
                            }
                        }
                    }
                }

            } else {

                (act as MainActivity).notify(getString(R.string.dialog_new_sample_error))

            }
        }
    }

    private fun startObservers() {

        //use the activity view model to access the current connection status
        val check = object : TimerTask() {

            override fun run() {

                activity?.runOnUiThread {

                    with (activity as? MainActivity) {
                        mBinding?.fragNewSampleToolbar?.menu?.findItem(R.id.action_connection)
                            ?.setIcon(if (this?.sDeviceViewModel?.isConnected() == true){
                                attachDeviceButtonPressListener()
                                R.drawable.ic_vector_link
                            }
                            else R.drawable.ic_vector_difference_ab)
                    }
                }
            }
        }

        Timer().cancel()

        Timer().purge()

        Timer().scheduleAtFixedRate(check, 0, 1500)

        //set the title header
        sViewModel.experiments.observe(viewLifecycleOwner, { experiments ->

            experiments.first { it.eid == mExpId }.also {

                activity?.runOnUiThread {

                    mBinding?.fragNewSampleToolbar?.title = it.name

                }
            }
        })

        attachDeviceButtonPressListener()
    }

    private fun FragmentNewSampleBinding.setupToolbar() {

        fragNewSampleToolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }

        fragNewSampleToolbar.setOnMenuItemClickListener {
            when (it.itemId) {

                R.id.action_connection -> {

                    with (activity as? MainActivity) {

                        if (this?.mConnected == true) {

                            sDeviceViewModel.reset()
                        } else {

                            findNavController().navigate(
                                NewSampleFragmentDirections
                                .actionToConnectInstructions())

                            this?.startDeviceConnection()
                        }
                    }
                }
            }

            true
        }
    }

    /**
     * Function that inserts a new sample into the database.
     * If the start scan parameter is true then automatically navigate to the scan list and
     * use extras to indicate we are starting a scan.
     */
    private fun insertSample(name: String, notes: String, startScan: Boolean) {

        activity?.let { act ->

            mBinding?.let { ui ->

                val newSampleString: String = act.getString(R.string.dialog_new_samples_prefix)

                launch (Dispatchers.IO) {

                    sViewModel.insertSampleAsync(Sample(mExpId, name, note = notes)).await()

                    act.runOnUiThread {

                        (act as MainActivity).notify("$newSampleString $name")

                        if (startScan) {

                            findNavController().navigate(NewSampleFragmentDirections
                                .actionToScanList(mExpId, name, startScan))

                        } else findNavController().navigate(NewSampleFragmentDirections
                            .actionToScanList(mExpId, name))
                    }
                }
            }
        }
    }

    /**
     * Update the sample in the database and return to the previous fragment.
     */
    private fun updateSample(name: String, notes: String) {

        launch(Dispatchers.IO) {

            sViewModel.update(mExpId, argUpdateName ?: name, name, notes)

            activity?.runOnUiThread {

                findNavController().popBackStack()
            }
        }
    }

    private fun setupButtons() {

        mBinding?.onSaveClick = sOnSaveClick

        mBinding?.onCancelClick = sOnCancelClick

        mBinding?.onBarcodeScanClick = sOnBarcodeScanClick
    }
}