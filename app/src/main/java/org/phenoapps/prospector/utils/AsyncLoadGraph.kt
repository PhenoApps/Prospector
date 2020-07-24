package org.phenoapps.prospector.utils

import android.content.Context
import android.graphics.Color
import android.os.AsyncTask
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.constraintlayout.widget.ConstraintLayout
import com.jjoe64.graphview.GraphView
import com.jjoe64.graphview.GridLabelRenderer
import com.jjoe64.graphview.series.DataPoint
import com.jjoe64.graphview.series.LineGraphSeries
import org.phenoapps.prospector.data.models.SpectralFrame
import kotlin.random.Random

class AsyncLoadGraph(private val context: Context,
                     private var graph: GraphView,
                     private val title: String,
                     private val horizontalAxisTitle: String,
                     private val verticalAxisTitle: String,
                     private val data: List<SpectralFrame>,
                     private val potatoRender: Boolean = false,
                     private val autoSize: Boolean = false) : AsyncTask<Void?, List<DataPoint>, List<DataPoint>>() {

    override fun doInBackground(vararg void: Void?): List<DataPoint> {

        //todo: show error message if spectral values are incorrect data types

        val values = data.map { it.spectralValues }.flatMap { it.split(" ") }

        return values.mapIndexed { index, value ->

            DataPoint(index.toDouble(), if (value.isEmpty()) 0.0 else value.toDouble())

        }

    }

    override fun onPostExecute(data: List<DataPoint>) {

        if (potatoRender) {

            renderPotato(data)

        } else renderNormal(data)

    }

    private fun setAutoViewport(data: List<DataPoint>) {

        //600 is the fixed size frame length for each LSFrame
        val maxX = 600 //data.size.toDouble()

        val maxY = data.map { it.y }.max() ?: 50.0

        val marginX = maxX*.05

        val marginY = maxY*.05

        // Set manual X bounds on the graph
        graph.viewport.isXAxisBoundsManual = true
        graph.viewport.setMinX(0.0-marginX)
        graph.viewport.setMaxX(maxX+marginX)

        // Set manual Y bounds on the graph
        graph.viewport.isYAxisBoundsManual = true
        graph.viewport.setMinY(0.0-marginY)
        graph.viewport.setMaxY(maxY)

    }

    private fun renderPotato(data: List<DataPoint>) {

        graph.removeAllSeries()

        setAutoViewport(data)

        graph.title = title

        graph.gridLabelRenderer.gridStyle = GridLabelRenderer.GridStyle.BOTH
        graph.gridLabelRenderer.horizontalAxisTitle = horizontalAxisTitle
        graph.gridLabelRenderer.verticalAxisTitle = verticalAxisTitle

        graph.gridLabelRenderer.numHorizontalLabels = 3
        graph.gridLabelRenderer.numVerticalLabels = 3

//        graph.gridLabelRenderer.labelFormatter = object : LabelFormatter {
//
//            override fun formatLabel(value: Double, isValueX: Boolean): String {
//
//                return if (isValueX) {
//
//                    when (value) {
//
//
//
//                    }
//
//                } else value.toString()
//
//            }
//
//            override fun setViewport(viewport: Viewport?) {}
//
//        }

        var prev = 0

        for (i in 600..data.size step 600) {

            val plot = LineGraphSeries(data.subList(prev, i).mapIndexed { index, dataPoint ->  if (index % 32 == 0) DataPoint(index.toDouble(), dataPoint.y) else null }.mapNotNull { it }.toTypedArray())

            prev = i

            // give line a unique color
            plot.color = Color.rgb(Random.nextInt(255), Random.nextInt(255), Random.nextInt(255))

            graph.addSeries(plot)

        }
    }

    private fun renderNormal(data: List<DataPoint>) {

        graph.removeAllSeries()

        if (autoSize) {

            setAutoViewport(data)

        } else {

            graph.viewport.isXAxisBoundsManual = false
            graph.viewport.isYAxisBoundsManual = false

            // Enable scaling
            graph.viewport.isScalable = true
            graph.viewport.isScrollable = true
            graph.viewport.setScalableY(true)
            graph.viewport.setScrollableY(true)

        }

        graph.title = title

        graph.gridLabelRenderer.gridStyle = GridLabelRenderer.GridStyle.BOTH
        graph.gridLabelRenderer.horizontalAxisTitle = horizontalAxisTitle
        graph.gridLabelRenderer.verticalAxisTitle = verticalAxisTitle

        graph.gridLabelRenderer.numHorizontalLabels = 10
        graph.gridLabelRenderer.numVerticalLabels = 10

//        graph.gridLabelRenderer.labelFormatter = object : LabelFormatter {
//
//            override fun formatLabel(value: Double, isValueX: Boolean): String {
//
//                return if (isValueX) {
//
//                    when (value) {
//
//
//
//                    }
//
//                } else value.toString()
//
//            }
//
//            override fun setViewport(viewport: Viewport?) {}
//
//        }

        var prev = 0

        for (i in 600..data.size step 600) {

            val plot = LineGraphSeries(data.subList(prev, i).mapIndexed { index, dataPoint ->  DataPoint(index.toDouble(), dataPoint.y) }.toTypedArray())

            prev = i

            // give line a unique color
            plot.color = Color.rgb(Random.nextInt(255), Random.nextInt(255), Random.nextInt(255))

            graph.addSeries(plot)

        }
    }
}