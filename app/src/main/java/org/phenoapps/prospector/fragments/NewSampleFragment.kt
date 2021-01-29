package org.phenoapps.prospector.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ContextThemeWrapper
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.phenoapps.prospector.R
import org.phenoapps.prospector.data.ProspectorDatabase
import org.phenoapps.prospector.data.ProspectorRepository
import org.phenoapps.prospector.data.models.Sample
import org.phenoapps.prospector.data.viewmodels.ExperimentSamplesViewModel
import org.phenoapps.prospector.data.viewmodels.factory.ExperimentSamplesViewModelFactory
import org.phenoapps.prospector.databinding.FragmentNewSampleBinding
import org.phenoapps.prospector.utils.Dialogs

class NewSampleFragment : Fragment(), CoroutineScope by MainScope() {

    private var mExpId: Long = -1L

    private val viewModel: ExperimentSamplesViewModel by viewModels {

        ExperimentSamplesViewModelFactory(
                ProspectorRepository.getInstance(
                        ProspectorDatabase.getInstance(requireContext())
                                .expScanDao()))

    }

    private val sOnBarcodeScanClick = View.OnClickListener {

        setFragmentResultListener("BarcodeResult") { key, bundle ->

            val code = bundle.getString("barcode_result", "") ?: ""

            mBinding?.sampleNameEditText?.setText(code)

        }

        findNavController().navigate(NewSampleFragmentDirections
                .actionToBarcodeScanner())

    }

    private val sOnSaveClick = View.OnClickListener {

        mBinding?.insertSample()

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

        mBinding?.let { ui ->

            setupButtons()

        }

        setHasOptionsMenu(true)

        (activity as? AppCompatActivity)?.supportActionBar?.title = ""

        return mBinding?.root
    }

    private fun FragmentNewSampleBinding.clearUi() {

        activity?.let { act ->

            sampleNameEditText.text.clear()

            sampleNoteEditText.text.clear()

        }
    }

    private fun FragmentNewSampleBinding.insertSampleAndUpdate() {

        activity?.let { act ->

            val name = sampleNameEditText.text.toString()
            val notes = sampleNoteEditText.text.toString()

            val newSampleString: String = act.getString(R.string.dialog_new_samples_prefix)

            launch {

                viewModel.insertSample(Sample(mExpId, name, note = notes)).await()

                act.runOnUiThread {

                    mBinding?.let { ui ->

                        Snackbar.make(ui.root,
                                "$newSampleString: $name.", Snackbar.LENGTH_SHORT).show()


                        clearUi()

                        findNavController().popBackStack()

                    }
                }
            }
        }

    }

    //checks if sample name is not empty
    //also checks if the name already exists and prompts the user to navigate to that sample
    private fun FragmentNewSampleBinding.insertSample() {

        val name = sampleNameEditText.text.toString()

        val newSampleError: String = getString(R.string.dialog_new_sample_error)

        if (name.isNotBlank()) {

            launch(Dispatchers.IO) {

                //if not sample exists with that name, insert it
                if (viewModel.getSamples(mExpId).find { it.name == name } == null) {

                    insertSampleAndUpdate()

                } else { //otherwise ask the user before inserting a duplicate

                    activity?.let { act ->

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

            activity?.runOnUiThread {

                mBinding?.let { ui ->

                    Snackbar.make(ui.root,
                            newSampleError, Snackbar.LENGTH_LONG).show()
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