package com.oqba26.monthlypaymentapp.data.model

import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.Serializable

@OptIn(InternalSerializationApi::class) // <<< CRITICAL FIX: Added the required Opt-In annotation
@Serializable
data class AuthRequest(
    val username: String,
    val password: String
)
