@file:OptIn(kotlinx.serialization.InternalSerializationApi::class)
package com.oqba26.monthlypaymentapp.utils

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Environment
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import io.ktor.client.*
import io.ktor.client.statement.bodyAsText
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import kotlin.time.Duration.Companion.milliseconds

@Serializable
data class UpdateInfo(
    val versionCode: Int,
    val versionName: String,
    val url: String,
    val releaseNotes: String,
    val isForceUpdate: Boolean = false,
)

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

    fun downloadAndInstall(url: String, fileName: String): Long {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!context.packageManager.canRequestPackageInstalls()) {
                Toast.makeText(context, "لطفاً اجازه نصب برنامه‌های ناشناخته را بدهید", Toast.LENGTH_LONG).show()
                val intent = Intent(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                    data = "package:${context.packageName}".toUri()
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
                return -1L
            }
        }

        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val uri = url.toUri()
        
        val oldFile = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), fileName)
        if (oldFile.exists()) oldFile.delete()

        val request = DownloadManager.Request(uri)
            .setTitle("دریافت به‌روزرسانی")
            .setDescription("نسخه جدید در حال دانلود است...")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, fileName)
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)

        val downloadId = downloadManager.enqueue(request)

        val onComplete = object : BroadcastReceiver() {
            override fun onReceive(receiverContext: Context, intent: Intent) {
                val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L)
                if (id == downloadId) {
                    installApk(fileName)
                    try {
                        receiverContext.unregisterReceiver(this)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
        
        val filter = IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
        ContextCompat.registerReceiver(context, onComplete, filter, ContextCompat.RECEIVER_EXPORTED)
        
        return downloadId
    }

    fun getDownloadProgress(downloadId: Long): Flow<Float> = flow {
        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        var isDownloading = true
        while (isDownloading) {
            val query = DownloadManager.Query().setFilterById(downloadId)
            val cursor = downloadManager.query(query)
            if (cursor != null && cursor.moveToFirst()) {
                val bytesDownloadedIndex = cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
                val bytesTotalIndex = cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
                val statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)

                if (bytesDownloadedIndex != -1 && bytesTotalIndex != -1 && statusIndex != -1) {
                    val bytesDownloaded = cursor.getInt(bytesDownloadedIndex)
                    val bytesTotal = cursor.getInt(bytesTotalIndex)
                    val status = cursor.getInt(statusIndex)

                    if (status == DownloadManager.STATUS_SUCCESSFUL || status == DownloadManager.STATUS_FAILED) {
                        isDownloading = false
                    }

                    if (bytesTotal > 0) {
                        emit(bytesDownloaded.toFloat() / bytesTotal.toFloat())
                    }
                }
                cursor.close()
            } else {
                isDownloading = false
                cursor?.close()
            }
            if (isDownloading) delay(500.milliseconds)
        }
    }.flowOn(Dispatchers.IO)

    private fun installApk(fileName: String) {
        val apkFile = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), fileName)
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
