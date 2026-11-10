package com.oqba26.monthlypaymentapp.utils

import android.annotation.SuppressLint
import android.content.Context
import android.provider.Settings

@SuppressLint("HardwareIds")
fun getDeviceHardwareId(context: Context): String {
    return try {
        Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "UNKNOWN"
    } catch (_: Exception) {
        "UNKNOWN"
    }
}

fun getDeviceModelName(): String {
    val manufacturer = android.os.Build.MANUFACTURER.replaceFirstChar { it.uppercase() }
    val model = android.os.Build.MODEL
    return if (model.startsWith(manufacturer, ignoreCase = true)) {
        model
    } else {
        "$manufacturer $model"
    }
}
