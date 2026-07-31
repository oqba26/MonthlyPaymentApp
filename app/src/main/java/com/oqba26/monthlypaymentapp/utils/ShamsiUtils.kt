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
