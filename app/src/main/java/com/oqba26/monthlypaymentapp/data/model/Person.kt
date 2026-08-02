@file:OptIn(kotlinx.serialization.InternalSerializationApi::class)

package com.oqba26.monthlypaymentapp.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "persons")
data class Person(
    @PrimaryKey 
    @SerialName("id") val id: String = "",
    
    @SerialName("name") val name: String,
    
    @SerialName("isArchived") val isArchived: Boolean = false,
    
    @SerialName("displayOrder") val displayOrder: Long? = 0L,
    
    @SerialName("createdAt") val createdAt: Long? = 0L,

    @SerialName("phoneNumber") val phoneNumber: String? = "",

    @SerialName("category") val category: String = "salary",
    
    @SerialName("isAnonymous") val isAnonymous: Boolean = false,

    @SerialName("monthlyCommitment") val monthlyCommitment: Double = 0.0,

    @SerialName("startMonth") val startMonth: Int = 1,

    @SerialName("startYear") val startYear: Int = 1403,

    @kotlinx.serialization.Transient
    val needsSync: Boolean = false
)
