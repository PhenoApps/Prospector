package org.phenoapps.prospector.fragments

import DESCENDING
import SORT_BY_DATE
import SORT_BY_NAME
import android.os.Bundle
import android.os.Handler
import android.view.*
import androidx.appcompat.view.ContextThemeWrapper
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
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
import org.phenoapps.prospector.listeners.ToggleClickListener
import org.phenoapps.prospector.utils.DateUtil
import org.phenoapps.prospector.utils.Dialogs
import org.phenoapps.prospector.utils.SnackbarQueue

class SampleListFragment : Fragment(), CoroutineScope by MainScope() {

    /*
    0 is sort by name while 1 is sort by date
     */
    private var mSortState = SORT_BY_DATE

    /*
    0 is ascending order, 1 is descending
     */
    private var mSortParity = DESCENDING

    private val sSnackbarQueue = SnackbarQueue()

    private var mExpId: Long = -1L

    private val sSamples: ExperimentSamplesViewModel by viewModels {

        ExperimentSamplesViewModelFactory(
                ProspectorRepository.getInstance(
                        ProspectorDatabase.getInstance(requireContext())
                                .expScanDao()))

    }

    private var mBinding: FragmentSampleListBinding? = null

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

            R.id.menu_search_sample -> {

                findNavController().navigate(SampleListFragmentDirections
                        .actionToBarcodeScan(mExpId))

            }
        }

        return super.onOptionsItemSelected(item)
    }


    private fun FragmentSampleListBinding.setupButtons() {

        onClick = View.OnClickListener {

            callInsertDialog()

        }

        onToggle = ToggleClickListener {

            mSortState = it

            updateUi()
        }

        onToggleParity = ToggleClickListener {

            mSortParity = it

            updateUi()
        }
    }

    private fun callInsertDialog() {

        Dialogs.askForName(requireActivity(), R.string.ask_sample_name, R.string.ok, R.string.cancel) { sampleName, scanNote ->

            if (sampleName.isNotBlank()) {

                launch {

                    sSamples.insertSample(Sample(mExpId, sampleName).apply {

                        this.date = DateUtil().getTime()

                        this.note = scanNote

                    }).await()

                    updateUi()
                }


            } else {

                mBinding?.let { ui ->

                    sSnackbarQueue.push(SnackbarQueue.SnackJob(ui.root, getString(R.string.ask_sample_name)))

                }
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
                                .currentList[viewHolder.adapterPosition].also { sample ->

                            launch {

                                deleteSample(sample)

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

        sSamples.getSamples(mExpId).observe(viewLifecycleOwner) {

            val sorted = when (mSortParity) {

                DESCENDING -> {

                     when (mSortState) {

                         SORT_BY_NAME -> {

                             it.sortedByDescending { it.name }
                         }

                         else -> {

                             it.sortedByDescending { it.date }
                         }
                     }
                }

                else -> {

                    when (mSortState) {

                        SORT_BY_NAME -> {

                            it.sortedBy { it.name }
                        }

                        else -> {

                            it.sortedBy { it.date }
                        }
                    }
                }
            }

            (mBinding?.recyclerView?.adapter as SampleAdapter)
                    .submitList(sorted)

            Handler().postDelayed({

                mBinding?.recyclerView?.scrollToPosition(0)

            }, 250)
        }
    }
}

