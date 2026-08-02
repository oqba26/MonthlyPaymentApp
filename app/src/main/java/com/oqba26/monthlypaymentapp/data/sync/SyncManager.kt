package com.oqba26.monthlypaymentapp.data.sync

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import com.oqba26.monthlypaymentapp.data.model.SyncOperation
import com.oqba26.monthlypaymentapp.data.model.SyncQueue
import com.oqba26.monthlypaymentapp.data.model.SyncType
import com.oqba26.monthlypaymentapp.data.repository.LocalPersonRepository
import com.oqba26.monthlypaymentapp.data.repository.NetworkRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ارسال تغییرات محلی به سرور.
 *
 * ## چه چیزی نسبت به نسخه‌ی قبل عوض شد
 *
 * ۱. **حلقه دیگر خودش را cancel نمی‌کند.** نسخه‌ی قبلی `collectLatest` روی Flow صف بود، ولی
 *    پردازش صف خودش صف را تغییر می‌دهد → Flow دوباره emit می‌کند → `collectLatest` پردازشِ
 *    در جریان را لغو می‌کند. اینجا Flow فقط نقش «زنگ» را دارد و پردازش در یک حلقه‌ی
 *    تک‌مصرف‌کننده با [Mutex] انجام می‌شود.
 *
 * ۲. **هیچ آیتمی بدون رسیدن به سرور حذف نمی‌شود.** قبلاً اگر رکورد محلی پیدا نمی‌شد،
 *    `else true` برمی‌گشت و آیتم به‌عنوان «موفق» از صف پاک می‌شد — یعنی داده‌ی کاربر بی‌صدا
 *    گم می‌شد. حالا آیتم نگه داشته می‌شود و تلاشش ثبت می‌گردد.
 *
 * ۳. **retry با backoff.** قبلاً اولین شکست `break` می‌زد و تا trigger بعدی هیچ کاری نمی‌شد؛
 *    آفلاین که بودی، هیچ‌وقت خودش دوباره تلاش نمی‌کرد. حالا هر آیتم شکست‌خورده عقب می‌افتد
 *    ولی حلقه بقیه را ادامه می‌دهد، و برگشتن شبکه خودش یک trigger است.
 *
 * ۴. **coalesce.** چند عمل روی یک رکورد به یک عمل نهایی تبدیل می‌شود؛ این همان چیزی است که
 *    حالت متناقض «آیتم صف هست ولی رکورد محلی نیست» را از بین می‌برد.
 */
