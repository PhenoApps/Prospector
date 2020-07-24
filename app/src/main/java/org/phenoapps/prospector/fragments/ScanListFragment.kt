package org.phenoapps.prospector.fragments

import android.os.Bundle
import android.view.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.view.ContextThemeWrapper
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.stratiotechnology.linksquareapi.LinkSquareAPI
import kotlinx.coroutines.*
import org.phenoapps.prospector.R
import org.phenoapps.prospector.adapter.ScansAdapter
import org.phenoapps.prospector.data.ExperimentScansRepository
import org.phenoapps.prospector.data.ProspectorDatabase
import org.phenoapps.prospector.data.models.Scan
import org.phenoapps.prospector.data.models.SpectralFrame
import org.phenoapps.prospector.data.viewmodels.ExperimentScansViewModel
import org.phenoapps.prospector.data.viewmodels.factory.ExperimentScanViewModelFactory
import org.phenoapps.prospector.databinding.FragmentScanListBinding
import org.phenoapps.prospector.utils.Dialogs
import org.phenoapps.prospector.utils.FileUtil
import org.phenoapps.prospector.utils.SnackbarQueue

/*
The scan activity should import, export, and scan new samples into an experiment
Experiment id's are passed as arguments to this activity. Argument named: "experiment" type: int
 */
class ScanListFragment : Fragment(), CoroutineScope by MainScope() {

    private val mSnackbar = SnackbarQueue()

    private lateinit var sViewModel: ExperimentScansViewModel

    private lateinit var mBinding: FragmentScanListBinding

    private var mExpId: Long = -1L

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

        if (::sViewModel.isInitialized && mExpId != -1L) {

            sViewModel.deleteExperiment(mExpId)

            findNavController().popBackStack()

        }

    }

    private suspend fun getSpecFrames(scan: Scan): List<SpectralFrame> {

        return withContext(Dispatchers.IO) {

            return@withContext sViewModel.spectralFrames(scan.eid, scan.sid)

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

    //TODO redo connection to API,
    private suspend fun connect(): Int {

        return withContext(Dispatchers.IO) {

            return@withContext sDevice.Connect("192.168.1.1", 18630)

        }

    }

    private val sOnClickScan = View.OnClickListener {

        Dialogs.askForName(requireActivity(), R.string.ask_new_scan_name, R.string.ok) {

            val attemptConnect = getString(R.string.attempting_connect)

            mBinding.dateView.text = attemptConnect


            launch {

                var connected = false

                var maxTries = 3

                var tries = 1

                while (!connected && tries <= maxTries) {

                    //link square api is not asynchronous, the methods cannot be suspended
                    ///var result = async { connect() }

                    when(connect()) {

                        1 -> connected = true

                        else -> {

                            mBinding.dateView.text = "$attemptConnect [$tries/$maxTries]"

                            tries++

                        }
                    }

                    if (connected) break

                }

                if (connected) {

                    mBinding.dateView.text = buildLinkSquareDeviceInfo(sDevice.GetDeviceInfo())

                } else {

                    mBinding.dateView.text = sDevice.GetLSError()

                }
            }

        }

    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        val viewModel: ExperimentScansViewModel by viewModels {

            ExperimentScanViewModelFactory(
                    ExperimentScansRepository.getInstance(
                            ProspectorDatabase.getInstance(requireContext()).expScanDao()
                    )
            )
        }

        sViewModel = viewModel

        val contextThemeWrapper = ContextThemeWrapper(activity, R.style.AppTheme)

        val localInflater = inflater.cloneInContext(contextThemeWrapper)

        mBinding = DataBindingUtil.inflate(localInflater, R.layout.fragment_scan_list, container, false)

        with (mBinding) {

            deleteOnClick = sOnClickDelete

            scanOnClick = sOnClickScan

        }

        //check if experiment id is included in the arguments.
        val eid = arguments?.getLong("experiment", -1L) ?: -1L

        if (eid != -1L) {

            mExpId = eid

            mBinding.recyclerView.adapter = ScansAdapter(this, requireContext(), sViewModel)

            val undoString = getString(R.string.undo)

            ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT) {

                override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
                    return false
                }

                override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {

                    launch {

                        (mBinding.recyclerView.adapter as ScansAdapter)
                                .currentList[viewHolder.adapterPosition].also { scan ->

                            val frames = getSpecFrames(scan)

                            viewModel.deleteScan(scan)

                            mSnackbar.push(SnackbarQueue.SnackJob(mBinding.root, scan.sid, undoString) {

                                reinsertScan(scan, frames)

                            })

                        }
                    }
                }
            }).attachToRecyclerView(mBinding.recyclerView)

            startObservers()

        }

        setHasOptionsMenu(true)

        return mBinding.root
    }

    private fun buildLinkSquareDeviceInfo(info: LinkSquareAPI.LSDeviceInfo): String {

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

    init {

        System.loadLibrary("LinkSquareAPI")
    }

    private val sDevice by lazy {

        LinkSquareAPI.getInstance()

    }

    private fun startObservers() {

        sViewModel.getScans(mExpId).observe(viewLifecycleOwner, Observer { data ->

            if (data.isNotEmpty()) {

                val exp = data.first()

                mBinding.expDate = exp.expDate

                mBinding.expName = exp.expName

                (mBinding.recyclerView.adapter as ScansAdapter).submitList(data.map {

                    it.sid?.let { sid ->

                        Scan(exp.eid, sid)
                                .apply {
                                    this.deviceId = it.deviceId
                                    this.date = it.scanDate
                                }

                    }
                }.mapNotNull { it })
            }
        })

        sDevice.Initialize()

    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {

        inflater.inflate(R.menu.menu_scan_list, menu)

        super.onCreateOptionsMenu(menu, inflater)

    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        when (item.itemId) {

            R.id.import_scans -> importScans.launch("*/*")

            R.id.export_scans -> null//TODO

        }

        return super.onOptionsItemSelected(item)
    }
}