package com.oqba26.monthlypaymentapp.data.model

@kotlinx.serialization.Serializable
data class BackupData(
    val persons: List<Person>,
    val payments: List<PaymentRecord>
)