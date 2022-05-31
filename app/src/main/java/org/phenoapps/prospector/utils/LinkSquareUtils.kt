package org.phenoapps.prospector.utils

import android.content.Context
import android.graphics.Color
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.stratiotechnology.linksquareapi.LinkSquareAPI
import org.apache.commons.math.stat.StatUtils
import org.phenoapps.interfaces.iot.Device
import org.phenoapps.interfaces.spectrometers.Spectrometer.Companion.DEVICE_TYPE_LS1
import org.phenoapps.interfaces.spectrometers.Spectrometer.Companion.DEVICE_TYPE_NIR
import org.phenoapps.prospector.R
import org.phenoapps.prospector.data.models.DeviceTypeExport
import org.phenoapps.prospector.fragments.ScanListFragment

class LinkSquareLightSources {
    companion object {
        const val LED = 0
        const val BULB = 1
    }
}

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

class LinkSquareExportRange {
    companion object {
        const val min: Double = 399.0
        const val max: Double = 1001.0
    }
}

class LinkSquareNIRExportRange {
    companion object {
        const val min: Double = 699.0
        const val max: Double = 1051.0
    }
}

class InnoSpectraRange {
    companion object {
        const val min: Double = 900.0
        const val max: Double = 1701.0
    }
}

class InnoSpectraExportRange {
    companion object {
        const val min: Double = 899.0
        const val max: Double = 1701.0
    }
}

class IndigoRange {
    companion object {
        const val min: Double = 700.0
        const val max: Double = 1100.0
    }
}

