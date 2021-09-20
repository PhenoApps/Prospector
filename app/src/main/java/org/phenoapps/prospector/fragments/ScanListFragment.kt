package org.phenoapps.prospector.fragments

import CONVERT_TO_WAVELENGTHS
import DEVICE_ALIAS
import DEVICE_TYPE_LS1
import DEVICE_TYPE_NIR
import OPERATOR
import android.os.Bundle
import android.util.Log
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.tabs.TabLayout
import com.stratiotechnology.linksquareapi.LSFrame
import com.stratiotechnology.linksquareapi.LinkSquareAPI
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.WithFragmentBindings
import kotlinx.coroutines.*
import org.phenoapps.prospector.R
import org.phenoapps.prospector.activities.MainActivity
import org.phenoapps.prospector.adapter.ScansAdapter
import org.phenoapps.prospector.data.models.Scan
import org.phenoapps.prospector.data.models.SpectralFrame
import org.phenoapps.prospector.data.viewmodels.DeviceViewModel
import org.phenoapps.prospector.data.viewmodels.ScanViewModel
import org.phenoapps.prospector.databinding.FragmentScanListBinding
import org.phenoapps.prospector.interfaces.GraphItemClickListener
import org.phenoapps.prospector.utils.*
import java.lang.IllegalStateException
import java.util.*

/**
 * Fragment for visualizing and managing spectrometer scans. Contains a graph view for displaying results,
 * and a recycler list that allows the user to choose which graphs to render.
 *
 * This fragment listens for button press events from the physical spectrometer. Pressing the physical button
 * and clicking the bottom fab on this page are ubiquitous actions.
 *
 * Uses two toolbars. The title toolbar is similar tothe experiment list fragmnet which controls the connection status.
 * The second toolbar, scanToolbar, controls scan-level functionality s.a deleting scans.
 *
 * TODO: improve color choice using color picker
 */
@WithFragmentBindings
@AndroidEntryPoint
class ScanListFragment : Fragment(), CoroutineScope by MainScope(), GraphItemClickListener {

    private val TAG = this.tag ?: "ScanListFragment"

    private val sDeviceViewModel: DeviceViewModel by activityViewModels()

    private var mSelectedScanId: Long = -1

    private val mSnackbar = SnackbarQueue()

    private val sViewModel: ScanViewModel by viewModels()

    private var mBinding: FragmentScanListBinding? = null

    private var mExpId: Long = -1L

    private var mSampleName: String = String()

    private val mKeyUtil by lazy {
        KeyUtil(context)
    }

    //navigate to the instructions page if the device is not connected
    private val sOnClickScan = View.OnClickListener {

        if (sDeviceViewModel.isConnected()) {

            sDeviceViewModel.getDeviceInfo()?.let { connectedDeviceInfo ->

                callScanDialog(connectedDeviceInfo)

            }

        } else {

            findNavController().navigate(ScanListFragmentDirections
                    .actionToConnectInstructions())
        }
    }

