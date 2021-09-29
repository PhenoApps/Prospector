package org.phenoapps.prospector.fragments

import CONVERT_TO_WAVELENGTHS
import DEVICE_ALIAS
import DEVICE_TYPE_LS1
import DEVICE_TYPE_NIR
import OPERATOR
import android.media.MediaPlayer
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

    private val sDeviceScope = CoroutineScope(Dispatchers.IO)

    private val sDeviceViewModel: DeviceViewModel by activityViewModels()

    private var mSelectedScanId: Long = -1

    private val mSnackbar = SnackbarQueue()

    private val sViewModel: ScanViewModel by viewModels()

    private var mBinding: FragmentScanListBinding? = null

    private var mExpId: Long = -1L

    private var mSampleName: String = String()

    private var mTimer: Timer? = null

    private var mIsScanning: Boolean = false

    private val mKeyUtil by lazy {
        KeyUtil(context)
    }

    private val mPrefs by lazy {
        PreferenceManager.getDefaultSharedPreferences(context)
    }

    //navigate to the instructions page if the device is not connected
    private val sOnClickScan = View.OnClickListener {

        if (sDeviceViewModel.isConnected()) {

            if (!mIsScanning) {

                mIsScanning = true

                sDeviceViewModel.getDeviceInfo()?.let { connectedDeviceInfo ->

                    callScanDialog(connectedDeviceInfo)

                }
            }


        } else {

            if (findNavController().currentDestination?.id == R.id.scan_list_fragment) {
                findNavController().navigate(ScanListFragmentDirections
                    .actionToConnectInstructions())
            }
        }
    }

    private suspend fun insertScan(name: String, frames: List<LSFrame>) {

        val alias = mPrefs.getString(DEVICE_ALIAS, "")
        val operator = mPrefs.getString(OPERATOR, "") ?: ""
        val device = sDeviceViewModel.getDeviceInfo()?.DeviceID ?: "Unknown Device Id"
        val deviceType = if (device.startsWith("NIR")) DEVICE_TYPE_NIR else DEVICE_TYPE_LS1

        val scan = Scan(mExpId, name).apply {
            this.deviceId = device
            this.deviceType = deviceType
            this.alias = alias
            this.operator = operator
        }

        frames.forEach { frame ->

            scan.lightSource = frame.lightSource.toInt()

            val sid = sViewModel.insertScanAsync(scan).await()

            sViewModel.insertFrame(sid, SpectralFrame(
                    sid,
                    frame.frameNo,
                    frame.raw_data.joinToString(" ") { value -> value.toString() },
                    frame.lightSource.toInt())
            )
        }
    }

    /**
     * Dialog is only called if the current connected deviceType matches the experiment deviceType.
     * If they match, the dialog begins which displays the status of the scan (indeterminately)
     */
    private fun callScanDialog(device: LinkSquareAPI.LSDeviceInfo) {

        activity?.let { act ->

            context?.let { ctx ->

                sViewModel.experiments.observeOnce(viewLifecycleOwner, { experiments ->

                    val exp = experiments?.find { it.eid == mExpId }

                    if (exp?.deviceType == resolveDeviceType(ctx, device)) {

                        val dialog = Dialogs.askForScan(act, R.string.scanning, R.string.close)

                        dialog.create()

                        val dialogInterface = dialog.show()

                        sDeviceViewModel.scan(ctx).observeOnce(viewLifecycleOwner) {

                            it?.let { frames ->

                                sDeviceScope.launch {

                                    insertScan(mSampleName, frames)

                                    activity?.runOnUiThread {
                                        dialogInterface.dismiss()
                                        checkAudioTriggers()
                                        loadGraph()
                                        mIsScanning = false
                                    }
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
        }
    }

    /**
     * Checks the preferences for audio enabled settings.
     * If the setting is enabled, run a chime because a scan was made.
     * Also check if the number of scans matches the target scan if so play a chime and finish the fragment.
     */
    private fun checkAudioTriggers() {

        //check if audio enabled
        if (mPrefs.getBoolean(mKeyUtil.audioEnabled, true)) {

            val scanAudio = MediaPlayer.create(context, R.raw.hero_simple_celebration)
            val targetMetAudio = MediaPlayer.create(context, R.raw.hero_decorative_celebration)

            //check if target scan is set and met
            val target = mPrefs.getString(mKeyUtil.targetScans, "") ?: ""

            //if no target is set, play the scan audio
            if (target.isBlank()) {

                scanAudio.start()

            } else { //check if target is met, otherwise play the scan audio

                sViewModel.getScans(mExpId, mSampleName).observeOnce(viewLifecycleOwner) {

                    target.toIntOrNull()?.let { targetInt ->

                        if (it.size >= targetInt) {

                            targetMetAudio.start()

                            (activity as? MainActivity)?.notify(getString(R.string.target_scan_success))

                            findNavController().popBackStack()

                        } else {

                            scanAudio.start()
                        }
                    }
                }
            }
        } else checkTarget() //still want to check target and pop backstack if it is met
    }

    private fun checkTarget() {

        //check if target scan is set and met
        val target = mPrefs.getString(mKeyUtil.targetScans, "") ?: ""

        //if no target is set, play the scan audio
        if (target.isNotBlank()) {

            sViewModel.getScans(mExpId, mSampleName).observeOnce(viewLifecycleOwner) {

                target.toIntOrNull()?.let { targetInt ->

                    if (it.size >= targetInt) {

                        (activity as? MainActivity)?.notify(getString(R.string.target_scan_success))

                        findNavController().popBackStack()

                    }
                }
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        val contextThemeWrapper = ContextThemeWrapper(activity, R.style.AppTheme)

        val localInflater = inflater.cloneInContext(contextThemeWrapper)

        mBinding = DataBindingUtil.inflate(localInflater, R.layout.fragment_scan_list, container, false)

        mBinding?.let { ui ->

            val prefs = PreferenceManager.getDefaultSharedPreferences(requireContext())

            ui.scanOnClick = sOnClickScan

            //check if experiment id is included in the arguments.
            mExpId = arguments?.getLong("experiment", -1L) ?: -1L

            mSampleName = arguments?.getString("sample", String()) ?: String()

            val startScan = arguments?.getBoolean("startScan", false) ?: false

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

                loadGraph()

                setupRecyclerView()

                startTimer()

                startObservers()

            } else {

                findNavController().popBackStack()

            }

            if (startScan && sDeviceViewModel.isConnected()) {

                sDeviceViewModel.getDeviceInfo()?.let { connectedDeviceInfo ->

                    callScanDialog(connectedDeviceInfo)

                }
            }
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

                            if (findNavController().currentDestination?.id == R.id.scan_list_fragment) {
                                findNavController().navigate(ScanListFragmentDirections
                                    .actionToConnectInstructions())
                            }

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

                        sDeviceScope.launch {

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

        mBinding?.graphView?.visibility = View.VISIBLE

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

        sDeviceScope.launch {

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

                                sDeviceScope.launch {

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

    private fun startTimer() {

        val check = object : TimerTask() {

            override fun run() {

                activity?.runOnUiThread {

                    if (isAdded) {
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
        }

        mTimer = Timer()

        mTimer?.scheduleAtFixedRate(check, 0, 1500)
    }

    private fun startObservers() {

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

                    ui.scanCount = total

                    ui.sampleName = "$mSampleName"

                    ui.executePendingBindings()

                    resetGraph()

                }
            } else {

                mBinding?.scanCount = "0"
                mBinding?.executePendingBindings()
                (mBinding?.recyclerView?.adapter as? ScansAdapter)?.submitList(data)
            }
        })

        attachDeviceButtonPressListener()

        //set the title header
        sViewModel.experiments.observeOnce(viewLifecycleOwner, { experiments ->

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

                if (!mIsScanning) {

                    mIsScanning = true

                    activity?.runOnUiThread {

                        if (sDeviceViewModel.isConnected()) {

                            sDeviceViewModel.getDeviceInfo()?.let { connectedDeviceInfo ->

                                callScanDialog(connectedDeviceInfo)

                            }
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

            sDeviceScope.launch {

                sViewModel.updateScanColor(mExpId, id, nonNullColor)

            }

            loadGraph()

        }
    }

    override fun onDestroy() {
        super.onDestroy()

        mTimer?.cancel()

        mTimer?.purge()

        mTimer = null
    }
}