package org.phenoapps.prospector.fragments.nano_configuration_creator

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import dagger.hilt.android.AndroidEntryPoint
import org.phenoapps.prospector.R
import org.phenoapps.prospector.activities.MainActivity
import org.phenoapps.prospector.data.viewmodels.devices.InnoSpectraViewModel

@AndroidEntryPoint
class SummaryFragment: Fragment(R.layout.fragment_config_creator_summary) {

    val args: SummaryFragmentArgs by navArgs()

    private var uiSubtitleText: TextView? = null
    private var uiGraph: LineChart? = null
    private var uiBackButton: Button? = null
    private var uiNextButton: Button? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        uiSubtitleText = view.findViewById(R.id.frag_cc_subtitle_tv)
        uiGraph = view.findViewById(R.id.frag_cc_chart)
        uiBackButton = view.findViewById(R.id.frag_cc_back_btn)
        uiNextButton = view.findViewById(R.id.frag_cc_next_btn)

        uiGraph?.let { graph ->

            val data = getData()

            setupChart(graph, data)
        }

        view.findViewById<Toolbar>(R.id.toolbar)?.title = ""

        val size = args.config.sections?.size ?: 0
        uiSubtitleText?.text = getString(R.string.frag_cc_summary_subtitle,
            args.config.name,
            size.toString(),
            "${args.config.sections?.sumOf { it.resolution } ?: 0.0}",
            "${args.config.sections?.minOf { it.start } ?: 900}",
            "${args.config.sections?.maxOf { it.end } ?: 1700}")

        setupButtonListeners()
        setupToolbar()
    }

    private fun setupToolbar() {
        val toolbar = view?.findViewById<Toolbar>(R.id.toolbar)

        toolbar?.setNavigationOnClickListener {
            findNavController().popBackStack()
        }
    }

    private val colors = intArrayOf(
        Color.rgb(137, 230, 81),
        Color.rgb(240, 240, 30),
        Color.rgb(89, 199, 250),
        Color.rgb(250, 104, 104),
        Color.rgb(250, 100, 250)
    )

    private fun setupChart(chart: LineChart, data: LineData) {

        // no description text
        chart.description.isEnabled = false

        // enable / disable grid background
        chart.setDrawGridBackground(false)

        // enable touch gestures
        chart.setTouchEnabled(false)

        // enable scaling and dragging
        chart.isDragEnabled = false
        chart.setScaleEnabled(false)

        // if disabled, scaling can be done on x- and y-axis separately
        chart.setPinchZoom(false)
        chart.setBackgroundColor(Color.TRANSPARENT)

        // set custom chart offsets (automatic offset calculation is hereby disabled)
//        chart.setViewPortOffsets(10f, 10f, 10f, 10f)

        // add data
        chart.data = data

        chart.fitScreen()

        // get the legend (only possible after setting data)
        val l = chart.legend
        l.isEnabled = false
        chart.axisLeft.isEnabled = true
        chart.axisLeft.isGranularityEnabled = true
        chart.axisLeft.granularity = 5f
        chart.axisLeft.labelCount = 2
        chart.axisLeft.spaceTop = 40f
        chart.axisLeft.setDrawLabels(true)
        chart.axisLeft.spaceBottom = 40f
        chart.axisRight.isEnabled = false
        chart.xAxis.setDrawLabels(true)
        chart.xAxis.isEnabled = true
        chart.xAxis.axisMinimum = 900f
        chart.xAxis.axisMaximum = 1700f
        chart.axisLeft.axisMinimum = 0f
        chart.axisLeft.axisMaximum = 624f
        chart.axisRight.axisMinimum = 0f
        chart.axisRight.axisMaximum = 624f

        // animate calls invalidate()...
        //chart.animateX(2500)
    }

    private fun getData(): LineData {

        val data = arrayListOf<LineDataSet>()

        for (i in 0..args.config.currentIndex) {
            val values: ArrayList<Entry> = arrayListOf()

            values.add(Entry(args.config.sections?.get(i)?.start ?: 0f,
                args.config.sections?.get(i)?.resolution?.toFloat() ?: 0f))
            values.add(Entry(args.config.sections?.get(i)?.end ?: 0f,
                args.config.sections?.get(i)?.resolution?.toFloat() ?: 0f))

            // create a dataset and give it a type
            val set = LineDataSet(values, "S$i")
            set.lineWidth = 5f
            set.circleRadius = 3f
            set.circleHoleRadius = 1.5f
            set.color = colors[i]
            set.setCircleColor(Color.GREEN)
            set.highLightColor = Color.MAGENTA
            set.setDrawValues(true)

            data.add(set)
        }

        // create a data object with the data sets
        return LineData(*data.toTypedArray())
    }

    private fun setupButtonListeners() {

        uiBackButton?.setOnClickListener {
            findNavController().popBackStack()
        }

        uiNextButton?.setOnClickListener {

            saveConfig()
        }
    }


    private fun saveConfig() {

        val deviceViewModel = (activity as MainActivity).sDeviceViewModel as InnoSpectraViewModel

        deviceViewModel.addConfig(args.config)

        findNavController().navigate(SummaryFragmentDirections
            .actionSummaryFragmentToInnoSpectraSettingsFragment())

    }
}