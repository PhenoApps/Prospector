package org.phenoapps.prospector.fragments

import ALPHA_DESC
import CONVERT_TO_WAVELENGTHS
import DATE_ASC
import DATE_DESC
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ContextThemeWrapper
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.*
import org.phenoapps.prospector.R
import org.phenoapps.prospector.activities.MainActivity
import org.phenoapps.prospector.adapter.SampleAdapter
import org.phenoapps.prospector.data.ProspectorDatabase
import org.phenoapps.prospector.data.models.Sample
import org.phenoapps.prospector.data.viewmodels.SampleViewModel
import org.phenoapps.prospector.data.viewmodels.factory.SampleViewModelFactory
import org.phenoapps.prospector.data.viewmodels.repository.ExperimentRepository
import org.phenoapps.prospector.data.viewmodels.repository.SampleRepository
import org.phenoapps.prospector.databinding.FragmentSampleListBinding
import org.phenoapps.prospector.utils.DateUtil
import org.phenoapps.prospector.utils.Dialogs
import org.phenoapps.prospector.utils.FileUtil
import org.phenoapps.prospector.utils.observeOnce

/**
 * Similar to the experiment fragment, this displays lists of samples for a given experiment.
 * Sample View Model includes experiment repo to query for experiment names.
 *
 * User can search for samples using a barcode scanner by pressing the magnifying glass in the toolbar.
 * Experiments can be exported here using the floppy disk in the toolbar.
 */
class SampleListFragment : Fragment(), CoroutineScope by MainScope() {

    //deprecated sort functionality, app only sorts by DATE_DESC atm
    private var mSortState = DATE_DESC

    //fragment argument
    private var mExpId: Long = -1L

    private val sViewModel: SampleViewModel by viewModels {

        with(ProspectorDatabase.getInstance(requireContext())) {
            SampleViewModelFactory(ExperimentRepository.getInstance(experimentDao()),
                    SampleRepository.getInstance(sampleDao()))
        }
    }

    private var mBinding: FragmentSampleListBinding? = null

    private val sOnNewClickListener = View.OnClickListener {

        findNavController().navigate(SampleListFragmentDirections
                .actionToNewSample(mExpId))

    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        val eid = arguments?.getLong("experiment", -1L) ?: -1L

        if (eid != -1L) { //finish fragment if an invalid eid is given

            setHasOptionsMenu(true)

            mExpId = eid

            val contextThemeWrapper = ContextThemeWrapper(activity, R.style.AppTheme)

            val localInflater = inflater.cloneInContext(contextThemeWrapper)

            mBinding = DataBindingUtil.inflate(localInflater, R.layout.fragment_sample_list, container, false)

            mBinding?.let { ui ->

                ui.setupRecyclerView()

                ui.setupButtons()

                ui.setupToolbar()

                startObservers()

                return ui.root
            }

        } else findNavController().popBackStack()

        return null
    }

    private fun FragmentSampleListBinding.setupToolbar() {

        samplesToolbar.setNavigationOnClickListener {

            findNavController().popBackStack()

        }

        samplesToolbar.setOnMenuItemClickListener { item ->

            when(item.itemId) {

                R.id.menu_search_sample -> {

                    findNavController().navigate(SampleListFragmentDirections
                            .actionToBarcodeSearch(mExpId))

                }

                R.id.menu_export -> {

                    exportFile("csv")

                }

                R.id.action_connection -> {

                    with (activity as? MainActivity) {

                        if (this?.mConnected == true) {

                            sDeviceViewModel.reset()
                        } else {

                            findNavController().navigate(ExperimentListFragmentDirections
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
     * Uses activity results contracts to create a document and call the export function
     * This method must queries for the device type export view and matches the exports with the
     * current experiment id.
     */
    private fun exportFile(exportType: String) {

        val prefs = PreferenceManager.getDefaultSharedPreferences(requireContext())

        val convert = prefs.getBoolean(CONVERT_TO_WAVELENGTHS, false)

        //start observing for exportable experiments using the defined view
        sViewModel.deviceTypeExports.observeOnce(viewLifecycleOwner, {

            //only export if the view has rows, and only export the current selected experiment
            it?.let { exports ->

                exports.filter { row -> row.experimentId == mExpId }.also { exportables ->

                    //grab the first experiment as an example to find the name and device type for the filename
                    val example = it.firstOrNull()

                    example?.let { it ->

                        val fileName = "${it.experiment}_${it.deviceType}_${DateUtil().getTime()}.csv"

                        //use the found experiment info from the example to launch an intent to create the output file
                        (activity as AppCompatActivity).registerForActivityResult(ActivityResultContracts.CreateDocument()) { nullUri -> nullUri?.let { uri ->

                            //ensure the context is not null and launch on a coroutine
                            context?.let { ctx ->

                                launch {

                                    withContext(Dispatchers.IO) {

                                        val start = System.nanoTime()

                                        //todo experiment is not the id
                                        FileUtil(ctx).exportCsv(uri, exportables, convert)

                                        Log.d("ExportTime", (1e-9 * (System.nanoTime() - start)).toString())
                                    }
                                }
                            }

                        }}.launch(fileName)
                    }
                }
            }
        })
    }

    private fun FragmentSampleListBinding.setupButtons() {

        onClick = sOnNewClickListener

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

        sViewModel.deleteSample(sample.eid, sample.name)

    }

    private fun startObservers() {

        (activity as? MainActivity)?.sDeviceViewModel?.isConnectedLive()?.observeForever { connected ->

            connected?.let { status ->

                with(mBinding?.samplesToolbar) {

                    this?.menu?.findItem(R.id.action_connection)
                            ?.setIcon(if (status) R.drawable.ic_bluetooth_connected_black_18dp
                            else R.drawable.ic_clear_black_18dp)

                }

            }
        }

        //set the title header
        sViewModel.experiments.observe(viewLifecycleOwner, { experiments ->

            experiments.first { it.eid == mExpId }.also {

                activity?.runOnUiThread {

                    mBinding?.samplesToolbar?.title = it.name

                }
            }
        })

        sViewModel.getSampleScanCounts(mExpId).observe(viewLifecycleOwner, { samples ->

            samples?.let { data ->

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

