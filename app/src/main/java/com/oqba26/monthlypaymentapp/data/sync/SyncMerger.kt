package com.oqba26.monthlypaymentapp.data.sync

import com.oqba26.monthlypaymentapp.data.model.PaymentRecord
import com.oqba26.monthlypaymentapp.data.model.Person
import java.util.UUID

/**
 * نتیجه‌ی تصمیم‌گیری برای همگام‌سازی: چه رکوردهایی نوشته/به‌روز شوند و چه شناسه‌هایی حذف شوند.
 */
data class MergePlan<T>(
    val upserts: List<T>,
    val deleteIds: List<String>
) {
    val isEmpty: Boolean get() = upserts.isEmpty() && deleteIds.isEmpty()
}

/**
 * منطق خالصِ ادغام داده‌ی سرور با داده‌ی محلی.
 *
 * این کلاس عمداً هیچ وابستگی‌ای به Room، Context یا شبکه ندارد تا کاملاً unit-testable باشد؛
 * تصمیم «چه چیزی حذف شود» حساس‌ترین بخش اپ است و باید تست داشته باشد.
 *
 * ## چرا این کلاس وجود دارد
 * پیاده‌سازی قبلی (`LocalPersonRepository.syncAll`) اول `deleteAll()` می‌زد و بعد داده‌ی سرور را
 * درج می‌کرد. نتیجه‌اش این بود که هر تغییر محلیِ هنوز-سینک-نشده‌ای نابود می‌شد — کاربر آفلاین
 * پرداختی ثبت می‌کرد و آن پرداخت بی‌صدا از بین می‌رفت.
 *
 * ## قاعده‌ی حاکم
 * **سرور منبع حقیقت است، مگر برای رکوردهایی که تغییر محلیِ ارسال‌نشده دارند.**
 * آن رکوردها دست‌نخورده می‌مانند تا [SyncManager] موفق شود آن‌ها را بفرستد و از صف حذفشان کند؛
 * از آن لحظه به بعد سرور دربارهٔ آن‌ها هم حرف آخر را می‌زند.
 */
object SyncMerger {

    /** نرمال‌سازی نام برای تطبیق رکوردهای سرور که شناسه ندارند. */
    fun normalizeName(name: String): String = name.trim().lowercase()

    /**
     * مجموعهٔ شناسه‌هایی که **نباید** لمس شوند.
     *
     * دو منبع دارد:
     *  - `pendingIds`: آیتم‌های موجود در صف سینک.
     *  - پرچم `needsSync` روی خودِ رکورد محلی.
     *
     * منبع دوم صرفاً تکرار اولی نیست: اگر صف به هر دلیلی از دست رفته باشد (مثلاً به‌خاطر باگ
     * قبلی که آیتم‌ها را بی‌اینکه واقعاً ارسال شوند حذف می‌کرد)، پرچم `needsSync` هنوز روی رکورد
     * هست و همان جلوی پاک شدنش را می‌گیرد.
     */
    private fun protectedPersonIds(local: List<Person>, pendingIds: Set<String>): Set<String> =
        pendingIds + local.filter { it.needsSync }.map { it.id }

    private fun protectedPaymentIds(local: List<PaymentRecord>, pendingIds: Set<String>): Set<String> =
        pendingIds + local.filter { it.needsSync }.map { it.id }

    /**
     * @param idGenerator تولیدکنندهٔ شناسه؛ فقط برای تست‌پذیری قابل تزریق است.
     */
    fun planPersons(
        local: List<Person>,
        server: List<Person>,
        pendingIds: Set<String>,
        idGenerator: () -> String = { UUID.randomUUID().toString() }
    ): MergePlan<Person> {
        val protectedIds = protectedPersonIds(local, pendingIds)
        val localByName = local.associateBy { normalizeName(it.name) }

        // حل شناسه: رکورد سرور که id ندارد، اگر همنامِ محلی داشت شناسهٔ او را می‌گیرد.
        // (این منطق از پیاده‌سازی قبلی حفظ شده و رفتار سرورهای قدیمی را پوشش می‌دهد.)
        val resolved = server.map { person ->
            val name = person.name.trim()
            val resolvedId = when {
                person.id.isNotBlank() -> person.id
                else -> localByName[normalizeName(name)]?.id ?: idGenerator()
            }
            person.copy(id = resolvedId, name = name, needsSync = false)
        }

        val serverIds = resolved.map { it.id }.toSet()

        return MergePlan(
            upserts = resolved.filterNot { it.id in protectedIds },
            deleteIds = local
                .map { it.id }
                .filterNot { it in serverIds || it in protectedIds }
        )
    }

    fun planPayments(
        local: List<PaymentRecord>,
        server: List<PaymentRecord>,
        pendingIds: Set<String>
    ): MergePlan<PaymentRecord> {
        val protectedIds = protectedPaymentIds(local, pendingIds)
        val serverIds = server.map { it.id }.toSet()

        return MergePlan(
            upserts = server
                .filterNot { it.id in protectedIds }
                .map { it.copy(needsSync = false) },
            deleteIds = local
                .map { it.id }
                .filterNot { it in serverIds || it in protectedIds }
        )
    }
}
