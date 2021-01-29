package org.phenoapps.prospector.fragments

import ALPHA_DESC
import DATE_ASC
import DATE_DESC
import android.os.Bundle
import android.os.Handler
import android.view.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ContextThemeWrapper
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
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

    private val viewModel: ExperimentSamplesViewModel by viewModels {

        ExperimentSamplesViewModelFactory(
                ProspectorRepository.getInstance(
                        ProspectorDatabase.getInstance(requireContext())
                                .expScanDao()))

    }

    private var mBinding: FragmentSampleListBinding? = null

    private val sOnSearchClickListener = View.OnClickListener {

        findNavController().navigate(SampleListFragmentDirections
                .actionToBarcodeSearch(mExpId))

    }

    private val sOnNewClickListener = View.OnClickListener {

        findNavController().navigate(SampleListFragmentDirections
                .actionToNewSample(mExpId))

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

                ui.setupRecyclerView()

                ui.setupButtons()

                startObservers()

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

            R.id.menu_search_sample -> {

                findNavController().navigate(SampleListFragmentDirections
                        .actionToBarcodeSearch(mExpId))

            }

//            R.id.menu_sort_sample -> {
//
//                item.setIcon(when (mSortState) {
//
//                    ALPHA_ASC -> {
//
//                        mSortState = DATE_DESC
//
//                        org.phenoapps.icons.R.drawable.ic_sort_by_date_descending_18dp
//
//                    }
//
//                    ALPHA_DESC -> {
//
//                        mSortState = ALPHA_ASC
//
//                        org.phenoapps.icons.R.drawable.ic_sort_by_alpha_white_ascending_18dp
//
//                    }
//
//                    DATE_ASC -> {
//
//                        mSortState = ALPHA_DESC
//
//                        org.phenoapps.icons.R.drawable.ic_sort_by_alpha_white_descending_18dp
//
//                    }
//
//                    else -> {
//
//                        mSortState = DATE_ASC
//
//                        org.phenoapps.icons.R.drawable.ic_sort_by_date_ascending_18dp
//
//                    }
//                })
//
//                startObservers()
//            }
        }

        return super.onOptionsItemSelected(item)
    }

    private fun FragmentSampleListBinding.setupButtons() {

        onClick = sOnNewClickListener

    }

    private suspend fun insertSample(sample: String, note: String) = withContext(Dispatchers.IO) {

        viewModel.insertSample(Sample(mExpId, sample).apply {

            this.date = DateUtil().getTime()

            this.note = note

        }).await()

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

                            }
                        }

                    } else mBinding?.recyclerView?.adapter?.notifyDataSetChanged()
                }
            }

        }).attachToRecyclerView(recyclerView)
    }

    private suspend fun deleteSample(sample: Sample) = withContext(Dispatchers.IO) {

        viewModel.deleteSample(sample.eid, sample.name)

    }

    private fun startObservers() {

        //set the title header
        viewModel.experiments.observe(viewLifecycleOwner, {

            it.filter { it.eid == mExpId }.first().also {

                activity?.runOnUiThread {

                    (activity as? AppCompatActivity)?.supportActionBar?.title = it.name

                }
            }
        })

        viewModel.getSampleScanCounts(mExpId).observe(viewLifecycleOwner, Observer {

            it?.let { data ->

                (mBinding?.recyclerView?.adapter as SampleAdapter)
                        .submitList(when (mSortState) {

                            DATE_DESC -> {

                                data.sortedByDescending { it.date }
                            }

                            DATE_ASC -> {

                                data.sortedBy { it.date }
                            }

                            ALPHA_DESC -> {

                                data.sortedByDescending { it.name }
                            }

                            else -> {

                                data.sortedBy { it.name }
                            }
                        })

                Handler().postDelayed({

                    mBinding?.recyclerView?.scrollToPosition(0)

                }, 250)
            }
        })
    }
}

