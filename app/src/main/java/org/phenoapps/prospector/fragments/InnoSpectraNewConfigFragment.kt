package org.phenoapps.prospector.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.view.ContextThemeWrapper
import androidx.core.os.bundleOf
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.setFragmentResultListener
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.WithFragmentBindings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.phenoapps.prospector.R
import org.phenoapps.prospector.activities.MainActivity
import org.phenoapps.prospector.data.viewmodels.devices.InnoSpectraViewModel
import org.phenoapps.prospector.databinding.FragmentInnoSpectraNewConfigCreatorBinding

@WithFragmentBindings
@AndroidEntryPoint
class InnoSpectraNewConfigFragment : Fragment(), CoroutineScope by MainScope() {

    private var mBinding: FragmentInnoSpectraNewConfigCreatorBinding? = null

    private var mSections = ArrayList<Section>()

    data class Section(val type: Int, val widthIndex: Int, val width: Float, val start: Float,
                               val end: Float, val expIndex: Int, val exposure: Float, val resolution: Int) {
        override fun toString(): String {

            return "$type $width $start $end $exposure $resolution"
        }
    }

    data class Config(val name: String, val repeats: Int, val sections: Array<Section>) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Config

            if (name != other.name) return false
            if (repeats != other.repeats) return false
            if (!sections.contentEquals(other.sections)) return false

            return true
        }

        override fun hashCode(): Int {
            var result = name.hashCode()
            result = 31 * result + repeats
            result = 31 * result + sections.contentHashCode()
            return result
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        val contextThemeWrapper = ContextThemeWrapper(activity, R.style.AppTheme)

        val localInflater = inflater.cloneInContext(contextThemeWrapper)

        mBinding = DataBindingUtil.inflate(localInflater, R.layout.fragment_inno_spectra_new_config_creator, null, false)

        mBinding?.fragNewConfigAddSectionBtn?.setOnClickListener {

            findNavController().navigate(InnoSpectraNewConfigFragmentDirections
                .actionToNewSectionCreator())
        }

        setFragmentResultListener("section") { _, bundle ->

            mSections.add(Section(bundle.getInt("type", 0),
                bundle.getInt("widthIndex", 0),
                bundle.getFloat("width", 0f),
                bundle.getFloat("start", 900f),
                bundle.getFloat("end", 1700f),
                bundle.getInt("expIndex", 0),
                bundle.getFloat("exp", 0f),
                bundle.getInt("res", 3)
            ))

            updateSectionsList()
        }

        mBinding?.fragNewConfigAddConfigBtn?.setOnClickListener {

            context?.let { ctx ->

                val deviceViewModel = (activity as MainActivity).sDeviceViewModel as InnoSpectraViewModel

                val name = mBinding?.fragNewConfigNameEt?.text?.toString() ?: ""
                val repeats = mBinding?.fragNewConfigRepeatsEt?.text?.toString()?.toIntOrNull() ?: 1

                val configs = deviceViewModel.getScanConfigs()

                if (repeats in 1..65535) {

                    if (mSections.size in 1..5) {

                        if (name.isNotBlank() && !configs.any { it.configName == name }) {

                            val totalRes = mSections.map { it.resolution }.sum()

                            if (totalRes < 2 || totalRes > 624) {

                                Toast.makeText(context, R.string.frag_new_config_resolution_to_high,
                                    Toast.LENGTH_SHORT).show()

                            } else {

                                mBinding?.fragNewConfigAddConfigBtn?.visibility = View.GONE
                                mBinding?.fragNewConfigPb?.visibility = View.VISIBLE

                                launch {

                                    deviceViewModel.addConfig(Config(name, repeats, mSections.toTypedArray()))

                                    val start = System.currentTimeMillis()
                                    val current = System.currentTimeMillis()
                                    var status = deviceViewModel.getConfigSaved()
                                    while (status == null) {
                                        delay(1000)
                                        status = deviceViewModel.getConfigSaved()

                                        if ((current - start) > 10000) status = false
                                    }

                                    activity?.runOnUiThread {

                                        deviceViewModel.resetConfigSaved()

                                        if (status == true) {

                                            setFragmentResult("new_config", bundleOf())

                                        }

                                        findNavController().popBackStack()
                                    }
                                }
                            }

                        } else {

                            Toast.makeText(context, R.string.frag_new_config_name_empty_or_reused,
                                Toast.LENGTH_SHORT).show()
                        }
                    } else {

                        Toast.makeText(context, R.string.frag_new_config_section_range_failed,
                            Toast.LENGTH_SHORT).show()
                    }
                } else {

                    Toast.makeText(context, R.string.frag_new_config_repeats_range_failed,
                        Toast.LENGTH_SHORT).show()
                }
            }
        }

        return mBinding?.root
    }

    private fun updateSectionsList() {

        context?.let { ctx ->

            val typeHeader = getString(R.string.inno_spectra_section_type_header)
            val widthHeader = getString(R.string.inno_spectra_section_width_header)
            val startHeader = getString(R.string.inno_spectra_spectral_start_header)
            val endHeader = getString(R.string.inno_spectra_spectral_end_header)
            val expHeader = getString(R.string.frag_new_section_exposure_header_text)
            val resHeader = getString(R.string.inno_spectra_digital_resolution_header)
            mBinding?.fragNewConfigRv?.adapter =
                ArrayAdapter(ctx, android.R.layout.simple_list_item_1,
                    mSections.mapIndexed { index, sect -> """
                        $index
                        $typeHeader: ${if (sect.type == 0) "Column" else "Hadamard"}
                        $widthHeader: ${sect.width}
                        $startHeader: ${sect.start} $endHeader: ${sect.end}
                        $expHeader: ${sect.exposure} $resHeader ${sect.resolution}
                    """.trimIndent() })
        }
    }

    override fun onResume() {
        super.onResume()

        updateSectionsList()
    }
}