    private fun insertScan(name: String, frames: List<LSFrame>) {

        val prefs = PreferenceManager.getDefaultSharedPreferences(requireContext())

        launch {

            frames.forEach { frame ->

                val sid = sViewModel.insertScanAsync(Scan(mExpId, name).apply {

                    val linkSquareApiDeviceId = sDeviceViewModel.getDeviceInfo()?.DeviceID ?: "Unknown Device Id"

                    deviceId = linkSquareApiDeviceId

                    this.deviceType = if (linkSquareApiDeviceId.startsWith("NIR")) DEVICE_TYPE_NIR else DEVICE_TYPE_LS1

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

    /**
     * Dialog is only called if the current connected deviceType matches the experiment deviceType.
     * If they match, the dialog begins which displays the status of the scan (indeterminately)
     */
    private fun callScanDialog(device: LinkSquareAPI.LSDeviceInfo) {

        sViewModel.experiments.observeOnce(viewLifecycleOwner, { experiments ->

            val exp = experiments?.find { it.eid == mExpId }

            if (exp?.deviceType == resolveDeviceType(requireContext(), device)) {

                val dialog = Dialogs.askForScan(requireActivity(), R.string.scanning, R.string.close)

                dialog.create()

                val dialogInterface = dialog.show()

                sDeviceViewModel.scan(requireContext()).observe(viewLifecycleOwner) {

                    it?.let { frames ->

                        launch {

                            insertScan(mSampleName, frames)

                            dialogInterface.dismiss()

                        }
                    }
                }

            } else {

                mBinding?.let { ui ->

                    mSnackbar.push(SnackbarQueue.SnackJob(ui.root, getString(R.string.frag_scan_device_type_mismatch)))
                }

            }
        })

    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        val contextThemeWrapper = ContextThemeWrapper(activity, R.style.AppTheme)

        val localInflater = inflater.cloneInContext(contextThemeWrapper)

        mBinding = DataBindingUtil.inflate(localInflater, R.layout.fragment_scan_list, container, false)

        mBinding?.let { ui ->

            val prefs = PreferenceManager.getDefaultSharedPreferences(requireContext())

            //ui.deleteOnClick = sOnClickDelete

            ui.scanOnClick = sOnClickScan

            //check if experiment id is included in the arguments.
            mExpId = arguments?.getLong("experiment", -1L) ?: -1L

            mSampleName = arguments?.getString("sample", String()) ?: String()

            ui.setupToolbar()

            if (mExpId != -1L && mSampleName.isNotBlank()) {

                ui.sampleName = mSampleName

                //whenever the tab layout changes, update the recylcer view
                ui.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
                    override fun onTabSelected(tab: TabLayout.Tab?) {

                        prefs.edit().putBoolean(mKeyUtil.lastSelectedGraph, when (tab?.position ?: 0) {
                            0 -> false
                            else -> true
                        }).apply()

                        resetGraph()

                        startObservers()

                    }

                    override fun onTabUnselected(tab: TabLayout.Tab?) {}

                    override fun onTabReselected(tab: TabLayout.Tab?) {}

                })

                ui.selectTabFromPrefs()

                launch {

                    loadGraph()

                }

                setupRecyclerView()

                startObservers()

            } else findNavController().popBackStack()
        }

        setHasOptionsMenu(true)

        return mBinding?.root
    }

    private fun FragmentScanListBinding.selectTabFromPrefs() {

        val prefs = PreferenceManager.getDefaultSharedPreferences(requireContext())

        tabLayout.getTabAt(if (prefs.getBoolean(mKeyUtil.lastSelectedGraph, false)) { 1 } else 0)
            ?.select()

    }

    private fun FragmentScanListBinding.setupToolbar() {
        
        scanToolbar.setNavigationOnClickListener {

            findNavController().popBackStack()

        }

        titleToolbar.setOnMenuItemClickListener {

            when(it.itemId) {

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

        scanToolbar.setOnMenuItemClickListener {

            when(it.itemId) {

                R.id.delete_scans -> Dialogs.onOk(AlertDialog.Builder(requireContext()),
                        getString(R.string.ask_delete_all_scans),
                        getString(R.string.cancel), getString(R.string.ok)) { booleanResult ->

                    if (booleanResult) {

                        launch {

                            deleteScans(mExpId, mSampleName)

                            resetGraph()
                        }
                    }
                }
            }

            true
        }
    }

    /**
     * Called whenever tab view is changed.
     */
    private fun resetGraph() {

        mSelectedScanId = -1

        mBinding?.graphView?.removeAllSeries()

        renderGraph(mSelectedScanId)
    }

    /**
     * Called when user changes the color of a given series.
     */
    private fun loadGraph() {

        mBinding?.graphView?.removeAllSeries()

        renderGraph(mSelectedScanId)

    }

    /**
     * Listens for the database spectral values and graphs them.
     * Checks the system preferences for translating pixel data to wavelengths.
     * "hacks" the graph view to display all data AND allow user to zoom/pan/scale
     *      does this by manually setting the view port, then enabling scaling features
     */
    private fun renderGraph(selectedScanId: Long) {

        mBinding?.let { ui ->

            sViewModel.getScans(mExpId, mSampleName).observeOnce(viewLifecycleOwner, { scans ->

                when (ui.tabLayout.selectedTabPosition) {

                    0 -> scans.filter { it.lightSource == 1 } //bulb

                    else -> scans.filter { it.lightSource == 0 } //led

                }.forEach { scan ->

                    sViewModel.getSpectralValuesLive(mExpId, scan.sid ?: -1).observeOnce(viewLifecycleOwner, { frames ->

                        frames?.let { data ->

                            if (data.isNotEmpty()) {

                                val convert = PreferenceManager.getDefaultSharedPreferences(context)
                                        .getBoolean(CONVERT_TO_WAVELENGTHS, true)

                                //trim actual values based on specs
                                val wavelengths = (if (convert) data.toWaveArray(scan.deviceType).filter {

                                    it.x <= when(scan.deviceType) {

                                        DEVICE_TYPE_NIR -> LinkSquareNIRRange.max

                                        else -> LinkSquareRange.max
                                    }

                                } else data.toPixelArray()).movingAverageSmooth()

                                setViewportGrid(ui.graphView, convert)

                                centerViewport(ui.graphView, wavelengths, convert, scan.deviceType)

                                setViewportScalable(ui.graphView)

                                renderNormal(ui.graphView, wavelengths,
                                        if ((scan.sid ?: -1) == selectedScanId) scan.color ?: "red" else "black")

                            }
                        }
                    })
                }
            })
        }
    }

    /**
     * User has options to re-import deleted scans (on swipe)
     */
    private fun reinsertScan(scan: Scan, frames: List<SpectralFrame>) {

        launch {

            val sid = sViewModel.insertScanAsync(scan).await()

            frames.forEach { frame ->

                sViewModel.insertFrame(sid, frame)

            }
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
                                    .currentList[viewHolder.absoluteAdapterPosition].also { scan ->

                                launch(Dispatchers.IO) {

                                    sViewModel.getSpectralValues(scan.eid, scan.sid
                                            ?: -1L).let { frames ->

                                        sViewModel.deleteScan(scan)

                                        mSnackbar.push(SnackbarQueue.SnackJob(ui.root, scan.name, undoString) {

                                            reinsertScan(scan, frames)

                                        })
                                    }
                                }
                            }

                        } else ui.recyclerView.adapter?.notifyItemChanged(viewHolder.absoluteAdapterPosition)
                    }
                }

            }).attachToRecyclerView(ui.recyclerView)
        }
    }

