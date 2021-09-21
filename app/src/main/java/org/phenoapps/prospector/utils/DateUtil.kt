package org.phenoapps.prospector.utils

import android.os.Build
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
}