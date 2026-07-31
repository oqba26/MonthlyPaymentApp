@file:OptIn(kotlinx.serialization.InternalSerializationApi::class)

package com.oqba26.monthlypaymentapp.data.model

import kotlinx.serialization.Serializable

@Serializable
data class AuthRequest(
    val email: String,
    val password: String
)
