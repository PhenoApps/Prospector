package org.phenoapps.prospector.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.view.ContextThemeWrapper
import androidx.core.os.bundleOf
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.WithFragmentBindings
import org.phenoapps.prospector.R
import org.phenoapps.prospector.databinding.FragmentInnoSpectraNewSectionCreatorBinding

@WithFragmentBindings
@AndroidEntryPoint
class InnoSpectraNewSectionFragment : Fragment() {

    private var mBinding: FragmentInnoSpectraNewSectionCreatorBinding? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        val contextThemeWrapper = ContextThemeWrapper(activity, R.style.AppTheme)

        val localInflater = inflater.cloneInContext(contextThemeWrapper)

        mBinding = DataBindingUtil.inflate(localInflater, R.layout.fragment_inno_spectra_new_section_creator, null, false)

        mBinding?.fragNewConfigAddSectionBtn?.setOnClickListener {

            val type = if (mBinding?.fragNewSectionColumnChip?.isChecked == true) 0 else 1
            val width = (mBinding?.fragNewSectionWidthSp?.selectedItem as String).toFloat()
            val widthIndex = mBinding?.fragNewSectionWidthSp?.selectedItemPosition
            val exp = (mBinding?.fragNewSectionExposureTimeSp?.selectedItem as String).toFloat()
            val expIndex = mBinding?.fragNewSectionExposureTimeSp?.selectedItemPosition
            val start = mBinding?.fragNewSectionSpectralRangeSl?.values?.get(0)
            val end = mBinding?.fragNewSectionSpectralRangeSl?.values?.get(1)
            val res = mBinding?.fragNewSectionDigitalResolutionSl?.value?.toInt()

            setFragmentResult("section", bundleOf(
                "type" to type,
                "width" to width,
                "widthIndex" to widthIndex,
                "exp" to exp,
                "expIndex" to expIndex,
                "start" to start,
                "end" to end,
                "res" to res))

            findNavController().popBackStack()
        }

        return mBinding?.root
    }
}