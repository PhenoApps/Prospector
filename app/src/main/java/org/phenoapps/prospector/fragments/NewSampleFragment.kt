package org.phenoapps.prospector.fragments

import android.os.Bundle
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
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.WithFragmentBindings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.phenoapps.prospector.R
import org.phenoapps.prospector.data.models.Sample
import org.phenoapps.prospector.data.viewmodels.SampleViewModel
import org.phenoapps.prospector.databinding.FragmentNewSampleBinding
import org.phenoapps.prospector.utils.Dialogs
import org.phenoapps.prospector.utils.SnackbarQueue

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

    private val sOnSaveClick = View.OnClickListener {

        launch {

            mBinding?.insertSample()

        }

    }

    private val sOnCancelClick = View.OnClickListener {

        findNavController().popBackStack()

    }

    private var mBinding: FragmentNewSampleBinding? = null

    private val mSnackbar = SnackbarQueue()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        mExpId = arguments?.getLong("experiment", -1L) ?: -1L

        val contextThemeWrapper = ContextThemeWrapper(activity, R.style.AppTheme)

        val localInflater = inflater.cloneInContext(contextThemeWrapper)

        mBinding = DataBindingUtil.inflate(localInflater, R.layout.fragment_new_sample, null, true)

        mBinding?.let { it ->

            setupButtons()

            //possible arguments sent if the sample is being edited
            argUpdateName?.let { oldName ->
                it.sampleNameEditText.setText(oldName)
            }

            argUpdateNote?.let { oldNote ->
                it.sampleNoteEditText.setText(oldNote)
            }

        }

        setHasOptionsMenu(true)

//        (activity as? AppCompatActivity)?.findViewById<Toolbar>(R.id.activityToolbar)?.title = ""

        return mBinding?.root
    }

    private fun FragmentNewSampleBinding.clearUi() {

        activity?.let { _ ->

            sampleNameEditText.text.clear()

            sampleNoteEditText.text.clear()

        }
    }

    private fun FragmentNewSampleBinding.insertSampleAndUpdate() {

        activity?.let { act ->

            val name = sampleNameEditText.text.toString()
            val notes = sampleNoteEditText.text.toString()

            if (argUpdateName != null) { //this is an update call

                launch(Dispatchers.IO) {

                    sViewModel.update(mExpId, argUpdateName ?: name, name, notes)

                }

            } else {

                val newSampleString: String = act.getString(R.string.dialog_new_samples_prefix)

                launch {

                    sViewModel.insertSampleAsync(Sample(mExpId, name, note = notes)).await()

                }

                activity?.runOnUiThread {

                    mSnackbar.push(SnackbarQueue.SnackJob(root, "$newSampleString $name."))

                    clearUi()

                    findNavController().popBackStack()
                }
            }
        }
    }

    //checks if sample name is not empty
    //also checks if the name already exists and prompts the user to navigate to that sample
    private suspend fun FragmentNewSampleBinding.insertSample() {

        val name = sampleNameEditText.text.toString()

        val newSampleError: String = getString(R.string.dialog_new_sample_error)

        //ensure we have a context
        activity?.let { act ->

            //ensure the name is given
            if (name.isNotBlank()) {

                //if not sample exists with that name, insert it
                if (sViewModel.getSamples(mExpId).find { it.name == name } == null || argUpdateName == name) {

                    insertSampleAndUpdate()

                    act.runOnUiThread {

                        findNavController()
                                .navigate(NewSampleFragmentDirections
                                        .actionToScanList(mExpId, name))
                    }

                } else { //otherwise ask the user before inserting a duplicate

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

            } else {

                act.runOnUiThread {

                    mBinding?.let { ui ->

                        Snackbar.make(ui.root,
                                newSampleError, Snackbar.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    private fun setupButtons() {

        mBinding?.onSaveClick = sOnSaveClick

        mBinding?.onCancelClick = sOnCancelClick

        mBinding?.onBarcodeScanClick = sOnBarcodeScanClick
    }
}