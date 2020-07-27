package org.phenoapps.prospector.fragments

import android.net.Uri
import android.os.Bundle
import android.view.*
import android.widget.GridLayout
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.view.ContextThemeWrapper
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.stratiotechnology.linksquareapi.LSFrame
import com.stratiotechnology.linksquareapi.LinkSquareAPI
import kotlinx.coroutines.*
import org.phenoapps.prospector.R
import org.phenoapps.prospector.adapter.ScansAdapter
import org.phenoapps.prospector.data.ExperimentScansRepository
import org.phenoapps.prospector.data.ProspectorDatabase
import org.phenoapps.prospector.data.models.ExperimentScans
import org.phenoapps.prospector.data.models.Scan
import org.phenoapps.prospector.data.models.SpectralFrame
import org.phenoapps.prospector.data.viewmodels.DeviceViewModel
import org.phenoapps.prospector.data.viewmodels.ExperimentScansViewModel
import org.phenoapps.prospector.data.viewmodels.factory.ExperimentScanViewModelFactory
import org.phenoapps.prospector.databinding.FragmentScanListBinding
import org.phenoapps.prospector.utils.DateUtil
import org.phenoapps.prospector.utils.Dialogs
import org.phenoapps.prospector.utils.FileUtil
import org.phenoapps.prospector.utils.SnackbarQueue

/*
The scan activity should import, export, and scan new samples into an experiment
Experiment id's are passed as arguments to this activity. Argument named: "experiment" type: int
 */
class ScanListFragment : Fragment(), CoroutineScope by MainScope() {

    private val mSnackbar = SnackbarQueue()

    private val sDeviceViewModel: DeviceViewModel by viewModels()

    private val sViewModel: ExperimentScansViewModel by viewModels {

        ExperimentScanViewModelFactory(
                ExperimentScansRepository.getInstance(
                        ProspectorDatabase.getInstance(requireContext()).expScanDao()
                )
        )
    }

    private var mBinding: FragmentScanListBinding? = null

    private var mExpId: Long = -1L

    private lateinit var mDeviceInfo: LinkSquareAPI.LSDeviceInfo

    private var mExpScans: List<ExperimentScans> = ArrayList()

    private suspend fun export(uri: Uri) = withContext(Dispatchers.IO) {

        val scanMap = mutableMapOf<ExperimentScans, List<SpectralFrame>>()
                .withDefault { ArrayList() }

        mExpScans.forEach { exp ->

            exp.sid?.let { sid ->

                val frames = sViewModel.spectralFrames(exp.eid, sid)

                scanMap[exp] = frames

            }
        }

        FileUtil(requireContext()).export(uri, scanMap)

    }

    private val exportScans by lazy {

        registerForActivityResult(ActivityResultContracts.CreateDocument()) { uri ->

            //check if uri is null or maybe throws an exception

            launch {

                export(uri)

            }

        }

    }

