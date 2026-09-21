package com.oqba26.monthlypaymentapp.data.remote

import android.util.Log
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.prepareGet
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsChannel
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.readUTF8Line
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.time.Duration.Companion.seconds

@Serializable
data class PocketBaseListResponse<T>(
    val page: Int = 1,
    val perPage: Int = 500,
    val totalPages: Int = 1,
    val totalItems: Int = 0,
    val items: List<T> = emptyList(),
)

@Serializable
data class PocketBaseAuthResponse(
    val token: String,
    val record: JsonObject? = null,
)

@Serializable
data class PocketBaseConnectEvent(
    val clientId: String,
)

@Serializable
data class PocketBaseSubscribeRequest(
    val clientId: String,
    val subscriptions: List<String>,
)

/**
 * کلاینت اختصاصی جهت ارتباط با APIهای REST و سیستم ریل‌تایم (SSE) بک‌اند PocketBase
 */
class PocketBaseClient(
    val baseUrl: String = POCKETBASE_DEFAULT_URL
) {
    companion object {
        const val POCKETBASE_DEFAULT_URL = "http://194.146.68.50/"
        @PublishedApi
        internal const val TAG = "PocketBaseClient"
    }

    val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
        coerceInputValues = true
    }

    val httpClient = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(json)
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 60000
            connectTimeoutMillis = 60000
            socketTimeoutMillis = 60000
        }
    }

    @Volatile
    var authToken: String? = null

    /**
     * ایجاد کاربر جدید در PocketBase
     */
    suspend fun createUser(
        collection: String = "users",
        email: String,
        password: String
    ): String? {
        return try {
            val cleanBaseUrl = baseUrl.trimEnd('/')
            val url = "$cleanBaseUrl/api/collections/$collection/records"
            val response = httpClient.post(url) {
                contentType(ContentType.Application.Json)
                setBody(
                    mapOf(
                        "email" to email,
                        "password" to password,
                        "passwordConfirm" to password
                    )
                )
            }
            if (!response.status.isSuccess()) {
                val errorText = response.bodyAsText()
                Log.e(TAG, "خطا در ساخت کاربر: $errorText")
                return null
            }
            val jsonText = response.bodyAsText()
            val parsed = json.parseToJsonElement(jsonText).jsonObject
            parsed["id"]?.jsonPrimitive?.content
        } catch (e: Exception) {
            Log.e(TAG, "خطا در ایجاد کاربر", e)
            null
        }
    }

    /**
     * احراز هویت با نام کاربری/ایمیل و رمز عبور
     */
    suspend fun authWithPassword(
        collection: String = "users",
        identity: String,
        password: String
    ): PocketBaseAuthResponse {
        val cleanBaseUrl = baseUrl.trimEnd('/')
        val url = "$cleanBaseUrl/api/collections/$collection/auth-with-password"
        val response = httpClient.post(url) {
            contentType(ContentType.Application.Json)
            setBody(mapOf("identity" to identity, "password" to password))
        }

        if (!response.status.isSuccess()) {
            val errorText = response.bodyAsText()
            throw RuntimeException("خطا در احراز هویت PocketBase: $errorText")
        }

        val authResp = response.bodyAsText()
        val parsed = json.decodeFromString<PocketBaseAuthResponse>(authResp)
        authToken = parsed.token
        return parsed
    }

    /**
     * دریافت لیست رکوردها از یک کالکشن
     */
    suspend inline fun <reified T> getRecords(
        collectionName: String,
        page: Int = 1,
        perPage: Int = 500,
        filter: String? = null,
        sort: String? = null
    ): List<T> {
        val cleanBaseUrl = baseUrl.trimEnd('/')
        val response = httpClient.get("$cleanBaseUrl/api/collections/$collectionName/records") {
            parameter("page", page)
            parameter("perPage", perPage)
            if (!filter.isNullOrEmpty()) {
                parameter("filter", filter)
            }
            if (!sort.isNullOrEmpty()) {
                parameter("sort", sort)
            }
            authToken?.let { token ->
                header(HttpHeaders.Authorization, "Bearer $token")
            }
        }

        if (!response.status.isSuccess()) {
            val errorText = response.bodyAsText()
            Log.e(TAG, "خطا در دریافت $collectionName: $errorText")
            return emptyList()
        }

        val bodyText = response.bodyAsText()
        val listResp = json.decodeFromString<PocketBaseListResponse<T>>(bodyText)
        return listResp.items
    }

    /**
     * ایجاد رکورد جدید در PocketBase
     */
    suspend inline fun <reified T> createRecord(
        collectionName: String,
        body: T
    ): T {
        val cleanBaseUrl = baseUrl.trimEnd('/')
        val url = "$cleanBaseUrl/api/collections/$collectionName/records"
        val response = httpClient.post(url) {
            contentType(ContentType.Application.Json)
            authToken?.let { token ->
                header(HttpHeaders.Authorization, "Bearer $token")
            }
            setBody(body)
        }

        if (!response.status.isSuccess()) {
            val errorText = response.bodyAsText()
            throw RuntimeException("خطا در ثبت رکورد $collectionName: $errorText")
        }

        val respText = response.bodyAsText()
        return json.decodeFromString<T>(respText)
    }

    /**
     * بروزرسانی رکورد بر اساس آی‌دی PocketBase
     */
    suspend inline fun <reified T> updateRecord(
        collectionName: String,
        pbRecordId: String,
        body: T
    ): T {
        val cleanBaseUrl = baseUrl.trimEnd('/')
        val url = "$cleanBaseUrl/api/collections/$collectionName/records/$pbRecordId"
        val response = httpClient.patch(url) {
            contentType(ContentType.Application.Json)
            authToken?.let { token ->
                header(HttpHeaders.Authorization, "Bearer $token")
            }
            setBody(body)
        }

        if (!response.status.isSuccess()) {
            val errorText = response.bodyAsText()
            throw RuntimeException("خطا در ویرایش رکورد $collectionName ($pbRecordId): $errorText")
        }

        val respText = response.bodyAsText()
        return json.decodeFromString<T>(respText)
    }

    /**
     * حذف رکورد
     */
    suspend fun deleteRecord(
        collectionName: String,
        pbRecordId: String
    ): Boolean {
        val cleanBaseUrl = baseUrl.trimEnd('/')
        val url = "$cleanBaseUrl/api/collections/$collectionName/records/$pbRecordId"
        val response = httpClient.delete(url) {
            authToken?.let { token ->
                header(HttpHeaders.Authorization, "Bearer $token")
            }
        }
        return response.status.isSuccess()
    }

    /**
     * حذف رکورد بر اساس فیلتر
     */
    suspend fun deleteRecordByFilter(
        collectionName: String,
        filterQuery: String
    ): Boolean {
        return try {
            val cleanBaseUrl = baseUrl.trimEnd('/')
            val searchResp = httpClient.get("$cleanBaseUrl/api/collections/$collectionName/records") {
                parameter("filter", filterQuery)
                authToken?.let { token ->
                    header(HttpHeaders.Authorization, "Bearer $token")
                }
            }

            if (searchResp.status.isSuccess()) {
                val jsonText = searchResp.bodyAsText()
                val parsed = json.parseToJsonElement(jsonText).jsonObject
                val items = parsed["items"]?.jsonArray
                if (!items.isNullOrEmpty()) {
                    items.forEach { item ->
                        val pbId = item.jsonObject["id"]?.jsonPrimitive?.content
                        if (pbId != null) {
                            deleteRecord(collectionName, pbId)
                        }
                    }
                    return true
                }
            }
            false
        } catch (e: Exception) {
            Log.e(TAG, "خطا در حذف رکورد برای $collectionName ($filterQuery)", e)
            false
        }
    }

    /**
     * آپسرت رکورد (اگر بر اساس فیلتر وجود داشت ویرایش می‌شود وگرنه ایجاد می‌شود)
     */
    suspend inline fun <reified T> upsertRecord(
        collectionName: String,
        filterQuery: String,
        body: T
    ) {
        try {
            val cleanBaseUrl = baseUrl.trimEnd('/')
            val searchResp = httpClient.get("$cleanBaseUrl/api/collections/$collectionName/records") {
                parameter("filter", filterQuery)
                authToken?.let { token ->
                    header(HttpHeaders.Authorization, "Bearer $token")
                }
            }

            var existingPbId: String? = null
            if (searchResp.status.isSuccess()) {
                val jsonText = searchResp.bodyAsText()
                val parsed = json.parseToJsonElement(jsonText).jsonObject
                val items = parsed["items"]?.jsonArray
                if (!items.isNullOrEmpty()) {
                    val firstItem = items[0].jsonObject
                    existingPbId = firstItem["id"]?.jsonPrimitive?.content
                }
            }

            if (existingPbId != null) {
                updateRecord(collectionName, existingPbId, body)
            } else {
                createRecord(collectionName, body)
            }
        } catch (e: Exception) {
            Log.e(TAG, "خطا در upsert برای $collectionName ($filterQuery)", e)
            throw e
        }
    }

    /**
     * اتصال ریل‌تایم SSE
     */
    fun listenRealtime(subscriptions: List<String>): Flow<String> = flow {
        val cleanBaseUrl = baseUrl.trimEnd('/')
        while (true) {
            try {
                httpClient.prepareGet("$cleanBaseUrl/api/realtime") {
                    header("Accept", "text/event-stream")
                    authToken?.let { token ->
                        header(HttpHeaders.Authorization, "Bearer $token")
                    }
                }.execute { response ->
                    val channel: ByteReadChannel = response.bodyAsChannel()
                    var currentEvent = ""

                    while (!channel.isClosedForRead) {
                        val line = channel.readUTF8Line() ?: break
                        val trimmed = line.trim()
                        if (trimmed.startsWith("event:")) {
                            currentEvent = trimmed.substringAfter("event:").trim()
                        } else if (trimmed.startsWith("data:")) {
                            val dataJson = trimmed.substringAfter("data:").trim()
                            if (currentEvent == "PB_CONNECT") {
                                val connectObj = json.decodeFromString<PocketBaseConnectEvent>(dataJson)
                                subscribeRealtime(cleanBaseUrl, connectObj.clientId, subscriptions)
                                Log.d(TAG, "اتصال ریل‌تایم برقرار شد. ClientId: ${connectObj.clientId}")
                            } else if (currentEvent.isNotEmpty()) {
                                emit(currentEvent)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "خطا در ریل‌تایم PocketBase، تلاش مجدد...", e)
            }
            delay(3.seconds)
        }
    }

    private suspend fun subscribeRealtime(cleanBaseUrl: String, clientId: String, subscriptions: List<String>) {
        val url = "$cleanBaseUrl/api/realtime"
        httpClient.post(url) {
            contentType(ContentType.Application.Json)
            authToken?.let { token ->
                header(HttpHeaders.Authorization, "Bearer $token")
            }
            setBody(PocketBaseSubscribeRequest(clientId = clientId, subscriptions = subscriptions))
        }
    }
}
