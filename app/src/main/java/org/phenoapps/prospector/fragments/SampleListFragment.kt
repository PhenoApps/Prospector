package org.phenoapps.prospector.fragments

import ALPHA_ASC
import ALPHA_DESC
import DATE_ASC
import DATE_DESC
import android.os.Bundle
import android.os.Handler
import android.view.*
import androidx.appcompat.view.ContextThemeWrapper
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.observe
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.*
import org.phenoapps.prospector.R
import org.phenoapps.prospector.adapter.SampleAdapter
import org.phenoapps.prospector.data.ProspectorDatabase
import org.phenoapps.prospector.data.ProspectorRepository
import org.phenoapps.prospector.data.models.Sample
import org.phenoapps.prospector.data.viewmodels.ExperimentSamplesViewModel
import org.phenoapps.prospector.data.viewmodels.factory.ExperimentSamplesViewModelFactory
import org.phenoapps.prospector.databinding.FragmentSampleListBinding
import org.phenoapps.prospector.utils.DateUtil
import org.phenoapps.prospector.utils.Dialogs
import org.phenoapps.prospector.utils.SnackbarQueue

class SampleListFragment : Fragment(), CoroutineScope by MainScope() {

    private var mSortState = DATE_DESC

    private val sSnackbarQueue = SnackbarQueue()

    private var mExpId: Long = -1L

    private val sSamples: ExperimentSamplesViewModel by viewModels {

        ExperimentSamplesViewModelFactory(
                ProspectorRepository.getInstance(
                        ProspectorDatabase.getInstance(requireContext())
                                .expScanDao()))

    }

    private var mBinding: FragmentSampleListBinding? = null

    private val sOnClickListener = View.OnClickListener {

        callInsertDialog()

    }

    override fun onDestroyView() {

        coroutineContext.cancel()

        super.onDestroyView()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        val eid = arguments?.getLong("experiment", -1L) ?: -1L

        if (eid != -1L) {

            setHasOptionsMenu(true)

            mExpId = eid

            val contextThemeWrapper = ContextThemeWrapper(activity, R.style.AppTheme)

            val localInflater = inflater.cloneInContext(contextThemeWrapper)

            mBinding = DataBindingUtil.inflate(localInflater, R.layout.fragment_sample_list, container, false)

            mBinding?.let { ui ->

                setFragmentResultListener("BarcodeResult") { key, bundle ->

                    ui.onClick = sOnClickListener

                    val code = bundle.getString("barcode_result", "") ?: ""

                    if (code.isNotBlank()) {

                        ui.editText.setText(code)

                    }

                    ui.executePendingBindings()
                }

                ui.setupRecyclerView()

                ui.setupButtons()

                updateUi()

                return ui.root
            }
        }

        return null
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {

        inflater.inflate(R.menu.menu_sample_list, menu)

        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        when(item.itemId) {

            R.id.menu_scan_code -> {

                findNavController().navigate(SampleListFragmentDirections
                        .actionToBarcodeScan())

            }

            R.id.menu_search_sample -> {

                findNavController().navigate(SampleListFragmentDirections
                        .actionToBarcodeSearch(mExpId))

            }

            R.id.menu_sort_sample -> {

                item.setIcon(when (mSortState) {

                    ALPHA_ASC -> {

                        mSortState = DATE_DESC

                        org.phenoapps.icons.R.drawable.ic_sort_by_date_descending_18dp

                    }

                    ALPHA_DESC -> {

                        mSortState = ALPHA_ASC

                        org.phenoapps.icons.R.drawable.ic_sort_by_alpha_white_ascending_18dp

                    }

                    DATE_ASC -> {

                        mSortState = ALPHA_DESC

                        org.phenoapps.icons.R.drawable.ic_sort_by_alpha_white_descending_18dp

                    }

                    else -> {

                        mSortState = DATE_ASC

                        org.phenoapps.icons.R.drawable.ic_sort_by_date_ascending_18dp

                    }
                })

                updateUi()
            }
        }

        return super.onOptionsItemSelected(item)
    }

    private fun FragmentSampleListBinding.setupButtons() {

        onClick = sOnClickListener

    }

    private suspend fun insertSample(sample: String, note: String) = withContext(Dispatchers.IO) {

        sSamples.insertSample(Sample(mExpId, sample).apply {

            this.date = DateUtil().getTime()

            this.note = note

        }).await()

    }

    private fun callInsertDialog() {

        val sample = mBinding?.editText?.text?.toString() ?: ""
        val note = mBinding?.noteText?.text?.toString() ?: ""

        if (sample.isNotBlank()) {

            CoroutineScope(Dispatchers.IO).launch {

                insertSample(sample, note)

            }

            mBinding?.editText?.setText("")
            mBinding?.noteText?.setText("")

            findNavController().navigate(SampleListFragmentDirections
                    .actionToScanList(mExpId, sample))

        } else {

            mBinding?.let { ui ->

                sSnackbarQueue.push(SnackbarQueue.SnackJob(ui.root, getString(R.string.ask_sample_name)))

            }
        }
    }

    private fun FragmentSampleListBinding.setupRecyclerView() {

        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        recyclerView.adapter = SampleAdapter(requireContext())

        ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT) {

            override fun onMove(recyclerView: RecyclerView,
                                viewHolder: RecyclerView.ViewHolder,
                                target: RecyclerView.ViewHolder): Boolean {

                return false

            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {

                Dialogs.onOk(androidx.appcompat.app.AlertDialog.Builder(requireContext()), getString(R.string.ask_delete_all_scans), getString(R.string.cancel), getString(R.string.ok)) { result ->

                    if (result) {

                        (mBinding?.recyclerView?.adapter as SampleAdapter)
                                .currentList[viewHolder.adapterPosition].also { s ->

                            launch {

                                deleteSample(Sample(s.eid, s.name))

                                updateUi()

                            }

                        }

                    } else mBinding?.recyclerView?.adapter?.notifyDataSetChanged()
                }
            }

        }).attachToRecyclerView(recyclerView)
    }

    private suspend fun deleteSample(sample: Sample) = withContext(Dispatchers.IO) {

        sSamples.deleteSample(sample.eid, sample.name)

    }

    private fun updateUi() {

        sSamples.getSampleScanCounts(mExpId).observe(viewLifecycleOwner) {

            it?.let { date ->

                (mBinding?.recyclerView?.adapter as SampleAdapter)
                        .submitList(when (mSortState) {

                            DATE_DESC -> {

                                it.sortedByDescending { it.date }
                            }

                            DATE_ASC -> {

                                it.sortedBy { it.date }
                            }

                            ALPHA_DESC -> {

                                it.sortedByDescending { it.name }
                            }

                            else -> {

                                it.sortedBy { it.name }
                            }
                        })

                Handler().postDelayed({

                    mBinding?.recyclerView?.scrollToPosition(0)

                }, 250)
            }
        }
    }
}

