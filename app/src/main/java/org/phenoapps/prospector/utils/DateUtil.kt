package org.phenoapps.prospector.utils

import android.os.Build
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

class DateUtil {

    //used for experiment dates
    fun getTime(): String = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        LocalDateTime.now().format(DateTimeFormatter.ofPattern(
            "yyyy-MM-dd-hh-mm-ss"))
    } else {
        Calendar.getInstance().time.toString()
    }

    fun displayTime(date: String): String = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

        try {

            val dbFormat = SimpleDateFormat("yyyy-MM-dd-hh-mm-ss", Locale.ENGLISH)
            val uiFormat = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)

            uiFormat.format(dbFormat.parse(date))

        } catch (e: Exception) {

            ""
        }

    } else {
        date
    }
}