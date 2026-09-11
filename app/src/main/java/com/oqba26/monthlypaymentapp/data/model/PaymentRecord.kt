@file:OptIn(InternalSerializationApi::class)

package com.oqba26.monthlypaymentapp.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import java.util.UUID

@Serializable
@Entity(tableName = "payments")
data class PaymentRecord(
    @PrimaryKey 
    @SerialName("id") val id: String = UUID.randomUUID().toString(),
    
    @SerialName("person_id") val personId: String,
    
    @SerialName("amount") val amount: Double,
    
    @SerialName("shamsi_year") val shamsiYear: Int,
    
    @SerialName("shamsi_month") val shamsiMonth: Int,
    
    @SerialName("timestamp") val timestamp: Long = System.currentTimeMillis(),
    
    @SerialName("description") val description: String? = "",

    @SerialName("created_at") val createdAt: Long? = null,

    @SerialName("category") val category: String = "salary",

    @Transient
    val needsSync: Boolean = false
)