@Singleton
class SyncManager @Inject constructor(
    private val localRepository: LocalPersonRepository,
    private val networkRepository: NetworkRepository,
    @ApplicationContext private val context: Context
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    /**
     * زنگ شروع پردازش. `CONFLATED` است چون چند زنگ پشت‌سرهم معنایی جز «یک بار دیگر بررسی کن»
     * ندارند؛ صف‌کردنشان فقط کار تکراری می‌سازد.
     */
    private val trigger = Channel<Unit>(Channel.CONFLATED)

    /** تضمین می‌کند فقط یک پردازش همزمان در جریان باشد. */
    private val mutex = Mutex()

    @Volatile
    private var started = false

    fun startSync() {
        if (started) return
        started = true

        // مصرف‌کننده‌ی تنها
        scope.launch {
            for (ignored in trigger) {
                mutex.withLock {
                    try {
                        processQueue()
                    } catch (e: Exception) {
                        // حلقه هرگز نباید بمیرد؛ وگرنه سینک تا ری‌استارت اپ متوقف می‌ماند.
                        Log.e(TAG, "خطای غیرمنتظره در پردازش صف", e)
                    }
                }
            }
        }

        // زنگ ۱: هر تغییر در صف
        scope.launch {
            localRepository.getSyncQueueFlow().collect { queue ->
                if (queue.isNotEmpty()) trigger.trySend(Unit)
            }
        }

        // زنگ ۲: برگشتن اینترنت
        registerNetworkCallback()

        // زنگ ۳: خودِ راه‌اندازی (آیتم‌های باقی‌مانده از اجرای قبلی)
        trigger.trySend(Unit)
    }

    private fun registerNetworkCallback() {
        try {
            val cm = context.getSystemService(ConnectivityManager::class.java) ?: return
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
            cm.registerNetworkCallback(request, object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    Log.d(TAG, "شبکه برگشت — تلاش دوباره برای سینک")
                    trigger.trySend(Unit)
                }
            })
        } catch (e: Exception) {
            // نبود این زنگ فقط باعث می‌شود سینک دیرتر (با زنگ‌های دیگر) اتفاق بیفتد.
            Log.e(TAG, "ثبت NetworkCallback ناموفق بود", e)
        }
    }

    private suspend fun processQueue() {
        val raw = localRepository.getPendingSyncItems()
        if (raw.isEmpty()) return

        val (effective, obsolete) = coalesce(raw)

        // آیتم‌هایی که coalesce بی‌اثر تشخیص داد (مثلاً INSERT بعدش DELETE، یا UPDATEهای
        // قدیمی‌ترِ یک رکورد) هرگز به سرور نمی‌رسند چون لازم نیست برسند.
        if (obsolete.isNotEmpty()) {
            localRepository.removeSyncItems(obsolete)
            Log.d(TAG, "${obsolete.size} آیتم تکراری/بی‌اثر از صف حذف شد")
        }

        val now = System.currentTimeMillis()
        val persons = localRepository.getAllPersons().associateBy { it.id }
        val payments = localRepository.getAllPayments().associateBy { it.id }
        var failed = false

        for (item in effective) {
            if (!isDue(item, now)) continue

            val success = try {
                when (item.type) {
                    SyncType.PERSON -> syncPerson(item, persons[item.entityId])
                    SyncType.PAYMENT -> syncPayment(item, payments[item.entityId])
                }
            } catch (e: Exception) {
                Log.e(TAG, "خطا در سینک آیتم ${item.id}", e)
                false
            }

            if (success) {
                localRepository.removeSyncItem(item)
            } else {
                // آیتم نگه داشته می‌شود. `break` نمی‌زنیم: شکست یک رکورد نباید بقیه را
                // معطل کند (مثلاً یک رکورد خرابِ همیشه-ناموفق کل صف را قفل می‌کرد).
                localRepository.recordSyncAttempt(item.id, item.attemptCount + 1, now)
                Log.w(TAG, "سینک آیتم ${item.id} ناموفق (تلاش ${item.attemptCount + 1}) — در صف می‌ماند")
                failed = true
            }
        }

        // اگر چیزی شکست خورد، خودمان زنگ بعدی را کوک می‌کنیم. بدون این، آیتمِ شکست‌خورده
        // تا تغییر بعدیِ صف یا برگشتن شبکه معطل می‌ماند — و در حالتی که شبکه وصل است ولی
        // سرور خطا می‌دهد، هیچ‌کدام از آن دو رخ نمی‌دهد.
        if (failed) scheduleRetry()
    }

    /** یک زنگ تأخیری برای آیتم‌های شکست‌خورده. */
    private fun scheduleRetry() {
        scope.launch {
            delay(RETRY_TICK_MS)
            trigger.trySend(Unit)
        }
    }

    /**
     * آیا زمان تلاش بعدیِ این آیتم رسیده؟
     *
     * آیتم تازه (`lastAttemptAt == 0`) همیشه due است. آیتم شکست‌خورده به اندازه‌ی
     * [backoffMillis] صبر می‌کند تا در حالت آفلاین، لاگ و مصرف باتری منفجر نشود.
     */
    private fun isDue(item: SyncQueue, now: Long): Boolean =
        item.lastAttemptAt == 0L || now - item.lastAttemptAt >= backoffMillis(item.attemptCount)

    /** ۲ ثانیه، ۴، ۸، ... تا سقف ۵ دقیقه. */
    private fun backoffMillis(attemptCount: Int): Long {
        if (attemptCount <= 0) return 0L
        val exponent = attemptCount.coerceAtMost(8) // جلوگیری از سرریز
        val delay = BASE_BACKOFF_MS shl (exponent - 1)
        return delay.coerceAtMost(MAX_BACKOFF_MS)
    }

    /**
     * چند عمل روی یک رکورد را به یک عمل نهایی تبدیل می‌کند.
     *
     * قواعد (به ترتیب زمانی روی هر `entityId`):
     *  - `INSERT` سپس `DELETE` → **هیچ‌کدام**. رکورد هرگز به سرور نرفته، پس چیزی برای حذف نیست.
     *  - `INSERT` سپس `UPDATE` → `INSERT` (که با upsert همان حالت نهایی را می‌فرستد).
     *  - چند `UPDATE` → آخری. حالت فعلیِ رکورد فرستاده می‌شود، پس میانی‌ها بی‌معنی‌اند.
     *  - `DELETE` آخرین عمل باشد → `DELETE` و همه‌ی عمل‌های قبلی بی‌اثر می‌شوند.
     *
     * @return جفتِ (آیتم‌هایی که باید پردازش شوند، آیتم‌هایی که باید بی‌پردازش حذف شوند)
     */
    private fun coalesce(items: List<SyncQueue>): Pair<List<SyncQueue>, List<SyncQueue>> {
        val effective = mutableListOf<SyncQueue>()
        val obsolete = mutableListOf<SyncQueue>()

        items.groupBy { it.entityId to it.type }.forEach { (_, group) ->
            val ordered = group.sortedBy { it.createdAt }
            val last = ordered.last()
            val hadInsert = ordered.any { it.operation == SyncOperation.INSERT }

            when {
                // ساخته و بعد پاک شد، بدون اینکه هرگز به سرور برسد → کل ماجرا بی‌اثر است.
                last.operation == SyncOperation.DELETE && hadInsert -> obsolete += ordered

                // ساخته شد (و شاید بعد ویرایش) → یک INSERT کافی است؛ upsert حالت فعلی را می‌فرستد.
                hadInsert -> {
                    val insert = ordered.first { it.operation == SyncOperation.INSERT }
                    effective += insert
                    obsolete += ordered.filter { it.id != insert.id }
                }

                // فقط UPDATE/DELETE → آخری حرف آخر را می‌زند.
                else -> {
                    effective += last
                    obsolete += ordered.filter { it.id != last.id }
                }
            }
        }

        return effective.sortedBy { it.createdAt } to obsolete
    }

    private suspend fun syncPerson(
        item: SyncQueue,
        person: com.oqba26.monthlypaymentapp.data.model.Person?
    ): Boolean = when (item.operation) {
        SyncOperation.DELETE -> networkRepository.deletePersonAndPayments(item.entityId)

        // INSERT و UPDATE هر دو upsert می‌شوند: idempotent است، پس retry بی‌خطر است و
        // خطای ۴۰۹ (تکراری بودن) هم دیگر رخ نمی‌دهد.
        SyncOperation.INSERT, SyncOperation.UPDATE -> {
            if (person == null) {
                // حالت متناقض. قبلاً اینجا `true` برمی‌گشت و آیتم پاک می‌شد؛ نتیجه‌اش
                // گم شدن دائمی داده بود. حالا آیتم می‌ماند تا وضعیت قابل بررسی باشد.
                Log.e(TAG, "رکورد محلی شخص ${item.entityId} پیدا نشد — آیتم در صف نگه داشته شد")
                false
            } else {
                networkRepository.addPerson(person)
            }
        }
    }

    private suspend fun syncPayment(
        item: SyncQueue,
        payment: com.oqba26.monthlypaymentapp.data.model.PaymentRecord?
    ): Boolean = when (item.operation) {
        SyncOperation.DELETE -> networkRepository.deletePayment(item.entityId)

        SyncOperation.INSERT, SyncOperation.UPDATE -> {
            if (payment == null) {
                Log.e(TAG, "رکورد محلی پرداخت ${item.entityId} پیدا نشد — آیتم در صف نگه داشته شد")
                false
            } else {
                // upsert است، پس ویرایش پرداخت هم درست کار می‌کند؛ قبلاً insert بود و
                // روی رکورد موجود شکست می‌خورد.
                networkRepository.addPayment(payment)
            }
        }
    }

    private companion object {
        const val TAG = "SyncManager"
        const val BASE_BACKOFF_MS = 2_000L
        const val MAX_BACKOFF_MS = 5 * 60 * 1000L
        const val RETRY_TICK_MS = 15_000L
    }
}
