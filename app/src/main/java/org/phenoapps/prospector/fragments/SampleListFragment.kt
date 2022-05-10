package org.phenoapps.prospector.fragments

import ALPHA_ASC
import ALPHA_DESC
import CONVERT_TO_WAVELENGTHS
import DATE_ASC
import DATE_DESC
import DEVICE_TYPE_LS1
import DEVICE_TYPE_NANO
import DEVICE_TYPE_NIR
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.view.ContextThemeWrapper
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.WithFragmentBindings
import kotlinx.coroutines.*
import org.phenoapps.prospector.R
import org.phenoapps.prospector.activities.MainActivity
import org.phenoapps.prospector.adapter.SampleAdapter
import org.phenoapps.prospector.data.models.Sample
import org.phenoapps.prospector.data.viewmodels.SampleViewModel
import org.phenoapps.prospector.data.viewmodels.devices.InnoSpectraViewModel
import org.phenoapps.prospector.databinding.FragmentSampleListBinding
import org.phenoapps.prospector.interfaces.SampleListClickListener
import org.phenoapps.prospector.utils.*
import java.util.*
import kotlin.math.exp

/**
 * Similar to the experiment fragment, this displays lists of samples for a given experiment.
 * Sample View Model includes experiment repo to query for experiment names.
 *
 * User can search for samples using a barcode scanner by pressing the magnifying glass in the toolbar.
 * Experiments can be exported here using the floppy disk in the toolbar.
 */
