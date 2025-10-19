package com.oqba26.monthlypaymentapp.data.model

import kotlinx.serialization.Serializable

// مدل برای ارسال به سرور هنگام ورود/ثبت نام
@Serializable
data class LoginRequest(
    val username: String, // یا email
    val password: String
)

// مدلی که سرور پس از ورود موفق برمی‌گرداند
@Serializable
data class LoginResponse(
    val token: String, // توکن دسترسی (JWT)
    val userId: String, // شناسه کاربر برای فیلتر کردن داده‌ها
    val username: String
)