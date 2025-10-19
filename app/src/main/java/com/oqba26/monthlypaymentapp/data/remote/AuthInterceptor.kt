package com.oqba26.monthlypaymentapp.data.remote

import com.oqba26.monthlypaymentapp.data.repository.SettingsRepository
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(
    private val settingsRepository: SettingsRepository
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        // توکن را به صورت Blocking از DataStore می‌خوانیم
        val authToken = runBlocking {
            settingsRepository.authTokenFlow.firstOrNull()
        }

        val requestBuilder = originalRequest.newBuilder()
        if (authToken != null) {
            // اضافه کردن توکن به هدر (فرمت استاندارد Bearer)
            requestBuilder.header("Authorization", "Bearer $authToken")
        }

        return chain.proceed(requestBuilder.build())
    }
}