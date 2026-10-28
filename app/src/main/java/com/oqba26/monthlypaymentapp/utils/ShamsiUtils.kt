@file:Suppress("unused")

package com.oqba26.monthlypaymentapp.utils

import android.icu.text.SimpleDateFormat
import saman.zamani.persiandate.PersianDate
import java.util.Date
import java.util.Locale

fun getCurrentShamsiYear(): Int = PersianDate().shYear
fun getCurrentShamsiMonth(): Int = PersianDate().shMonth
fun getCurrentShamsiDay(): Int = PersianDate().shDay

fun formatTimestampToPersianDate(timestamp: Long): String {
    val persianDate = PersianDate(Date(timestamp))
    val year = persianDate.shYear.toString().toPersianDigits()
    val month = persianDate.shMonth.toString().padStart(2, '0').toPersianDigits()
    val day = persianDate.shDay.toString().padStart(2, '0').toPersianDigits()
    return "$year/$month/$day"
}

fun formatTimestampToPersianTime(timestamp: Long): String {
    val timeFormat = SimpleDateFormat("HH:mm", Locale.US)
    val timeString = timeFormat.format(Date(timestamp))
    return timeString.toPersianDigits()
}

fun formatTimestampToPersianDateTime(timestamp: Long): String {
    val date = formatTimestampToPersianDate(timestamp)
    val time = formatTimestampToPersianTime(timestamp)
    return "$date، ساعت $time"
}

fun isTimestampInCurrentCycle(timestamp: Long): Boolean {
    val now = PersianDate()
    val curYear = now.shYear
    val curMonth = now.shMonth
    val curDay = now.shDay

    val (startYear, startMonth) = if (curDay >= 20) {
        curYear to curMonth
    } else {
        if (curMonth == 1) (curYear - 1) to 12 else curYear to (curMonth - 1)
    }

    val (endYear, endMonth) = if (startMonth == 12) {
        (startYear + 1) to 1
    } else {
        startYear to (startMonth + 1)
    }

    val recordDate = PersianDate(Date(timestamp))
    val rYear = recordDate.shYear
    val rMonth = recordDate.shMonth
    val rDay = recordDate.shDay

    val isFirstHalf = (rYear == startYear && rMonth == startMonth && rDay >= 20)
    val isSecondHalf = (rYear == endYear && rMonth == endMonth && rDay < 20)

    return isFirstHalf || isSecondHalf
}
