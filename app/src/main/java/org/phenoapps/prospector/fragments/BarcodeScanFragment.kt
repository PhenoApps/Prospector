package org.phenoapps.prospector.fragments

import android.Manifest
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.view.ContextThemeWrapper
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.google.zxing.ResultPoint
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeResult
import org.phenoapps.prospector.R
import org.phenoapps.prospector.data.ProspectorDatabase
import org.phenoapps.prospector.data.ProspectorRepository
import org.phenoapps.prospector.data.models.Sample
import org.phenoapps.prospector.data.viewmodels.ExperimentSamplesViewModel
import org.phenoapps.prospector.data.viewmodels.factory.ExperimentSamplesViewModelFactory
import org.phenoapps.prospector.databinding.FragmentBarcodeScanBinding

class BarcodeScanFragment : Fragment() {

    private val sSamples: ExperimentSamplesViewModel by viewModels {

        ExperimentSamplesViewModelFactory(
                ProspectorRepository.getInstance(
                        ProspectorDatabase.getInstance(requireContext())
                                .expScanDao()))

    }

    private var mSamples: List<Sample> = ArrayList<Sample>()

    private var mExpId = -1L

    private var mBinding: FragmentBarcodeScanBinding? = null

    private val checkCamPermissions by lazy {

        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->

            if (granted) {

                setupBarcodeScanner()

            }
        }
    }

    private fun setupBarcodeScanner() {

        mBinding?.barcodeScanner.apply {

            this?.barcodeView?.let { scanner ->

                with(scanner) {

                    cameraSettings.isContinuousFocusEnabled = true

                    cameraSettings.isAutoTorchEnabled = true

                    cameraSettings.isAutoFocusEnabled = true

                    cameraSettings.isBarcodeSceneModeEnabled = true

                    decodeSingle(object : BarcodeCallback {

                        override fun barcodeResult(result: BarcodeResult) {

                            if (result.text == null) return // || result.text == lastText) return

                            mSamples.find { it.name == result.text.toString() }?.name?.let { name ->

                                findNavController().navigate(BarcodeScanFragmentDirections
                                        .actionToScanList(mExpId, name))

                            }

                        }

                        override fun possibleResultPoints(resultPoints: List<ResultPoint>) {

                        }

                    })
                }
            }
        }
    }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        val eid = arguments?.getLong("experiment", -1L) ?: -1L

        if (eid != -1L) {

            val contextThemeWrapper = ContextThemeWrapper(activity, R.style.AppTheme)

            val localInflater = inflater.cloneInContext(contextThemeWrapper)

            mExpId = eid

            mBinding = DataBindingUtil.inflate(localInflater, R.layout.fragment_barcode_scan, null, false)

            checkCamPermissions.launch(Manifest.permission.CAMERA)

            setupBarcodeScanner()

            startObservers()

            return mBinding?.root

        } else findNavController().popBackStack()

        return null
    }

    private fun startObservers() {

        sSamples.getSamples(mExpId).observe(viewLifecycleOwner, Observer {

            it?.let { samples ->

                mSamples = samples

            }
        })
    }

    override fun onResume() {
        super.onResume()

        mBinding?.barcodeScanner?.resume()
    }

    override fun onPause() {
        super.onPause()

        mBinding?.barcodeScanner?.pause()
    }

}