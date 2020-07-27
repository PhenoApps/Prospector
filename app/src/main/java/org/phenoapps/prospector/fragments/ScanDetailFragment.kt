package org.phenoapps.prospector.fragments

import android.os.Bundle
import android.view.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.view.ContextThemeWrapper
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.observe
import androidx.navigation.fragment.findNavController
import com.jjoe64.graphview.GraphView
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
import org.phenoapps.prospector.databinding.FragmentDetailScanBinding
import org.phenoapps.prospector.databinding.FragmentScanListBinding
import org.phenoapps.prospector.utils.AsyncLoadGraph
import org.phenoapps.prospector.utils.Dialogs
import org.phenoapps.prospector.utils.FileUtil


class ScanDetailFragment : Fragment(), CoroutineScope by MainScope() {

    private lateinit var sViewModel: ExperimentScansViewModel

    private lateinit var mBinding: FragmentDetailScanBinding

    private var mExpId: Long = -1L

    private var mScanId: String = String()

    private val sOnClickDelete = View.OnClickListener {

        if (::sViewModel.isInitialized && mExpId != -1L) {

            sViewModel.deleteExperiment(mExpId)

            findNavController().popBackStack()

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

        mBinding = DataBindingUtil.inflate(localInflater, R.layout.fragment_detail_scan, container, false)


        //check if experiment id is included in the arguments.
        val eid = arguments?.getLong("experiment", -1L) ?: -1L

        val sid = arguments?.getString("scan", String()) ?: String()

        if (eid != -1L && sid.isNotBlank()) {

            mExpId = eid

            mScanId = sid

            loadGraph()

            startObservers()

            mBinding.toggleButton.setOnClickListener {

                loadGraph()

            }
        }

        setHasOptionsMenu(true)

        return mBinding.root
    }

    private fun loadGraph() {

        sViewModel.forceSpectralValues(mExpId, mScanId).observe(viewLifecycleOwner) { frames: List<SpectralFrame> ->

            AsyncLoadGraph(requireContext(),
                    mBinding.graphView,
                    mScanId,
                    requireContext().getString(R.string.horizontal_axis),
                    requireContext().getString(R.string.vertical_axis),
                    frames,
                    false,
                    mBinding.toggleButton.isChecked).execute()

        }

    }

    private fun startObservers() {

        sViewModel.getSpectralValues(mExpId, mScanId).observe(viewLifecycleOwner, Observer { data ->

            if (data.isNotEmpty()) {

                val exp = data.first()

                mBinding.scanDate = exp.scanDate

                mBinding.scanName = exp.sid

            }
        })
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {

        //inflater.inflate(R.menu.menu_scan_list, menu)

        super.onCreateOptionsMenu(menu, inflater)

    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

//        when (item.itemId) {
//
//            R.id.import_scans -> importScans.launch("*/*")
//
//            R.id.export_scans -> null
//
//        }

        return super.onOptionsItemSelected(item)
    }
}