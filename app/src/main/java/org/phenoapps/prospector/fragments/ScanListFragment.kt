package org.phenoapps.prospector.fragments

import CONVERT_TO_WAVELENGTHS
import DEVICE_ALIAS
import DEVICE_TYPE
import OPERATOR
import android.os.Bundle
import android.view.*
import androidx.appcompat.app.AlertDialog
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.tabs.TabLayout
import com.stratiotechnology.linksquareapi.LSFrame
import kotlinx.coroutines.*
import org.phenoapps.prospector.R
import org.phenoapps.prospector.adapter.ScansAdapter
import org.phenoapps.prospector.data.ProspectorDatabase
import org.phenoapps.prospector.data.ProspectorRepository
import org.phenoapps.prospector.data.models.Scan
import org.phenoapps.prospector.data.models.SpectralFrame
import org.phenoapps.prospector.data.viewmodels.DeviceViewModel
import org.phenoapps.prospector.data.viewmodels.ExperimentSamplesViewModel
import org.phenoapps.prospector.data.viewmodels.factory.ExperimentSamplesViewModelFactory
import org.phenoapps.prospector.databinding.FragmentScanListBinding
import org.phenoapps.prospector.interfaces.GraphItemClickListener
import org.phenoapps.prospector.utils.*

class ScanListFragment : Fragment(), CoroutineScope by MainScope(), GraphItemClickListener {

    private var mScanId: Long = 1L

    private var mScanIds: Set<Long> = HashSet<Long>()
    private var mScanColors = HashMap<Long, String>()

    private val mSnackbar = SnackbarQueue()

    private val sDeviceViewModel: DeviceViewModel by activityViewModels()

    private val sViewModel: ExperimentSamplesViewModel by viewModels {

        ExperimentSamplesViewModelFactory(
                ProspectorRepository.getInstance(
                        ProspectorDatabase.getInstance(requireContext()).expScanDao()
                )
        )
    }

    private var mBinding: FragmentScanListBinding? = null

    private var mExpId: Long = -1L

    private var mSampleName: String? = null

    private fun insertScan(name: String, frames: List<LSFrame>) {

        val prefs = PreferenceManager.getDefaultSharedPreferences(requireContext())

        launch {

            frames.forEach { frame ->

                val sid = sViewModel.insertScan(Scan(mExpId, name).apply {

                    deviceId = sDeviceViewModel.getDeviceInfo()?.DeviceID

                    this.deviceType = prefs.getString(DEVICE_TYPE, "LinkSquare") ?: "LinkSquare"

                    this.alias = prefs.getString(DEVICE_ALIAS, "")

                    this.lightSource = frame.lightSource.toInt()

                    this.operator = prefs.getString(OPERATOR, "") ?: ""

                }).await()

                sViewModel.insertFrame(sid, SpectralFrame(
                        sid,
                        frame.frameNo,
                        frame.raw_data.joinToString(" ") { value -> value.toString() },
                        frame.lightSource.toInt())
                )

            }
        }
    }

    private fun reinsertScan(scan: Scan, frames: List<SpectralFrame>) {

        launch {

            val sid = sViewModel.insertScan(scan).await()

            frames.forEach { frame ->

                sViewModel.insertFrame(sid, frame)

            }
        }
    }

    private val sOnClickScan = View.OnClickListener {

        if (sDeviceViewModel.isConnected()) {

            callScanDialog()

        } else {

            findNavController().navigate(ScanListFragmentDirections
                    .actionToConnectInstructions())
        }
    }

