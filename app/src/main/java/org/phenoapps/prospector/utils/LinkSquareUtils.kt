package org.phenoapps.prospector.utils

import android.content.Context
import android.graphics.Color
import com.jjoe64.graphview.GraphView
import com.jjoe64.graphview.GridLabelRenderer
import com.jjoe64.graphview.series.DataPoint
import com.jjoe64.graphview.series.LineGraphSeries
import com.stratiotechnology.linksquareapi.LinkSquareAPI
import org.phenoapps.prospector.R
import org.phenoapps.prospector.data.models.SpectralFrame
import kotlin.random.Random

val plotColor = Color.BLACK

fun renderPotato(graph: GraphView, data: List<DataPoint>) {

    graph.removeAllSeries()

    graph.gridLabelRenderer.isVerticalLabelsVisible = false
    graph.gridLabelRenderer.isHorizontalLabelsVisible = false

    var prev = 0

    var frameSize = 600

    var numFrames = data.size / frameSize

    for (i in frameSize..data.size step frameSize) {

        val plot = LineGraphSeries((data.subList(prev, i))
                .mapIndexed { index, dataPoint ->
                    if (index % 10 == 0) {
                        DataPoint((index.toDouble() % 600)+440, dataPoint.y)
                    } else null
                }.mapNotNull { it }.toTypedArray())

        prev = i

        // give line a unique color
        plot.color = plotColor

        graph.addSeries(plot)

    }
}

fun List<SpectralFrame>.toPixelArray(): List<DataPoint> {

    val values = this.map { it.spectralValues }.flatMap { it.split(" ") }

    return (values.mapIndexed { index, value ->

        DataPoint(index.toDouble(), value.toDouble())

    })

}

fun centerViewport(graph: GraphView, data: List<DataPoint>) = with(graph) {

    //600 is the fixed size frame length for each LSFrame
    val minX = 440.0

    val maxX = 440.0+600.0

    val maxY = data.map { it.y }.max() ?: 400.0

    val minY = data.map { it.y }.min() ?: 0.0

    val marginX = maxX*.05

    val marginY = maxY*.05

    viewport.isXAxisBoundsManual = true
    viewport.setMinX(minX)
    viewport.setMaxX(maxX)

    viewport.isYAxisBoundsManual = true
    viewport.setMinY(minY)
    viewport.setMaxY(maxY)

}

fun setViewportGrid(graph: GraphView) = with(graph){

//    this.title = title
    gridLabelRenderer.gridStyle = GridLabelRenderer.GridStyle.BOTH
    gridLabelRenderer.isHorizontalLabelsVisible=false
    gridLabelRenderer.isVerticalLabelsVisible=false
//    gridLabelRenderer.horizontalAxisTitle = xAxis
//    gridLabelRenderer.verticalAxisTitle = yAxis
    gridLabelRenderer.numHorizontalLabels = 10
    gridLabelRenderer.numVerticalLabels = 10

}

fun setViewportScalable(graph: GraphView) = with(graph){

    viewport.isXAxisBoundsManual = false
    viewport.isYAxisBoundsManual = false

    // Enable scaling
    viewport.isScalable = true
    viewport.isScrollable = true
    viewport.setScalableY(true)
    viewport.setScrollableY(true)

}

fun renderNormal(graph: GraphView, data: List<DataPoint>) = with(graph) {

    graph.removeAllSeries()

    var prev = 0

    var frameSize = 600

    var numFrames = data.size / frameSize

    var frame = 0

    for (i in frameSize..data.size step frameSize) {

        val plot = LineGraphSeries((data.subList(prev, i))
                .mapIndexed { index, dataPoint ->
                    DataPoint((index.toDouble() % frameSize)+440, dataPoint.y)
                }.toTypedArray())

        prev = i

        frame += 1

        plot.color = Color.rgb(Random.nextInt(255),Random.nextInt(255),Random.nextInt(255))

        graph.addSeries(plot)

    }

}

fun buildLinkSquareDeviceInfo(context: Context, data: LinkSquareAPI.LSDeviceInfo?): String {

    data?.let { info ->

        val aliasHeader = context.getString(R.string.alias_header)
        val deviceIdHeader = context.getString(R.string.device_id_header)
        val deviceTypeHeader = context.getString(R.string.device_type_header)
        val hwVersion = context.getString(R.string.hw_version_header)
        val opMode = context.getString(R.string.op_mode_header)
        val swVersion = context.getString(R.string.sw_version_header)

        return arrayOf("${aliasHeader}: ${info.Alias}",
            "${deviceIdHeader}: ${info.DeviceID}",
            "${deviceTypeHeader}: ${info.DeviceType}",
            "${hwVersion}: ${info.HWVersion}",
            "${opMode}: ${info.OPMode}",
            "${swVersion}: ${info.SWVersion}").joinToString(System.lineSeparator())


    }

    return "None"
}