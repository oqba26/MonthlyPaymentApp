@file:OptIn(kotlinx.serialization.InternalSerializationApi::class)

package com.oqba26.monthlypaymentapp.viewmodel

import kotlinx.serialization.Serializable

@Serializable
@Suppress("unused")
data class UpdateArchivedStatusRequest(
    val isArchived: Boolean
)

@Serializable
@Suppress("unused")
data class UpdateDisplayOrderRequest(
    val displayOrder: Long // FIX: Changed from Int to Long
)