    private fun callScanDialog() {

        val dialog = Dialogs.askForScan(requireActivity(), R.string.scanning_sample, R.string.ok, R.string.close) {

        }

        dialog.create()

        val dialogInterface = dialog.show()

        sDeviceViewModel.scan(requireContext()).observe(viewLifecycleOwner, {

            it?.let {  frames ->

                launch {

                    mSampleName?.let { sid ->

                        insertScan(sid, frames)

                    }

                    dialogInterface.dismiss()

                }
            }
        })
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        val contextThemeWrapper = ContextThemeWrapper(activity, R.style.AppTheme)

        val localInflater = inflater.cloneInContext(contextThemeWrapper)

        mBinding = DataBindingUtil.inflate(localInflater, R.layout.fragment_scan_list, container, false)

        mBinding?.let { ui ->

            //ui.deleteOnClick = sOnClickDelete

            ui.scanOnClick = sOnClickScan

            //check if experiment id is included in the arguments.
            val eid = arguments?.getLong("experiment", -1L) ?: -1L

            val name = arguments?.getString("sample", String()) ?: String()

            ui.title = name

            if (eid != -1L && name.isNotBlank()) {

                mExpId = eid

                mSampleName = name

                ui.sampleName = mSampleName

                //whenever the tab layout changes, update the recylcer view
                ui.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
                    override fun onTabSelected(tab: TabLayout.Tab?) {

                        resetGraph()

                        startObservers()

                    }

                    override fun onTabUnselected(tab: TabLayout.Tab?) {}

                    override fun onTabReselected(tab: TabLayout.Tab?) {}

                })

                launch {

                    loadGraph()

                }

                setupRecyclerView()

                startObservers()

            }
        }

        setHasOptionsMenu(true)

        return mBinding?.root
    }

    private fun resetGraph() {

        mScanIds = HashSet<Long>()

        mScanColors = HashMap()

        mBinding?.let { ui ->

            resetGraph(ui.graphView)

        }
    }

    private fun loadGraph() {

//        mBinding?.let { ui ->
//
//            resetGraph(ui.graphView)
//
//        }

        mBinding?.graphView?.removeAllSeries()

        mSampleName?.let { sampleId ->

            mScanIds.forEach {

                renderGraph(sampleId, it, mScanColors[it])
            }
        }
    }

    private fun renderGraph(sampleId: String, scanId: Long, color: String?) {

        mBinding?.let { ui ->

            sViewModel.getSpectralValuesLive(mExpId, scanId).observeOnce(viewLifecycleOwner, {

                it?.let { data ->

                    if (data.isNotEmpty()) {

                        val convert = PreferenceManager.getDefaultSharedPreferences(context)
                                .getBoolean(CONVERT_TO_WAVELENGTHS, true)

                        val wavelengths = if (convert) data.toWaveArray() else data.toPixelArray()

                        setViewportGrid(ui.graphView)

                        centerViewport(ui.graphView, wavelengths)

                        setViewportScalable(ui.graphView)

                        renderNormal(ui.graphView, wavelengths, color)

                    }
                }
            })
        }
    }

    private fun setupRecyclerView() {

        mBinding?.let { ui ->

            ui.recyclerView.adapter = ScansAdapter(requireContext(), this)

            val undoString = getString(R.string.undo)

            ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT) {

                override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
                    return false
                }

                override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {

                    Dialogs.onOk(AlertDialog.Builder(requireContext()), getString(R.string.ask_delete_scan), getString(R.string.cancel), getString(R.string.ok)) { result ->

                        if (result) {

                            (ui.recyclerView.adapter as ScansAdapter)
                                    .currentList[viewHolder.adapterPosition].also { scan ->

                                launch {

                                    withContext(Dispatchers.IO) {

                                        sViewModel.getSpectralValues(scan.eid, scan.sid
                                                ?: -1L).let { frames ->

                                            sViewModel.deleteScan(scan)

                                            mSnackbar.push(SnackbarQueue.SnackJob(ui.root, scan.name, undoString) {

                                                reinsertScan(scan, frames)

                                            })
                                        }
                                    }
                                }
                            }
                        } else ui.recyclerView.adapter?.notifyDataSetChanged()
                    }
                }

            }).attachToRecyclerView(ui.recyclerView)
        }
    }

    private fun startObservers() {

        mSampleName?.let { name ->

            sViewModel.getScans(mExpId, name).observe(viewLifecycleOwner, { data ->

                if (data.isNotEmpty()) {

                    data.forEach {
                        it?.sid?.let { id ->
                            it?.color?.let { color ->

                                mScanColors[id] = color

                            }
                        }
                    }

                    mBinding?.let { ui ->

                        with (ui.recyclerView.adapter as ScansAdapter) {

                            submitList(when (ui.tabLayout.selectedTabPosition) {
                                0 -> data.filter { it.lightSource == 0 }
                                else -> data.filter { it.lightSource == 1 }
                            })

                        }

                        ui.executePendingBindings()

                    }
                } else (mBinding?.recyclerView?.adapter as? ScansAdapter)?.submitList(data)
            })
        }

        /**
         * LinkSquare API live data listener that responds to on-device button clicks.
         */
        sDeviceViewModel.setEventListener {

            requireActivity().runOnUiThread {

                if (sDeviceViewModel?.isConnected()) {

                    callScanDialog()

                }

            }

        }.observe(viewLifecycleOwner, Observer {

        })
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {

        inflater.inflate(R.menu.menu_scan_list, menu)

        super.onCreateOptionsMenu(menu, inflater)

    }

    private suspend fun deleteScans(exp: Long, sample: String) = withContext(Dispatchers.IO) {

        sViewModel.deleteScans(exp, sample)

    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        when (item.itemId) {

            R.id.delete_scans -> Dialogs.onOk(androidx.appcompat.app.AlertDialog.Builder(requireContext()), getString(R.string.ask_delete_all_scans), getString(R.string.cancel), getString(R.string.ok)) {

                if (it) {

                    mSampleName?.let { sample ->

                        launch {

                            deleteScans(mExpId, sample)

                        }
                    }
                }
            }
        }

        return super.onOptionsItemSelected(item)
    }

    /**
     * Listener connected to the adapter. Whenever a date is clicked it is either added to
     * the list of viewable graphs or removed if it already exists.
     */
    override fun onItemClicked(id: Long, color: String?) {

        mScanIds = if (id in mScanIds) mScanIds - id else mScanIds + id

        loadGraph()
    }

    override fun onItemLongClicked(id: Long, color: String?) {

        color?.let { nonNullColor ->

            mScanColors[id] = nonNullColor

            launch {

                sViewModel.updateScanColor(mExpId, id, nonNullColor)

            }

            loadGraph()

        }
    }
}