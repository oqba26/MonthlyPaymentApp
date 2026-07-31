@file:OptIn(io.github.jan.supabase.annotations.SupabaseInternal::class)

package com.oqba26.monthlypaymentapp.data.remote

import com.oqba26.monthlypaymentapp.BuildConfig
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.serializer.KotlinXSerializer
import io.ktor.client.plugins.HttpTimeout
import kotlinx.serialization.json.Json

object ApiClient {

    val client = createSupabaseClient(
        supabaseUrl = BuildConfig.SUPABASE_URL,
        supabaseKey = BuildConfig.SUPABASE_KEY
    ) {
        defaultSerializer = KotlinXSerializer(Json {
            ignoreUnknownKeys = true
            isLenient = true
            encodeDefaults = true
        })

        httpConfig {
            install(HttpTimeout) {
                requestTimeoutMillis = 30000
                connectTimeoutMillis = 30000
                socketTimeoutMillis = 30000
            }
        }

        install(Postgrest)
        install(Auth)
        install(Realtime)
    }
}
