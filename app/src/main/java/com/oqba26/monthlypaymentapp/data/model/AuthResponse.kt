package com.oqba26.monthlypaymentapp.data.model

import kotlinx.serialization.Serializable

@Serializable
data class AuthResponse(
    val token: String,
    // ⭐️ سرور شما می‌تواند userId را برگرداند یا ندهد. آن را Nullable می‌کنیم.
    val userId: String? = null
)