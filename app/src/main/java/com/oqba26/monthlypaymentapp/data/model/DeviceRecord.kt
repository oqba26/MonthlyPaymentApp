package com.oqba26.monthlypaymentapp.data.model

import android.annotation.SuppressLint
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class DeviceRecord(
    @SerialName("id") val id: String = "",
    @SerialName("deviceId") val deviceId: String,
    @SerialName("deviceName") val deviceName: String = "",
    @SerialName("isAdmin") val isAdmin: Boolean = false,
    @SerialName("allowBackup") val allowBackup: Boolean = false,
    @SerialName("allowLogout") val allowLogout: Boolean = false,
    @SerialName("lastActive") val lastActive: Long = 0L,
)
