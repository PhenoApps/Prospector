package org.phenoapps.prospector.fragments

import android.os.Bundle
import android.view.*
import androidx.appcompat.view.ContextThemeWrapper
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.phenoapps.prospector.R
import org.phenoapps.prospector.data.ProspectorDatabase
import org.phenoapps.prospector.data.ProspectorRepository
import org.phenoapps.prospector.data.models.SpectralFrame
import org.phenoapps.prospector.data.viewmodels.ExperimentSamplesViewModel
import org.phenoapps.prospector.data.viewmodels.factory.ExperimentSamplesViewModelFactory
import org.phenoapps.prospector.databinding.FragmentDetailScanBinding
import org.phenoapps.prospector.utils.*


class ScanDetailFragment : Fragment(), CoroutineScope by MainScope() {

    private lateinit var sViewModel: ExperimentSamplesViewModel

    private lateinit var mBinding: FragmentDetailScanBinding

    private var mExpId: Long = -1L

    private var mSample: String = String()

    private var mScanId: Long = -1L

    private var mCentered: Boolean = true

    private val sOnClickDelete = View.OnClickListener {

        if (::sViewModel.isInitialized && mExpId != -1L) {

            sViewModel.deleteExperiment(mExpId)

            findNavController().popBackStack()

        }

    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        val viewModel: ExperimentSamplesViewModel by viewModels {

            ExperimentSamplesViewModelFactory(
                    ProspectorRepository.getInstance(
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

        val sample = arguments?.getString("sample", String()) ?: String()

        val sid = arguments?.getLong("scan", -1L) ?: -1L

        if (eid != -1L && sample.isNotBlank() && sid != -1L) {

            mExpId = eid

            mSample = sample

            mScanId = sid

            launch {

                loadGraph()

            }

        }

        setHasOptionsMenu(true)

        return mBinding.root
    }


    private fun loadGraph() {

        sViewModel.getSpectralValuesLive(mExpId, mScanId).observe(viewLifecycleOwner, Observer<List<SpectralFrame>> {

            it?.let { data ->

                if (data.isNotEmpty()) {

                    val wavelengths = it.toWaveArray()

                    setViewportGrid(mBinding.graphView)

                    centerViewport(mBinding.graphView, wavelengths)

                    setViewportScalable(mBinding.graphView)

                    renderNormal(mBinding.graphView, wavelengths)

                }
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