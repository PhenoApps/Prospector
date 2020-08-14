package org.phenoapps.prospector.fragments

import DEVICE_TYPE
import OPERATOR
import android.os.Bundle
import android.view.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.stratiotechnology.linksquareapi.LSFrame
import com.stratiotechnology.linksquareapi.LinkSquareAPI
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
import org.phenoapps.prospector.utils.Dialogs
import org.phenoapps.prospector.utils.FileUtil
import org.phenoapps.prospector.utils.SnackbarQueue

/*
The scan activity should import, export, and scan new samples into an experiment
Experiment id's are passed as arguments to this activity. Argument named: "experiment" type: int
 */
class ScanListFragment : Fragment(), CoroutineScope by MainScope() {

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

    private lateinit var mDeviceInfo: LinkSquareAPI.LSDeviceInfo

//    private var mExpScans: List<ExperimentScans> = ArrayList()

    private val importScans by lazy {

        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->

            launch {

                withContext(Dispatchers.IO) {

                    FileUtil(requireContext()).parseInputFile(mExpId, uri, sViewModel)

                }
            }
        }
    }

    private fun insertScan(name: String, frames: List<LSFrame>) {

        val prefs = PreferenceManager.getDefaultSharedPreferences(requireContext())

        launch {

            frames.forEach { frame ->

                val sid = sViewModel.insertScan(Scan(mExpId, name).apply {

                    if (::mDeviceInfo.isInitialized) {

                        deviceId = mDeviceInfo.DeviceID

                    }

                    this.deviceType = prefs.getString(DEVICE_TYPE, "LinkSquare") ?: "LinkSquare"

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

        callScanDialog()

    }

    private fun callScanDialog() {

        val dialog = Dialogs.askForScan(requireActivity(), R.string.scanning_sample, R.string.ok, R.string.close) {

        }

        dialog.create()

        val dialogInterface = dialog.show()

        sDeviceViewModel.scan(requireContext()).observe(viewLifecycleOwner, Observer {

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

            if (eid != -1L && name.isNotBlank()) {

                mExpId = eid

                mSampleName = name

                ui.sampleName = mSampleName

                ui.recyclerView.adapter = ScansAdapter(requireContext(), sViewModel)

                val undoString = getString(R.string.undo)

                ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT) {

                    override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
                        return false
                    }

                    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {

                        Dialogs.onOk(androidx.appcompat.app.AlertDialog.Builder(requireContext()), getString(R.string.ask_delete_scan), getString(R.string.cancel), getString(R.string.ok)) { result ->

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

                        updateUi()
                    }

                }).attachToRecyclerView(ui.recyclerView)

                startObservers()

            }
        }

        setHasOptionsMenu(true)

        return mBinding?.root
    }

    private fun startObservers() {

        updateUi()

        sDeviceViewModel.setEventListener {

            requireActivity().runOnUiThread {

                callScanDialog()

            }

        }.observe(viewLifecycleOwner, Observer {

        })
    }

    private fun updateUi() {

        mSampleName?.let { name ->

            sViewModel.getScans(mExpId, name).observe(viewLifecycleOwner, Observer { data ->

                if (data.isNotEmpty()) {

                    mBinding?.let { ui ->

                        with (ui.recyclerView.adapter as ScansAdapter) {

                            submitList(data)

                        }

                        ui.executePendingBindings()

                    }
                } else (mBinding?.recyclerView?.adapter as? ScansAdapter)?.submitList(data)
            })
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {

        inflater.inflate(R.menu.menu_scan_list, menu)

        super.onCreateOptionsMenu(menu, inflater)

    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        when (item.itemId) {

            R.id.delete_scans -> Dialogs.onOk(androidx.appcompat.app.AlertDialog.Builder(requireContext()), getString(R.string.ask_delete_all_scans), getString(R.string.cancel), getString(R.string.ok)) {

                if (it) {

                    mSampleName?.let { sample ->

                        launch {

                            sViewModel.deleteScans(mExpId, sample)

                            updateUi()
                        }
                    }
                }
            }
        }

        return super.onOptionsItemSelected(item)
    }
}