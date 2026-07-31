package com.oqba26.monthlypaymentapp.utils

// An extension function to convert any Int to a Persian-digit String.
@Suppress("unused")
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

fun String.normalizePhoneNumber(): String {
    // فقط اعداد را نگه دار
    val digitsOnly = this.filter { it.isDigit() }
    
    // اگر با 98 شروع می‌شد، آن را به 0 تبدیل کن (فرمت رایج موبایل در ایران)
    return when {
        digitsOnly.startsWith("98") -> "0${digitsOnly.substring(2)}"
        !digitsOnly.startsWith("0") && digitsOnly.length == 10 -> "0$digitsOnly"
        else -> digitsOnly
    }
}

/**
 * الگوریتم لون‌اشتاین برای محاسبه شباهت دو رشته
 * مقدار برگشتی 0 یعنی کاملاً مشابه و هرچه بیشتر باشد شباهت کمتر است
 */
fun calculateLevenshteinDistance(s1: String, s2: String): Int {
    val dp = Array(s1.length + 1) { IntArray(s2.length + 1) }

    for (i in 0..s1.length) dp[i][0] = i
    for (j in 0..s2.length) dp[0][j] = j

    for (i in 1..s1.length) {
        for (j in 1..s2.length) {
            val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
            dp[i][j] = minOf(
                dp[i - 1][j] + 1,
                dp[i][j - 1] + 1,
                dp[i - 1][j - 1] + cost
            )
        }
    }
    return dp[s1.length][s2.length]
}

/**
 * بررسی شباهت دو نام به صورت هوشمند
 */
fun areNamesSimilar(name1: String, name2: String, threshold: Double = 0.4): Boolean {
    val n1 = name1.lowercase().trim()
    val n2 = name2.lowercase().trim()
    
    // اگر یکی شامل دیگری باشد
    if (n1.contains(n2) || n2.contains(n1)) return true
    
    val distance = calculateLevenshteinDistance(n1, n2)
    val maxLength = maxOf(n1.length, n2.length)
    if (maxLength == 0) return true
    
    val normalizedDistance = distance.toDouble() / maxLength
    return normalizedDistance <= threshold
}
