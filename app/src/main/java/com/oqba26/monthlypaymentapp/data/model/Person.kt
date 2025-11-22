package com.oqba26.monthlypaymentapp.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.Serializable
import kotlin.OptIn // CORRECTED IMPORT

@OptIn(InternalSerializationApi::class)
@Serializable
@Entity(tableName = "persons")
data class Person(
    @PrimaryKey val id: String = "",
    val name: String,
    val isArchived: Boolean = false,
    val displayOrder: Long = 0L, 
    val createdAt: Long = 0L
)
