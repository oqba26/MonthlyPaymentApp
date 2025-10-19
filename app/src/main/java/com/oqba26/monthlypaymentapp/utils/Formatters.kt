package com.oqba26.monthlypaymentapp.utils

import java.text.NumberFormat
import java.util.Locale

private val persianLocale = Locale("fa", "IR")
private val numberFormat = NumberFormat.getNumberInstance(persianLocale)

/**
 * Formats a number into a Persian string with thousands separators.
 * Example: 200000.0 -> "۲۰۰٬۰۰۰"
 */
fun formatNumberAsPersian(number: Number): String {
    return numberFormat.format(number.toLong())
}
