package com.oqba26.monthlypaymentapp.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@kotlinx.serialization.Serializable
@Entity(tableName = "payments")
data class PaymentRecord(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val personId: String,
    val amount: Double,
    val shamsiYear: Int,
    val shamsiMonth: Int,
    val timestamp: Long = System.currentTimeMillis()
)