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
import androidx.navigation.fragment.findNavController
import com.google.zxing.ResultPoint
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeResult
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.WithFragmentBindings
import org.phenoapps.prospector.R
import org.phenoapps.prospector.activities.MainActivity
import org.phenoapps.prospector.data.models.Sample
import org.phenoapps.prospector.data.viewmodels.SampleViewModel
import org.phenoapps.prospector.databinding.FragmentBarcodeScanBinding
import org.phenoapps.prospector.utils.MediaUtil

/**
 * Similar to the barcode scan fragment, this loads all current samples and searches
 * for a sample name that matches the scanned barcode. If it exists the fragment
 * immediately navigates to the sample specific page.
 */
@WithFragmentBindings
@AndroidEntryPoint
class BarcodeSearchFragment : Fragment() {

    private val sViewModel: SampleViewModel by viewModels()

    private var mSamples: List<Sample> = ArrayList()

    private var mExpId = -1L

    private var mBinding: FragmentBarcodeScanBinding? = null

    private val checkCamPermissions by lazy {

        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->

            if (granted) {

                setupBarcodeScanner()

            }
        }
    }

    private val mMediaUtil by lazy {
        MediaUtil(activity)
    }

    private fun setupBarcodeScanner() {

        mBinding?.barcodeScanner.apply {

            this?.barcodeView?.let { scanner ->

                with(scanner) {

                    cameraSettings.isContinuousFocusEnabled = true

                    cameraSettings.isAutoTorchEnabled = true

                    cameraSettings.isAutoFocusEnabled = true

                    cameraSettings.isBarcodeSceneModeEnabled = true

                    decodeContinuous(object : BarcodeCallback {

                        override fun barcodeResult(result: BarcodeResult) {

                            if (result.text == null) return // || result.text == lastText) return

                            if (mSamples.any { it.name == result.text.toString() }) {

                                mMediaUtil.play(MediaUtil.BARCODE_SCAN)

                                mSamples.find { it.name == result.text.toString() }?.name?.let { name ->

                                    if (findNavController().currentDestination?.id == R.id.barcode_search_fragment) {
                                        findNavController().navigate(BarcodeSearchFragmentDirections
                                            .actionToScanList(mExpId, name))
                                    }
                                }
                            } else {

                                mMediaUtil.play(MediaUtil.BARCODE_SEARCH_FAIL)

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

        //error checking to ensure this fragment was called correctly
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

    //listens for the current samples list
    private fun startObservers() {

        sViewModel.getSamplesLive(mExpId).observe(viewLifecycleOwner, {

            it?.let { samples ->

                mSamples = samples

            }
        })
    }

    override fun onResume() {
        super.onResume()

        mBinding?.barcodeScanner?.resume()

        (activity as? MainActivity)?.setToolbar(R.id.action_nav_data)

    }

    override fun onPause() {
        super.onPause()

        mBinding?.barcodeScanner?.pause()
    }

}