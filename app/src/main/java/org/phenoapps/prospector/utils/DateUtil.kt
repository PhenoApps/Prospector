package org.phenoapps.prospector.utils

import android.os.Build
import java.text.SimpleDateFormat
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.*

class DateUtil {

    //used for experiment dates
    fun getTime(): String = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        ZonedDateTime.now().format(DateTimeFormatter.ofPattern(
            "yyyy-MM-dd HH:mm:ss.SSSZZZZZ"))
    } else {
        //calendar instance was showing weird times on some devices
        //this will explicitly set the format
        with (Calendar.getInstance()) {
            val month = "${get(Calendar.MONTH) + 1}".padStart(2, '0')
            //instances shows single digit seconds, so we have to pad to two digits
            val seconds = get(Calendar.SECOND).toString()
            val ms = get(Calendar.MILLISECOND).toString()
            //also months start from 0 so add 1
            "${get(Calendar.YEAR)}" +
                    "-$month-${get(Calendar.DAY_OF_MONTH)}" +
                    " ${get(Calendar.HOUR_OF_DAY)}:${get(Calendar.MINUTE)}:${seconds.padStart(2, '0')}.${ms.padStart(3, '0')}+00:00"
        }
    }

    fun displayScanTime(date: String): String = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

        try {

            val dbFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSZZZZZ", Locale.ENGLISH)
            val uiFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH)

            dbFormat.parse(date)?.let { parsed ->

                return uiFormat.format(parsed)

            }

            date

        } catch (e: Exception) {

            date
        }

    } else {
        date
    }

    fun displayTime(date: String): String = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

        try {

            val dbFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSZZZZZ", Locale.ENGLISH)
            val uiFormat = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)

            dbFormat.parse(date)?.let { parsed ->

                return uiFormat.format(parsed)

            }

            date

        } catch (e: Exception) {

            date
        }

    } else {
        date
    }
}