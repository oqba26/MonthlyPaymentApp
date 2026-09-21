@file:OptIn(InternalSerializationApi::class)
package com.oqba26.monthlypaymentapp.utils

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Environment
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.utils.io.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

@Serializable
data class UpdateInfo(
    val versionCode: Int,
    val versionName: String,
    val url: String,
    val releaseNotes: String,
    val isForceUpdate: Boolean = false,
)

sealed class DownloadState {
    data class Progress(val progress: Float) : DownloadState()
    data class Success(val file: File) : DownloadState()
    data class Error(val message: String) : DownloadState()
}

class UpdateManager(private val context: Context) {

    private val updateUrl = "https://raw.githubusercontent.com/oqba26/MonthlyPaymentApp/main/update.json"

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    private val client = HttpClient {
        install(ContentNegotiation) {
            json(json)
        }
    }

    suspend fun checkForUpdate(): UpdateInfo? = withContext(Dispatchers.IO) {
        if (!isNetworkAvailable()) return@withContext null

        try {
            val timestamp = System.currentTimeMillis()
            val urlWithParams = if (updateUrl.contains("?")) "$updateUrl&t=$timestamp" else "$updateUrl?t=$timestamp"

            val responseText: String = client.get(urlWithParams).bodyAsText()
            val updateInfo: UpdateInfo = json.decodeFromString(responseText)

            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            val currentVersionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                pInfo.longVersionCode.toInt()
            } else {
                @Suppress("DEPRECATION")
                pInfo.versionCode
            }

            if (updateInfo.versionCode > currentVersionCode) {
                return@withContext updateInfo
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        null
    }

    fun downloadApk(url: String, fileName: String): Flow<DownloadState> = flow {
        try {
            val destinationFile = File(
                context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
                fileName
            )
            if (destinationFile.exists()) {
                destinationFile.delete()
            }

            client.prepareGet(url) {
                header(HttpHeaders.UserAgent, "Mozilla/5.0 (Linux; Android 10; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36")
            }.execute { response ->
                if (!response.status.isSuccess()) {
                    emit(DownloadState.Error("خطا در دانلود فایل (کد ${response.status.value})"))
                    return@execute
                }

                val channel: ByteReadChannel = response.bodyAsChannel()
                val contentLength = response.contentLength() ?: -1L

                destinationFile.outputStream().use { output ->
                    val buffer = ByteArray(16384)
                    var bytesCopied = 0L
                    while (!channel.isClosedForRead) {
                        val read = channel.readAvailable(buffer, 0, buffer.size)
                        if (read <= 0) break
                        output.write(buffer, 0, read)
                        bytesCopied += read

                        if (contentLength > 0) {
                            val progress = bytesCopied.toFloat() / contentLength.toFloat()
                            emit(DownloadState.Progress(progress.coerceIn(0f, 1f)))
                        } else {
                            emit(DownloadState.Progress(0f))
                        }
                    }
                }
                emit(DownloadState.Progress(1f))
                emit(DownloadState.Success(destinationFile))
            }
        } catch (e: Exception) {
            emit(DownloadState.Error(e.localizedMessage ?: "خطای ارتباط با سرور"))
        }
    }.flowOn(Dispatchers.IO)

    fun installApk(apkFile: File) {
        if (!apkFile.exists()) return

        try {
            val contentUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(contentUri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "خطا در اجرای فایل نصب: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    fun openInBrowser(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, url.toUri()).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false
        return when {
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
            else -> false
        }
    }
}