    private val importScans by lazy {

        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->

            val scans = FileUtil(requireContext()).parseInputFile(mExpId, uri)

            scans.keys.forEach { key ->

                CoroutineScope(Dispatchers.IO).launch {

                    sViewModel.insertScan(key)

                    scans[key]?.forEach { frame ->

                        sViewModel.insertFrame(key.sid, frame)

                    }

                }

            }
        }
    }

    private val sOnClickDelete = View.OnClickListener {

        if (mExpId != -1L) {

            sViewModel.deleteExperiment(mExpId)

            findNavController().popBackStack()

        }

    }

    private suspend fun getSpecFrames(scan: Scan): List<SpectralFrame> {

        return withContext(Dispatchers.IO) {

            return@withContext sViewModel.spectralFrames(scan.eid, scan.sid)

        }
    }

    private fun insertScan(scanId: String, frames: List<LSFrame>) {

        CoroutineScope(Dispatchers.IO).async {

            sViewModel.insertScan(Scan(mExpId, scanId).apply {

                if (::mDeviceInfo.isInitialized) {

                    deviceId = mDeviceInfo.DeviceID

                }

            })

            frames.map {
                SpectralFrame(
                        scanId,
                        it.frameNo,
                        it.length,
                        it.raw_data.joinToString(" ") { value -> value.toString() },
                        it.lightSource.toInt())
            }.forEach { frame ->

                sViewModel.insertFrame(scanId, frame)

            }

        }
    }

    private fun reinsertScan(scan: Scan, frames: List<SpectralFrame>) {

        CoroutineScope(Dispatchers.IO).async {

            sViewModel.insertScan(scan)

            frames.forEach { frame ->

                sViewModel.insertFrame(scan.sid, frame)

            }

        }

    }

    private val sOnClickScan = View.OnClickListener {

        callInsertDialog()

    }

    private fun registerLinkSquareButtonCallback() {

        sDeviceViewModel.onClick(requireContext()) {

            requireActivity().runOnUiThread {

                callInsertDialog()

            }

        }

    }

    private fun callInsertDialog() {

        Dialogs.askForName(requireActivity(), R.string.ask_new_scan_name, R.string.ok) { scanId ->

            sDeviceViewModel.scan(requireContext()).observe(viewLifecycleOwner, Observer {

                it?.let {  frames ->

                    insertScan(scanId, frames)

                }

            })

        }
    }

    private fun disconnectDeviceAsync() {

        sDeviceViewModel.disconnect()

    }

    //TODO move connection to shared view model that begins in Experiment list page
    private fun connectDeviceAsync() {

        sDeviceViewModel.connection(requireContext()).observe(viewLifecycleOwner, Observer {

            it?.let {

                when (it) {

                    LinkSquareAPI.RET_OK -> {

                        sDeviceViewModel.getDeviceInfo()?.let { info ->

                            mDeviceInfo = info

                        }

                        registerLinkSquareButtonCallback()

                        requireActivity().runOnUiThread {

                            mBinding?.let { ui ->

                                mSnackbar.push(SnackbarQueue.SnackJob(ui.root, getString(R.string.connected)))

                                ui.deviceTextView.text = buildLinkSquareDeviceInfo(mDeviceInfo)

                            }
                        }
                    }
                }
            }
        })
    }

    override fun onDestroyView() {

        super.onDestroyView()

        async {

            disconnectDeviceAsync()

        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        async {

            connectDeviceAsync()

        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        val contextThemeWrapper = ContextThemeWrapper(activity, R.style.AppTheme)

        val localInflater = inflater.cloneInContext(contextThemeWrapper)

        mBinding = DataBindingUtil.inflate(localInflater, R.layout.fragment_scan_list, container, false)

        mBinding?.let { ui ->

            ui.deleteOnClick = sOnClickDelete

            ui.scanOnClick = sOnClickScan

            ui.expDate = getString(R.string.loading)

            ui.expName = getString(R.string.loading)
        }


        //check if experiment id is included in the arguments.
        val eid = arguments?.getLong("experiment", -1L) ?: -1L

        if (eid != -1L) {

            mExpId = eid

            mBinding?.let { ui ->

                ui.recyclerView.adapter = ScansAdapter(this, requireContext(), sViewModel)

                val undoString = getString(R.string.undo)

                ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT) {

                    override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
                        return false
                    }

                    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {

                        launch {

                            (ui.recyclerView.adapter as ScansAdapter)
                                    .currentList[viewHolder.adapterPosition].also { scan ->

                                val frames = getSpecFrames(scan)

                                sViewModel.deleteScan(scan)

                                mSnackbar.push(SnackbarQueue.SnackJob(ui.root, scan.sid, undoString) {

                                    reinsertScan(scan, frames)

                                })

                            }
                        }
                    }

                }).attachToRecyclerView(ui.recyclerView)

                startObservers()

            }
        }

        setHasOptionsMenu(true)

        return mBinding?.root
    }

    private fun buildLinkSquareDeviceInfo(data: LinkSquareAPI.LSDeviceInfo?): String {

        data?.let { info ->

            val aliasHeader = getString(R.string.alias_header)
            val deviceIdHeader = getString(R.string.device_id_header)
            val deviceTypeHeader = getString(R.string.device_type_header)
            val hwVersion = getString(R.string.hw_version_header)
            val opMode = getString(R.string.op_mode_header)
            val swVersion = getString(R.string.sw_version_header)

            return """
            ${aliasHeader}: ${info.Alias}      
            ${deviceIdHeader}: ${info.DeviceID}          
            ${deviceTypeHeader}: ${info.DeviceType}       
            ${hwVersion}: ${info.HWVersion}        
            ${opMode}: ${info.OPMode}        
            ${swVersion}: ${info.SWVersion}
        """.trimIndent()

        }

        return "None"
    }

    private fun startObservers() {

        sViewModel.getScans(mExpId).observe(viewLifecycleOwner, Observer { data ->

            if (data.isNotEmpty()) {

                mExpScans = data

                val exp = data.first()

                mBinding?.let { ui ->

                    ui.expDate = exp.expDate

                    ui.expName = exp.expName

                    (ui.recyclerView.adapter as ScansAdapter).submitList(data.map {

                        it.sid?.let { sid ->

                            Scan(exp.eid, sid)
                                    .apply {
                                        this.deviceId = it.deviceId
                                        this.date = it.scanDate
                                    }

                        }

                    }.mapNotNull { it })

                    ui.executePendingBindings()

                }
            }
        })
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {

        inflater.inflate(R.menu.menu_scan_list, menu)

        super.onCreateOptionsMenu(menu, inflater)

    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        when (item.itemId) {

            R.id.import_scans -> importScans.launch("*/*")

            R.id.export_scans -> {

                val defaultFileNamePrefix = getString(R.string.default_csv_export_file_name)

                exportScans.launch("${defaultFileNamePrefix}_${DateUtil().getTime()}.csv")
            }

        }

        return super.onOptionsItemSelected(item)
    }
}