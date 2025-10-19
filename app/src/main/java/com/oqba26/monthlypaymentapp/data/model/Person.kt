package com.oqba26.monthlypaymentapp.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
@Entity(tableName = "persons")
data class Person(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val userId: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)