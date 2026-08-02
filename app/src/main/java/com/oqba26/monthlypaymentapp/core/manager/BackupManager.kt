package com.oqba26.monthlypaymentapp.core.manager

import android.content.Context
import android.util.Log
import com.oqba26.monthlypaymentapp.data.model.BackupData
import com.oqba26.monthlypaymentapp.data.repository.LocalPersonRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * اطلاعات یک نسخه‌ی پشتیبان خودکار، برای نمایش در تنظیمات.
 */
data class SnapshotInfo(
    val file: File,
    val reason: String,
    val timestamp: Long,
    val personCount: Int,
    val paymentCount: Int
)

/**
 * تور نجات: قبل از هر عملیات خطرناک، یک نسخه‌ی کامل از داده‌ی محلی روی حافظه‌ی خود گوشی
 * ذخیره می‌کند تا اگر با وجود همه‌ی محافظت‌های دیگر داده‌ای از بین رفت، کاربر بتواند برش گرداند.
 *
 * فایل‌ها در `filesDir/snapshots` ذخیره می‌شوند — حافظه‌ی خصوصی اپ، پس نیازی به مجوز
 * دسترسی به حافظه ندارد و اپ‌های دیگر نمی‌توانند بخوانندش.
 *
 * فقط [MAX_SNAPSHOTS] نسخه‌ی جدیدتر نگه داشته می‌شود؛ قدیمی‌ترها خودکار پاک می‌شوند تا
 * حافظه‌ی گوشی پر نشود.
 */
@Singleton
class BackupManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val localPersonRepository: LocalPersonRepository
) {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val snapshotDir: File
        get() = File(context.filesDir, DIR_NAME).apply { if (!exists()) mkdirs() }

    /**
     * گرفتن یک نسخه‌ی پشتیبان.
     *
     * اگر داده‌ای برای پشتیبان‌گیری نباشد کاری نمی‌کند — snapshot خالی هم بی‌فایده است و هم
     * جای snapshot مفیدِ قبلی را در سقف سه‌تایی می‌گیرد.
     *
     * این تابع عمداً هیچ استثنایی به بیرون نمی‌دهد: شکستِ گرفتن پشتیبان نباید عملیات اصلی
     * (سینک، بازیابی، ارتقای نسخه) را متوقف کند.
     *
     * @param reason شناسه‌ی کوتاه انگلیسی برای علت — بخشی از نام فایل می‌شود.
     * @return فایل ساخته‌شده یا `null` اگر ساخته نشد.
     */
    suspend fun createSnapshot(reason: String): File? = withContext(Dispatchers.IO) {
        try {
            val data = localPersonRepository.getDataForBackup()
            if (data.persons.isEmpty() && data.payments.isEmpty()) {
                Log.d(TAG, "snapshot گرفته نشد: داده‌ای وجود ندارد")
                return@withContext null
            }

            val safeReason = reason.filter { it.isLetterOrDigit() || it == '-' || it == '_' }
                .ifBlank { "manual" }
            val file = File(snapshotDir, "$safeReason-${System.currentTimeMillis()}.json")
            file.writeText(json.encodeToString(data))

            pruneOldSnapshots()
            Log.d(TAG, "snapshot ساخته شد: ${file.name} (${data.persons.size} شخص، ${data.payments.size} پرداخت)")
            file
        } catch (e: Exception) {
            Log.e(TAG, "ساخت snapshot ناموفق بود — عملیات اصلی ادامه می‌یابد", e)
            null
        }
    }

    /** لیست نسخه‌های موجود، جدیدترین اول. فایل‌های خراب نادیده گرفته می‌شوند. */
    suspend fun listSnapshots(): List<SnapshotInfo> = withContext(Dispatchers.IO) {
        snapshotFiles().mapNotNull { file ->
            try {
                val data = json.decodeFromString<BackupData>(file.readText())
                SnapshotInfo(
                    file = file,
                    reason = file.nameWithoutExtension.substringBeforeLast('-'),
                    timestamp = file.nameWithoutExtension.substringAfterLast('-').toLongOrNull()
                        ?: file.lastModified(),
                    personCount = data.persons.size,
                    paymentCount = data.payments.size
                )
            } catch (e: Exception) {
                Log.w(TAG, "snapshot خراب نادیده گرفته شد: ${file.name}", e)
                null
            }
        }
    }

    /**
     * بازگرداندن یک نسخه.
     *
     * قبل از بازگردانی، خودش یک snapshot از وضعیت فعلی می‌گیرد — تا اگر کاربر اشتباهی
     * نسخه‌ی غلط را انتخاب کرد، راه برگشت داشته باشد.
     */
    suspend fun restoreSnapshot(file: File): Boolean = withContext(Dispatchers.IO) {
        try {
            val data = json.decodeFromString<BackupData>(file.readText())
            createSnapshot(REASON_BEFORE_RESTORE)
            localPersonRepository.restoreBackup(data)
            true
        } catch (e: Exception) {
            Log.e(TAG, "بازگردانی snapshot ناموفق بود: ${file.name}", e)
            false
        }
    }

    private fun snapshotFiles(): List<File> =
        snapshotDir.listFiles { f -> f.isFile && f.extension == "json" }
            ?.sortedByDescending { it.lastModified() }
            ?: emptyList()

    private fun pruneOldSnapshots() {
        snapshotFiles().drop(MAX_SNAPSHOTS).forEach { old ->
            if (old.delete()) Log.d(TAG, "snapshot قدیمی حذف شد: ${old.name}")
        }
    }

    companion object {
        private const val TAG = "BackupManager"
        private const val DIR_NAME = "snapshots"
        private const val MAX_SNAPSHOTS = 3

        /** قبل از اولین ادغام داده‌ی سرور در هر اجرا. */
        const val REASON_BEFORE_SYNC = "before-sync"

        /** قبل از بازگردانی نسخه‌ی پشتیبان (دستی یا خودکار). */
        const val REASON_BEFORE_RESTORE = "before-restore"

        /** بعد از ارتقای نسخه‌ی اپ. */
        const val REASON_APP_UPGRADE = "app-upgrade"
    }
}