@WithFragmentBindings
@AndroidEntryPoint
class SampleListFragment : ConnectionFragment(R.layout.fragment_sample_list), CoroutineScope by MainScope(),
    SampleListClickListener {

    //deprecated sort functionality, app only sorts by DATE_DESC atm
    private var mSortState = DATE_DESC

    //fragment argument
    private var mExpId: Long = -1L
    private var mDeviceType: String = String()
    private var mName: String = String()

    private val sViewModel: SampleViewModel by viewModels()

    private var mBinding: FragmentSampleListBinding? = null

    private val sOnNewClickListener = View.OnClickListener {

        if (findNavController().currentDestination?.id == R.id.sample_list_fragment) {
            findNavController().navigate(SampleListFragmentDirections
                .actionToNewSample(mExpId))
        }
    }

    private val mScope by lazy {
        CoroutineScope(Dispatchers.IO)
    }

    /**
     * Click listener that is used for the new scan button
     * when the sample scanner preference is enabled.
     */
    private val sOnNewClickSampleScannerListener = View.OnClickListener {

        setFragmentResultListener("BarcodeResult") { _, bundle ->

            val code = bundle.getString("barcode_result", "") ?: ""

            mScope.launch {

                val sample = Sample(mExpId, code)

                sViewModel.insertSampleAsync(sample).await()

                activity?.runOnUiThread {
                    if (findNavController().currentDestination?.id == R.id.sample_list_fragment) {
                        findNavController().navigate(SampleListFragmentDirections
                            .actionToScanList(mExpId, sample.name))
                    }
                }
            }

            updateUi()
        }

        if (findNavController().currentDestination?.id == R.id.sample_list_fragment) {
            findNavController().navigate(SampleListFragmentDirections
                .actionToBarcodeScan())
        }
    }

    private lateinit var mSnackbar: SnackbarQueue
    private lateinit var requestExportLauncher: ActivityResultLauncher<String>
    private lateinit var requestPermissionsLauncher: ActivityResultLauncher<Array<String>>

    private var mIsExporting = false

    private val mPrefs by lazy {
        PreferenceManager.getDefaultSharedPreferences(context)
    }

    private val mKeyUtil by lazy {
        KeyUtil(context)
    }

    private fun resetPermissionLauncher() {
        //check permissions before trying to export the file
        requestPermissionsLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { granted ->

            //ensure all permissions are granted
            if (!granted.values.all { it }) {

                (activity as? MainActivity)?.notify(R.string.must_accept_permissions_to_export)

            } else {

                if (!mIsExporting) {

                    mIsExporting = true

                    context?.let { ctx ->

                        val fileName = "${mName}_${mDeviceType}_${DateUtil().getTime()}.csv"

                        if (DocumentTreeUtil.isEnabled(ctx)) {

                            DocumentTreeUtil.getDirectory(ctx, R.string.dir_exports)?.let { dir ->

                                if (dir.exists()) {

                                    dir.createFile("*/*", fileName)?.let { file ->

                                        if (file.exists()) {

                                            export(file.uri)

                                        }
                                    }
                                }
                            }

                        } else {

                            requestExportLauncher.launch(fileName)

                        }
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mSortState = mPrefs.getInt("last_samples_sort_state", DATE_DESC)

        mSnackbar = SnackbarQueue()

        resetPermissionLauncher()

        requestExportLauncher = registerForActivityResult(
            ActivityResultContracts.CreateDocument()) { nullUri ->

            nullUri?.let { uri ->

                export(uri)
            }
        }
    }

    private fun export(uri: Uri) {

        mIsExporting = false

        //start observing for exportable experiments using the defined view
        sViewModel.deviceTypeExports(mExpId).observeOnce(viewLifecycleOwner) {

            //only export if the view has rows, and only export the current selected experiment
            it?.let { exports ->

                //grab the first experiment as an example to find the name and device type for the filename
                val example = exports.firstOrNull()

                if (example == null) {

                    (activity as MainActivity).notify(getString(R.string.frag_sample_list_non_to_export))

                }

                example?.let { it ->

                    context?.let { ctx ->

                        val convert = mPrefs.getBoolean(CONVERT_TO_WAVELENGTHS, true)

                        mScope.launch {

                            mBinding?.toggleProgressBar()

                            if (it.deviceType in arrayOf(DEVICE_TYPE_LS1, DEVICE_TYPE_NIR)) {

                                FileUtil(ctx).exportCsv(uri, exports, convert)

                            } else {

                                FileUtil(ctx).exportCsv(uri, exports, false)
                            }

                            mBinding?.toggleProgressBar()

                            (activity as? MainActivity)?.showCitationDialog()

                        }
                    }
                }
            }
        }
    }

    private fun FragmentSampleListBinding.toggleProgressBar() {

        activity?.runOnUiThread {

            //make other elements the current vis of the progress bar
            val elementsVis = this.fragSampleListProgressBar.visibility

            arrayOf(this.fragSampleListSbv, this.toolbar, this.recyclerView, this.addSampleButton, this.fragSampleListSearchBtn).forEach {
                it.visibility = elementsVis
            }

            this.fragSampleListProgressBar.visibility = when (elementsVis) {
                View.VISIBLE -> View.GONE
                else -> View.VISIBLE
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        val eid = arguments?.getLong("experiment", -1L) ?: -1L
        val deviceType = arguments?.getString("deviceType", "") ?: ""
        val name = arguments?.getString("name", "") ?: ""

        if (eid != -1L) { //finish fragment if an invalid eid is given

            setHasOptionsMenu(true)

            mExpId = eid
            mDeviceType = deviceType
            mName = name

            val maker = mPrefs.getString(mKeyUtil.deviceMaker, DEVICE_TYPE_LS1) ?: DEVICE_TYPE_LS1
            if (maker !in deviceType) {

                if (DEVICE_TYPE_LS1 in deviceType) {

                    (activity as MainActivity).switchLinkSquare()

                } else {

                    (activity as MainActivity).switchInnoSpectra()
                }
            }

            val contextThemeWrapper = ContextThemeWrapper(activity, R.style.AppTheme)

            val localInflater = inflater.cloneInContext(contextThemeWrapper)

            mBinding = DataBindingUtil.inflate(localInflater, R.layout.fragment_sample_list, container, false)

            mBinding?.let { ui ->

                ui.setupRecyclerView()

                ui.setupButtons()

                ui.setupToolbar()

                startObservers()

                ui.fragSampleListSbv.initializeSortState(mSortState)

                ui.fragSampleListSbv.onClickSortType = { _ ->

                    mSortState = when (mSortState) {
                        ALPHA_ASC -> DATE_ASC
                        ALPHA_DESC -> DATE_DESC
                        DATE_ASC -> ALPHA_ASC
                        else -> ALPHA_DESC
                    }

                    notifySort()
                }

                ui.fragSampleListSbv.onClickSortOrder = { _ ->

                    mSortState = when (mSortState) {
                        ALPHA_ASC -> ALPHA_DESC
                        ALPHA_DESC -> ALPHA_ASC
                        DATE_ASC -> DATE_DESC
                        else -> DATE_ASC
                    }

                    notifySort()
                }

                return ui.root
            }

        } else findNavController().popBackStack()

        return null
    }

    private fun notifySort() {

        mPrefs.edit().putInt("last_samples_sort_state", mSortState).apply()

        (activity as? MainActivity)?.notify(when (mSortState) {
            ALPHA_ASC -> R.string.sort_alpha_ascending
            ALPHA_DESC -> R.string.sort_alpha_descending
            DATE_ASC -> R.string.sort_date_ascending
            else -> R.string.sort_date_descending
        })

        updateUi()
    }

    private fun FragmentSampleListBinding.setupToolbar() {

        toolbar.setNavigationOnClickListener {

            findNavController().popBackStack()

        }

        toolbar.setOnMenuItemClickListener { item ->

            when(item.itemId) {

                R.id.menu_export -> {
                    /**
                     * Uses activity results contracts to create a document and call the export function
                     * This method must queries for the device type export view and matches the exports with the
                     * current experiment id.
                     */
                    requestPermissionsLauncher.launch(arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE,
                        android.Manifest.permission.WRITE_EXTERNAL_STORAGE))

                }

                R.id.action_connection -> {

                    val deviceViewModel = (activity as MainActivity).sDeviceViewModel

                    with (activity as? MainActivity) {

                        if (this?.mConnected == true) {

                            deviceViewModel?.reset(context)
                        } else {

                            if (findNavController().currentDestination?.id == R.id.sample_list_fragment) {
                                findNavController().navigate(SampleListFragmentDirections
                                    .actionToConnectInstructions())
                            }

                            this?.startDeviceConnection()
                        }
                    }
                }
            }

            true
        }
    }

    private fun FragmentSampleListBinding.setupButtons() {

        onClick = if (mPrefs.getBoolean(mKeyUtil.sampleScanEnabled, false)) {
            sOnNewClickSampleScannerListener
        } else sOnNewClickListener

        fragSampleListSearchBtn.setOnClickListener {
            if (findNavController().currentDestination?.id == R.id.sample_list_fragment) {
                findNavController().navigate(SampleListFragmentDirections
                    .actionToBarcodeSearch(mExpId))
            }
        }
    }

    override fun onListItemLongClicked(sample: IndexedSampleScanCount) {

        if (findNavController().currentDestination?.id == R.id.sample_list_fragment) {
            findNavController().navigate(SampleListFragmentDirections
                .actionToNewSample(sample.eid, sample.name, sample.note))
        }
    }

    private fun FragmentSampleListBinding.setupRecyclerView() {

        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        recyclerView.adapter = SampleAdapter(requireContext(), this@SampleListFragment)

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
                                .currentList[viewHolder.absoluteAdapterPosition].also { s ->

                            mScope.launch {

                                deleteSample(Sample(s.eid, s.name))

                            }
                        }

                    } else mBinding?.recyclerView?.adapter?.notifyItemChanged(viewHolder.absoluteAdapterPosition)
                }
            }

        }).attachToRecyclerView(recyclerView)
    }

    private suspend fun deleteSample(sample: Sample) = withContext(Dispatchers.IO) {

        sViewModel.deleteSample(sample.eid, sample.name)

    }

    private fun startObservers() {

        //set the title header
        sViewModel.experiments.observe(viewLifecycleOwner) { experiments ->

            experiments.first { it.eid == mExpId }.also {

                activity?.runOnUiThread {

                    mBinding?.toolbar?.title = it.name

                }
            }
        }

        updateUi()

    }

    data class IndexedSampleScanCount(
        val index: Int,
        val eid: Long,
        val name: String,
        val date: String,
        val note: String,
        val count: Int
    )
    private val dummyRow = IndexedSampleScanCount(-1, -1, "", "", "", -1)
    private fun updateUi() {
        sViewModel.getSampleFramesCount(mExpId).observe(viewLifecycleOwner) { samples ->

            samples?.let { data ->

                val indexedData = data.mapIndexed { index, s ->
                    IndexedSampleScanCount(index, s.eid, s.name, s.date, s.note, s.count)
                }

                (mBinding?.recyclerView?.adapter as SampleAdapter)
                    .submitList(when (mSortState) {

                        DATE_DESC -> {

                            indexedData.sortedByDescending { it.date }
                        }

                        DATE_ASC -> {

                            indexedData.sortedBy { it.date }
                        }

                        ALPHA_DESC -> {

                            indexedData.sortedByDescending { it.name.lowercase() }
                        }

                        else -> {

                            indexedData.sortedBy { it.name.lowercase() }
                        }
                    } + listOf(dummyRow))

                Handler(Looper.getMainLooper()).postDelayed({

                    mBinding?.recyclerView?.scrollToPosition(0)

                }, 250)
            }
        }
    }

    override fun onResume() {
        super.onResume()

        (activity as? MainActivity)?.setToolbar(R.id.action_nav_data)

    }
}