package com.oqba26.monthlypaymentapp.utils

import saman.zamani.persiandate.PersianDate
import saman.zamani.persiandate.PersianDateFormat

fun Long.toPersianDate(pattern: String = "Y/M/d"): String {
    val pd = PersianDate(this) // this = timestamp (ms)
    return PersianDateFormat(pattern).format(pd)
}