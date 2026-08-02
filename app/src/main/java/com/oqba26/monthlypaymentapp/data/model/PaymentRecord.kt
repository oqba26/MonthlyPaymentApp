@file:OptIn(kotlinx.serialization.InternalSerializationApi::class)

package com.oqba26.monthlypaymentapp.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
@Entity(tableName = "payments")
data class PaymentRecord(
    @PrimaryKey 
    @SerialName("id") val id: String = UUID.randomUUID().toString(),
    
    @SerialName("personId") val personId: String,
    
    @SerialName("amount") val amount: Double,
    
    @SerialName("shamsiYear") val shamsiYear: Int,
    
    @SerialName("shamsiMonth") val shamsiMonth: Int,
    
    @SerialName("timestamp") val timestamp: Long = System.currentTimeMillis(),
    
    @SerialName("description") val description: String? = "", // ⭐️ تغییر به String? برای پذیرش مقادیر null

    @SerialName("createdAt") val createdAt: Long? = null,

    @SerialName("category") val category: String = "salary",

    @kotlinx.serialization.Transient
    val needsSync: Boolean = false
)