    private fun startObservers() {

        val check = object : TimerTask() {

            override fun run() {

                activity?.runOnUiThread {

                    with(mBinding?.titleToolbar) {

                        this?.menu?.findItem(R.id.action_connection)
                            ?.setIcon(if (sDeviceViewModel.isConnected()) {
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

        //updates recycler view with available scans
        sViewModel.getScans(mExpId, mSampleName).observe(viewLifecycleOwner, { data ->

            if (data.isNotEmpty()) {

                mBinding?.let { ui ->

                    with (ui.recyclerView.adapter as ScansAdapter) {

                        submitList(when (ui.tabLayout.selectedTabPosition) {
                            0 -> data.filter { it.lightSource == 1 } //bulb first
                            else -> data.filter { it.lightSource == 0 } //led second
                        })

                    }

                    val total = this.resources.getQuantityString(
                        R.plurals.numberOfScans, data.size, data.size)

                    ui.sampleName = "$mSampleName $total"

                    ui.executePendingBindings()

                    resetGraph()

                }
            } else (mBinding?.recyclerView?.adapter as? ScansAdapter)?.submitList(data)
        })

        attachDeviceButtonPressListener()

        //set the title header
        sViewModel.experiments.observe(viewLifecycleOwner, { experiments ->

            experiments.first { it.eid == mExpId }.also {

                activity?.runOnUiThread {

                    mBinding?.titleToolbar?.title = it.name

                }
            }
        })
    }

    /**
     * LinkSquare API live data listener that responds to on-device button clicks.
     */
    private fun attachDeviceButtonPressListener() {

        try {

            sDeviceViewModel.setEventListener {

                requireActivity().runOnUiThread {

                    if (sDeviceViewModel.isConnected()) {

                        sDeviceViewModel.getDeviceInfo()?.let { connectedDeviceInfo ->

                            callScanDialog(connectedDeviceInfo)

                        }

                    }

                }

            }.observe(viewLifecycleOwner, {})

        } catch (e: IllegalStateException) {

            Log.d(TAG, "Failed to connect LS")
        }
    }

    private suspend fun deleteScans(exp: Long, sample: String) = withContext(Dispatchers.IO) {

        sViewModel.deleteScans(exp, sample)

    }

    /**
     * Listener connected to the adapter. Whenever a date is clicked it is either added to
     * the list of viewable graphs or removed if it already exists.
     */
    override fun onItemClicked(id: Long, color: String?) {

        mSelectedScanId = id

        loadGraph()
    }

    override fun onItemLongClicked(id: Long, color: String?) {

        color?.let { nonNullColor ->

            launch {

                sViewModel.updateScanColor(mExpId, id, nonNullColor)

            }

            loadGraph()

        }
    }
}