class IndigoExportRange {
    companion object {
        const val min: Double = 700.0
        const val max: Double = 1100.0
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
fun List<ScanListFragment.ScanFrames>.toWaveArray(deviceType: String): List<Entry> {

    return when (deviceType) {
        DEVICE_TYPE_NIR, DEVICE_TYPE_LS1 -> {
            val values = this.map { it.spectralValues }.flatMap { it.split(" ") }
            values.mapIndexed { index, value -> LinkSquareNIR.pixelToWavelength(index, if (value.isEmpty()) 0.0 else value.toDouble()) }
        }

        else -> {
            val data = this.map { it.spectralValues }.joinToString(" ").split(" ")
            val wavelengths = this.map { it.wavelengths }.joinToString(" ").split( " ")
            data.mapIndexed { index, value ->
                Entry(wavelengths[index].toFloatOrNull() ?: 0f, value.toFloatOrNull() ?: 0f)
            }
        }
    }
}

fun List<Entry>.movingAverageSmooth(): List<Entry> {

    //moving average smoothing
    val data = this.map { it.y.toDouble() }.toDoubleArray()

    val smooth = arrayListOf<Entry>()

    val windowSize = 32

    val frameSize = this.size-1

    for (i in indices) {

        smooth.add(Entry(this[i].x, when {
            i > frameSize - windowSize -> StatUtils.mean(data, i, frameSize - i + 1)
            else -> StatUtils.mean(data, i, windowSize)
        }.toFloat()))

    }

    return smooth
}

/**
 * Converts a string of space delimited values to wavelength data points.
 * Mainly used to translate pixel strings to a list of the converted values.
 * Similar to List<SpectralFrame>.toWaveArray but doesn't use GraphView data points.
 */
fun DeviceTypeExport.toWaveArray(deviceType: String): ArrayList<Pair<Float, Float>> {

    val result = ArrayList<Pair<Float, Float>>()

    when (deviceType) {
        DEVICE_TYPE_NIR, DEVICE_TYPE_LS1 -> {

            val tokens = this.spectralData.split(" ")

            tokens.forEachIndexed { index, value ->

                val wave = when (deviceType) {
                    DEVICE_TYPE_NIR -> LinkSquareNIR.pixelToWavelength(index, value.toDouble())
                    DEVICE_TYPE_LS1 -> LinkSquare.pixelToWavelength(index, value.toDouble())
                    else -> Entry(900 + index.toFloat(), value.toFloat())
                }

                result.add(result.size, wave.x to wave.y)

            }
        }

        else -> {

            val values = this.spectralData.split(" ")
            val wavelengths = this.wavelengths?.split(" ")

            if (values.size == wavelengths?.size ?: 0) {

                wavelengths?.forEachIndexed { index, s ->

                    val wave = Entry(s.toFloat(), values[index].toFloat())

                    result.add(result.size, wave.x to wave.y)

                }
            }

        }
    }

    return result
}

/**
 * Similar to toWaveArray but doesn't convert the pixel values.
 */
fun List<ScanListFragment.ScanFrames>.toPixelArray(): List<Entry> {

    val values = this.map { it.spectralValues }.flatMap { it.split(" ") }

    return (values.mapIndexed { index, value ->

        Entry(index.toFloat(), value.toFloat())

    })

}

/**
 * Centers the graph viewport whenever new data is loaded.
 * Fits the bounds of the viewport to the min/max values of the data.
 *
 * Defaults to a range between [440, 1040]
 */
//fun centerViewport(graph: GraphView, data: List<Entry>, converted: Boolean, deviceType: String) = with(graph) {
//
//    val minX = data.minByOrNull { it.x }?.x ?: 440.0
//
//    val maxX = data.maxByOrNull { it.x }?.x ?: minX+600.0
//
//    val maxY = data.map { it.y }.maxOrNull() ?: 400.0
//
//    val minY = data.map { it.y }.minOrNull() ?: 0.0
//
//    viewport.isXAxisBoundsManual = true
//    viewport.setMinX(minX)
//
//    if (converted) {
//        //cull the graph based on the device specifications
//        viewport.setMaxX(when(deviceType) {
//            DEVICE_TYPE_NIR -> LinkSquareNIRRange.max
//            else -> LinkSquareRange.max
//        })
//        viewport.setMinX(when(deviceType) {
//            DEVICE_TYPE_NIR -> LinkSquareNIRRange.min
//            else -> LinkSquareRange.min
//        })
//    } else { //otherwise use all the pixel data
//        viewport.setMaxX(maxX)
//        viewport.setMinX(minX)
//    }
//
//    viewport.isYAxisBoundsManual = true
//    viewport.setMinY(minY)
//    viewport.setMaxY(maxY)
//
//}

///**
// * Sets the grid parameters and labels for the graph.
// */
//fun setViewportGrid(graph: GraphView, convert: Boolean) = with(graph){
//
//    gridLabelRenderer.gridStyle = GridLabelRenderer.GridStyle.BOTH
//    gridLabelRenderer.isHorizontalLabelsVisible=true
//    gridLabelRenderer.isVerticalLabelsVisible=true
//    gridLabelRenderer.labelsSpace = 1
//    //gridLabelRenderer.padding = 1
//    gridLabelRenderer.verticalAxisTitle = "Intensity (a.u)"
//    gridLabelRenderer.horizontalAxisTitle = if (convert) "Wavelength (nm)" else "Pixel (px)"
//    gridLabelRenderer.numHorizontalLabels = 5
//    gridLabelRenderer.numVerticalLabels = 5
//
//}

///**
// * Enables graph scrolling/scaling
// */
//fun setViewportScalable(graph: GraphView) = with(graph){
//
//    viewport.isXAxisBoundsManual = false
//    viewport.isYAxisBoundsManual = false
//
//    // Enable scaling
//    //viewport.isScalable = true
//    viewport.isScrollable = true
//    //viewport.setScalableY(true)
//    viewport.setScrollableY(true)
//
//}

//class to represent coloring datasets
data class FrameEntry(val data: List<Entry>, val color: Int = Color.BLACK)

/**
 * Color the graph if it is chosen and plot the series into the graph view.
 */
fun renderNormal(graph: LineChart, entries: ArrayList<FrameEntry>) = with(graph) {

//    //link square devices always have 600 data points per scan
//    val plot = LineGraphSeries(data.toTypedArray())
//
//    plot.thickness = 2
//
//    plot.color = Color.parseColor(color)
////    plot.color = if (color != null) try {
////        Color.parseColor(color)
////    } catch (e: Exception) {
////        Color.rgb(Random.nextInt(255),Random.nextInt(255),Random.nextInt(255))
////    }
////    else Color.rgb(Random.nextInt(255),Random.nextInt(255),Random.nextInt(255))
//
//    graph.addSeries(plot)

    graph.clear()

    graph.data = LineData(*entries
        .mapIndexed { index, data -> LineDataSet(data.data, index.toString()).apply {
            lineWidth = 0.5f
            color = data.color
            mode = LineDataSet.Mode.LINEAR
            setDrawValues(false)
            setDrawCircles(false)
            setDrawFilled(false)
        } }
        .toTypedArray())

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
fun resolveDeviceType(context: Context, data: Device.DeviceInfo): String =

    if (data.deviceId.startsWith("NIR")) {

        context.getString(R.string.linksquare_nir)

    } else context.getString(R.string.linksquare)
