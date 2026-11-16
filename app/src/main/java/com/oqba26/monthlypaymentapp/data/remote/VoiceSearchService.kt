@file:OptIn(kotlinx.serialization.InternalSerializationApi::class)

package com.oqba26.monthlypaymentapp.data.remote

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
internal data class SttResponse(
    val text: String = ""
)

@Singleton
class VoiceSearchService @Inject constructor() {

    companion object {
        private const val TAG = "VoiceSearchService"
        const val DEFAULT_STT_URL = "http://194.146.68.50:8096/stt"
    }

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun transcribeAudio(
        audioFile: File,
        serverUrl: String = DEFAULT_STT_URL
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            if (!audioFile.exists() || audioFile.length() == 0L) {
                return@withContext Result.failure(IllegalArgumentException("فایل صوتی خالی یا نامعتبر است"))
            }

            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(
                    "file",
                    audioFile.name,
                    audioFile.asRequestBody("audio/m4a".toMediaTypeOrNull())
                )
                .build()

            val request = Request.Builder()
                .url(serverUrl)
                .post(requestBody)
                .build()

            val response = okHttpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                val code = response.code
                val errBody = response.body?.string().orEmpty()
                Log.e(TAG, "STT server error: code=$code, body=$errBody")
                return@withContext Result.failure(Exception("خطای سرور: $code"))
            }

            val responseString = response.body?.string().orEmpty()
            val parsed = json.decodeFromString<SttResponse>(responseString)
            val cleanedText = parsed.text.trim().trimEnd('.', '،', '!', '؟')
            Log.d(TAG, "Recognized text: $cleanedText")
            Result.success(cleanedText)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to transcribe audio", e)
            Result.failure(e)
        } finally {
            try {
                if (audioFile.exists()) {
                    audioFile.delete()
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to delete temp audio file", e)
            }
        }
    }
}
