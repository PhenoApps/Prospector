package org.phenoapps.prospector.utils

import com.jjoe64.graphview.series.DataPoint
import kotlin.math.exp

//old function supplied by Stratio
//fun pixelToWavelength(index: Int, pixelValue: Double): DataPoint {
//
//    val a1 = 13.91
//    val b1 = 0.006626
//    val a2 = 1.972e-8
//    val b2 = 0.0422
//    val c = 400
//
//    val x = index+1
//    val y = (a1*exp(b1*x) + a2*exp(b2*x) + c)
//
//    return DataPoint(y, pixelValue)
//}

class LinkSquare {

    companion object {

        fun pixelToWavelength(index: Int, pixelValue: Double): DataPoint {

            val a1 = 80.64
            val b1 = 0.002842
            val a2 = 0.02079
            val b2 = 0.0178
            val c = 320

            val x = index+1
            val y = (a1*exp(b1*x) + a2*exp(b2*x) + c)

            return DataPoint(y, pixelValue)
        }
    }
}

class LinkSquareNIR {

    companion object {

        fun pixelToWavelength(index: Int, pixelValue: Double): DataPoint {

            val a1 = 76.27
            val b1 = 0.004256
            val a2 = -72.37
            val b2 = -0.002159
            val c = 700

            val x = index+1
            val y = (a1*exp(b1*x) + a2*exp(b2*x) + c)

            return DataPoint(y, pixelValue)
        }
    }
}