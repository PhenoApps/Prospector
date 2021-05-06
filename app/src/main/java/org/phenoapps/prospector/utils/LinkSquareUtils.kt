package org.phenoapps.prospector.utils

import DEVICE_TYPE_NIR
import android.content.Context
import android.graphics.Color
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import com.jjoe64.graphview.GraphView
import com.jjoe64.graphview.GridLabelRenderer
import com.jjoe64.graphview.series.DataPoint
import com.jjoe64.graphview.series.LineGraphSeries
import com.stratiotechnology.linksquareapi.LinkSquareAPI
import org.apache.commons.math.stat.StatUtils
import org.phenoapps.prospector.R
import org.phenoapps.prospector.data.models.SpectralFrame

/**
 * Specified wavelength ranges of the LinkSquare spectrometers
 */
class LinkSquareRange {
    companion object {
        const val min: Double = 400.0
        const val max: Double = 1000.0
    }
}

class LinkSquareNIRRange {
    companion object {
        const val min: Double = 700.0
        const val max: Double = 1050.0
    }
}

/**
 * Public helper functions for LinkSquare related processing.
 *
 */
fun <T> LiveData<T>.observeOnce(lifecycleOwner: LifecycleOwner, observer: Observer<T>) {
    observe(lifecycleOwner, object : Observer<T> {
        override fun onChanged(t: T?) {
            observer.onChanged(t)
            removeObserver(this)
        }
    })
}

/**
 * Flat maps spectral frames to a single wave list.
 * Some devices have a proprietary conversion between pixels to wavelengths
 */
fun List<SpectralFrame>.toWaveArray(deviceType: String): List<DataPoint> {

    val values = this.map { it.spectralValues }.flatMap { it.split(" ") }

    return (values.mapIndexed { index, value ->

        when (deviceType) {
            DEVICE_TYPE_NIR -> LinkSquareNIR.pixelToWavelength(index, if (value.isEmpty()) 0.0 else value.toDouble())
            else -> LinkSquare.pixelToWavelength(index, if (value.isEmpty()) 0.0 else value.toDouble())
        }

    })

}

fun List<DataPoint>.movingAverageSmooth(): List<DataPoint> {

    //moving average smoothing
    val data = this.map { it.y }.toDoubleArray()

    val smooth = arrayListOf<DataPoint>()

    val windowSize = 32

    val frameSize = this.size-1

    for (i in indices) {

        smooth.add(DataPoint(this[i].x, when {
            i > frameSize - windowSize -> StatUtils.mean(data, i, frameSize - i + 1)
            else -> StatUtils.mean(data, i, windowSize)
        }))

    }

    return smooth
}

/**
 * Converts a string of space delimited values to wavelength data points.
 * Mainly used to translate pixel strings to a list of the converted values.
 * Similar to List<SpectralFrame>.toWaveArray but doesn't use GraphView data points.
 */
fun String.toWaveArray(deviceType: String): ArrayList<Pair<Double, Double>> {

    val result = ArrayList<Pair<Double, Double>>()

    val tokens = this.split(" ")

    tokens.forEachIndexed { index, value ->

        val wave = when (deviceType) {
            DEVICE_TYPE_NIR -> LinkSquareNIR.pixelToWavelength(index, value.toDouble())
            else -> LinkSquare.pixelToWavelength(index, value.toDouble())
        }

        result.add(result.size, wave.x to wave.y)

    }

    return result
}

/**
 * Similar to toWaveArray but doesn't convert the pixel values.
 */
fun List<SpectralFrame>.toPixelArray(): List<DataPoint> {

    val values = this.map { it.spectralValues }.flatMap { it.split(" ") }

    return (values.mapIndexed { index, value ->

        DataPoint(index.toDouble(), value.toDouble())

    })

}

/**
 * Centers the graph viewport whenever new data is loaded.
 * Fits the bounds of the viewport to the min/max values of the data.
 *
 * Defaults to a range between [440, 1040]
 */
fun centerViewport(graph: GraphView, data: List<DataPoint>, converted: Boolean, deviceType: String) = with(graph) {

    val minX = data.minByOrNull { it.x }?.x ?: 440.0

    val maxX = data.maxByOrNull { it.x }?.x ?: minX+600.0

    val maxY = data.map { it.y }.maxOrNull() ?: 400.0

    val minY = data.map { it.y }.minOrNull() ?: 0.0

    viewport.isXAxisBoundsManual = true
    viewport.setMinX(minX)

    if (converted) {
        //cull the graph based on the device specifications
        viewport.setMaxX(when(deviceType) {
            DEVICE_TYPE_NIR -> LinkSquareNIRRange.max
            else -> LinkSquareRange.max
        })
        viewport.setMinX(when(deviceType) {
            DEVICE_TYPE_NIR -> LinkSquareNIRRange.min
            else -> LinkSquareRange.min
        })
    } else { //otherwise use all the pixel data
        viewport.setMaxX(maxX)
        viewport.setMinX(minX)
    }

    viewport.isYAxisBoundsManual = true
    viewport.setMinY(minY)
    viewport.setMaxY(maxY)

}

/**
 * Sets the grid parameters and labels for the graph.
 */
fun setViewportGrid(graph: GraphView, convert: Boolean) = with(graph){

    gridLabelRenderer.gridStyle = GridLabelRenderer.GridStyle.BOTH
    gridLabelRenderer.isHorizontalLabelsVisible=true
    gridLabelRenderer.isVerticalLabelsVisible=true
    gridLabelRenderer.labelsSpace = 1
    //gridLabelRenderer.padding = 1
    gridLabelRenderer.verticalAxisTitle = "Intensity (a.u)"
    gridLabelRenderer.horizontalAxisTitle = if (convert) "Wavelength (nm)" else "Pixel (px)"
    gridLabelRenderer.numHorizontalLabels = 5
    gridLabelRenderer.numVerticalLabels = 5

}

/**
 * Enables graph scrolling/scaling
 */
fun setViewportScalable(graph: GraphView) = with(graph){

    viewport.isXAxisBoundsManual = false
    viewport.isYAxisBoundsManual = false

    // Enable scaling
    //viewport.isScalable = true
    viewport.isScrollable = true
    //viewport.setScalableY(true)
    viewport.setScrollableY(true)

}

/**
 * Color the graph if it is chosen and plot the series into the graph view.
 */
fun renderNormal(graph: GraphView, data: List<DataPoint>, color: String?) = with(graph) {

    //link square devices always have 600 data points per scan
    val plot = LineGraphSeries(data.toTypedArray())

    plot.thickness = 2

    plot.color = Color.parseColor(color)
//    plot.color = if (color != null) try {
//        Color.parseColor(color)
//    } catch (e: Exception) {
//        Color.rgb(Random.nextInt(255),Random.nextInt(255),Random.nextInt(255))
//    }
//    else Color.rgb(Random.nextInt(255),Random.nextInt(255),Random.nextInt(255))

    graph.addSeries(plot)

}

/**
 * Returns a string that composes all data from the LinkSquare API.
 */
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

/**
 * Used to translate the device id to a device type.
 * TODO: with the new LinkSquare 1.15 api, there is a DeviceType, must confirm that NIR=1 and LS=0
 */
fun resolveDeviceType(context: Context, data: LinkSquareAPI.LSDeviceInfo): String =

    if (data.DeviceID.startsWith("NIR")) {

        context.getString(R.string.linksquare_nir)

    } else context.getString(R.string.linksquare)
