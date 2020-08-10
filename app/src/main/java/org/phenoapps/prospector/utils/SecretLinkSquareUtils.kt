package org.phenoapps.prospector.utils

import com.jjoe64.graphview.series.DataPoint
import org.phenoapps.prospector.data.models.SpectralFrame
import kotlin.math.exp

fun List<SpectralFrame>.toWaveArray(): List<DataPoint> {

    val values = this.map { it.spectralValues }.flatMap { it.split(" ") }

    return (values.mapIndexed { index, value ->

        pixelToWavelength(index, if (value.isEmpty()) 0.0 else value.toDouble())

    })

}

//TODO discuss wavelengths output format
fun SpectralFrame.toWaveArray(): SpectralFrame {

    return this.also {

        it.spectralValues = it.spectralValues.mapIndexed { index, value ->

            pixelToWavelength(index, value.toDouble())

        }.map { it.y }.joinToString(" ")
    }
}

fun pixelToWavelength(index: Int, pixelValue: Double): DataPoint {

    val a1 = 13.91
    val b1 = 0.006626
    val a2 = 1.972e-8
    val b2 = 0.0422
    val c = 400

    val x = index+1
    val y = (a1* exp(b1*pixelValue) + a2* exp(b2*pixelValue) + c)

    return DataPoint(x.toDouble(), y)
}