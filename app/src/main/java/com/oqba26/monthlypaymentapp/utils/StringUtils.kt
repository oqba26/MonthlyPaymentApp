package com.oqba26.monthlypaymentapp.utils

// An extension function to convert any Int to a Persian-digit String.
fun Int.toPersianDigits(): String {
    return this.toString().toPersianDigits()
}

// An extension function to convert a String containing English digits to Persian digits.
fun String.toPersianDigits(): String {
    return this.map {
        when (it) {
            '0' -> '۰'
            '1' -> '۱'
            '2' -> '۲'
            '3' -> '۳'
            '4' -> '۴'
            '5' -> '۵'
            '6' -> '۶'
            '7' -> '۷'
            '8' -> '۸'
            '9' -> '۹'
            else -> it
        }
    }.joinToString("")
}
