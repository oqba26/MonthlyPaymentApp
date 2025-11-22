package com.oqba26.monthlypaymentapp.data.remote

import com.oqba26.monthlypaymentapp.data.repository.SettingsRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(
    private val settingsRepository: SettingsRepository
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        // Get the token from DataStore, blocking the current thread.
        // This is acceptable for an I/O-bound operation in an interceptor.
        val authToken = runBlocking {
            settingsRepository.authTokenFlow.first() // Use first() to wait for a value
        }

        val originalRequest = chain.request()
        val requestBuilder = originalRequest.newBuilder()

        // Add the Authorization header only if the token is not null or blank
        if (!authToken.isNullOrBlank()) {
            requestBuilder.header("Authorization", "Bearer $authToken")
        }

        return chain.proceed(requestBuilder.build())
    }
}
