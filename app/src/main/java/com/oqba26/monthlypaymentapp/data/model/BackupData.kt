package com.oqba26.monthlypaymentapp.data.model

import android.annotation.SuppressLint

@SuppressLint("UnsafeOptInUsageError")
@kotlinx.serialization.Serializable
data class BackupData(
    val persons: List<Person>,
    val payments: List<PaymentRecord>
)