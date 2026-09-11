@file:OptIn(InternalSerializationApi::class)

package com.oqba26.monthlypaymentapp.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
@Entity(tableName = "persons")
data class Person(
    @PrimaryKey 
    @SerialName("id") val id: String = "",
    
    @SerialName("name") val name: String,
    
    @SerialName("is_archived") val isArchived: Boolean = false,
    
    @SerialName("display_order") val displayOrder: Long? = 0L,
    
    @SerialName("created_at") val createdAt: Long? = 0L,

    @SerialName("phone_number") val phoneNumber: String? = "",

    @SerialName("category") val category: String = "salary",
    
    @SerialName("is_anonymous") val isAnonymous: Boolean = false,

    @SerialName("monthly_commitment") val monthlyCommitment: Double = 0.0,

    @SerialName("start_month") val startMonth: Int = 1,

    @SerialName("start_year") val startYear: Int = 1403,

    @Transient
    val needsSync: Boolean = false
)
