package org.phenoapps.prospector.fragments

import android.Manifest
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.view.ContextThemeWrapper
import androidx.core.os.bundleOf
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.navigation.fragment.findNavController
import com.google.zxing.ResultPoint
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeResult
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.WithFragmentBindings
import org.phenoapps.prospector.R
import org.phenoapps.prospector.databinding.FragmentBarcodeScanBinding

/**
 * A barcode fragment that uses Zebra SDK. Specifically, this fragment returns the first scanned
 * barcode as a fragment result. The bundle includes the key "barcode_result" which is a String.
 */
@WithFragmentBindings
@AndroidEntryPoint
class BarcodeScanFragment : Fragment() {

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

                    /**
                     * This is where the barcode result callback occurs and the fragment returns on success.
                     */
                    decodeSingle(object : BarcodeCallback {

                        override fun barcodeResult(result: BarcodeResult) {

                            if (result.text == null) return // || result.text == lastText) return

                            setFragmentResult("BarcodeResult",
                                bundleOf("barcode_result" to result.text.toString()))

                            findNavController().popBackStack()

                        }

                        override fun possibleResultPoints(resultPoints: List<ResultPoint>) {

                        }

                    })
                }
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        val contextThemeWrapper = ContextThemeWrapper(activity, R.style.AppTheme)

        val localInflater = inflater.cloneInContext(contextThemeWrapper)

        mBinding = DataBindingUtil.inflate(localInflater, R.layout.fragment_barcode_scan, null, false)

        checkCamPermissions.launch(Manifest.permission.CAMERA)

        setupBarcodeScanner()

        return mBinding?.root

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