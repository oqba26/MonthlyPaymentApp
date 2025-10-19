package com.oqba26.monthlypaymentapp.data.remote

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.oqba26.monthlypaymentapp.BuildConfig
import com.oqba26.monthlypaymentapp.data.repository.SettingsRepository
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit

// ⭐️ تغییر به کلاس برای تزریق SettingsRepository
class ApiClient(settingsRepository: SettingsRepository) {

    // تنظیمات Kotlinx.Serialization
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    // ⭐️ ایجاد اینترسپتور احراز هویت
    private val authInterceptor = AuthInterceptor(settingsRepository)

    private val logging = HttpLoggingInterceptor().apply {
        level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY
        else HttpLoggingInterceptor.Level.NONE
    }

    // ⭐️ افزودن AuthInterceptor به OkHttpClient
    private val client: OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .addInterceptor(logging)
        .build()

    val api: ApiService by lazy {
        val contentType = "application/json".toMediaType()

        Retrofit.Builder()
            .baseUrl("http://167.235.136.65:8080/") // آدرس سرور
            .client(client)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
            .create(ApiService::class.java)
    }
}