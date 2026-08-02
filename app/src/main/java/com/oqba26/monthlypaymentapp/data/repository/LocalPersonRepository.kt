@file:Suppress("unused")

package com.oqba26.monthlypaymentapp.data.repository

import androidx.room.withTransaction
import com.oqba26.monthlypaymentapp.data.dao.PaymentDao
import com.oqba26.monthlypaymentapp.data.dao.PersonDao
import com.oqba26.monthlypaymentapp.data.dao.SyncQueueDao
import com.oqba26.monthlypaymentapp.data.database.AppDatabase
import com.oqba26.monthlypaymentapp.data.model.BackupData
import com.oqba26.monthlypaymentapp.data.model.PaymentRecord
import com.oqba26.monthlypaymentapp.data.model.Person
import com.oqba26.monthlypaymentapp.data.model.SyncQueue
import com.oqba26.monthlypaymentapp.data.model.SyncOperation
import com.oqba26.monthlypaymentapp.data.model.SyncType
import com.oqba26.monthlypaymentapp.data.sync.SyncMerger
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class LocalPersonRepository(
    private val personDao: PersonDao,
    private val paymentDao: PaymentDao,
    private val syncQueueDao: SyncQueueDao,
    private val database: AppDatabase
) {
    // Reads
    fun getAllPersonsFlow(): Flow<List<Person>> = personDao.getAllPersonsFlow()
    fun getAllPaymentsFlow(): Flow<List<PaymentRecord>> = paymentDao.getAllPaymentsFlow()
    fun getPersonByIdFlow(personId: String): Flow<Person?> = personDao.getPersonByIdFlow(personId)
    fun getPaymentsForPersonFlow(personId: String): Flow<List<PaymentRecord>> =
        paymentDao.getPaymentsForPersonFlow(personId)

    /**
     * ادغام غیرمخربِ داده‌ی سرور با داده‌ی محلی.
     *
     * جانشین `syncAll` قبلی است که اول `deleteAll()` می‌زد و بعد داده‌ی سرور را درج می‌کرد؛
     * آن رفتار هر تغییر محلیِ هنوز-ارسال-نشده را نابود می‌کرد.
     *
     * اینجا هیچ `deleteAll`ی وجود ندارد. تصمیمِ «چه چیزی حذف شود» را [SyncMerger] می‌گیرد
     * (که تست واحد دارد) و رکوردهای موجود در صف سینک یا دارای `needsSync` را کنار می‌گذارد.
     *
     * فقط با داده‌ی **معتبرِ** سرور صدا زده شود — یعنی بعد از اینکه
     * [com.oqba26.monthlypaymentapp.data.repository.NetworkRepository.refresh] مقدار `true` برگردانده باشد.
     */
    suspend fun mergeFromServer(persons: List<Person>, payments: List<PaymentRecord>) {
        database.withTransaction {
            // خواندن با تابع suspend و نه collect کردن Flow:
            // collect کردن Flow روم داخل تراکنش خطر deadlock دارد.
            val localPersons = personDao.getAllPersons()
            val localPayments = paymentDao.getAllPayments()
            val pendingIds = syncQueueDao.getAllPending().map { it.entityId }.toSet()

            val personPlan = SyncMerger.planPersons(localPersons, persons, pendingIds)
            val paymentPlan = SyncMerger.planPayments(localPayments, payments, pendingIds)

            // پرداخت‌ها اول حذف می‌شوند تا اگر کلید خارجی اضافه شد، ترتیب درست باشد.
            if (paymentPlan.deleteIds.isNotEmpty()) paymentDao.deleteByIds(paymentPlan.deleteIds)
            if (personPlan.deleteIds.isNotEmpty()) personDao.deleteByIds(personPlan.deleteIds)

            if (personPlan.upserts.isNotEmpty()) personDao.insertAll(personPlan.upserts)
            if (paymentPlan.upserts.isNotEmpty()) paymentDao.insertAllPaymentRecords(paymentPlan.upserts)
        }
    }

    // Backup/Restore
    suspend fun getDataForBackup(): BackupData {
        val persons = personDao.getAllPersons()
        val payments = paymentDao.getAllPayments()
        return BackupData(persons = persons, payments = payments)
    }

    /**
     * بازگردانی کامل از یک نسخه‌ی پشتیبان.
     *
     * برخلاف [mergeFromServer]، این عملیات عمداً مخرب است — کاربر صریحاً خواسته وضعیت فعلی
     * با نسخه‌ی پشتیبان جایگزین شود. صدازننده باید قبلش snapshot بگیرد
     * ([com.oqba26.monthlypaymentapp.core.manager.BackupManager.restoreSnapshot] این کار را می‌کند).
     *
     * صف سینک هم بازسازی می‌شود. قبلاً دست‌نخورده می‌ماند و دو مشکل داشت:
     *  - آیتم‌های قدیمی به رکوردهای پاک‌شده اشاره می‌کردند.
     *  - داده‌ی بازیابی‌شده هرگز به سرور نمی‌رفت، پس اولین سینکِ بعدی دوباره پاکش می‌کرد.
     */
    suspend fun restoreBackup(backupData: BackupData) {
        database.withTransaction {
            personDao.deleteAll()
            paymentDao.deleteAllPaymentRecords()
            syncQueueDao.deleteAll()

            val safePersons = backupData.persons.map { p ->
                val name = p.name.trim()
                val id = p.id.ifBlank { UUID.randomUUID().toString() }
                p.copy(id = id, name = name, needsSync = true)
            }
            val safePayments = backupData.payments.map { it.copy(needsSync = true) }

            personDao.insertAll(safePersons)
            paymentDao.insertAllPaymentRecords(safePayments)

            // همه‌ی رکوردهای بازیابی‌شده باید دوباره به سرور برسند.
            safePersons.forEach {
                syncQueueDao.insert(
                    SyncQueue(entityId = it.id, type = SyncType.PERSON, operation = SyncOperation.INSERT)
                )
            }
            safePayments.forEach {
                syncQueueDao.insert(
                    SyncQueue(entityId = it.id, type = SyncType.PAYMENT, operation = SyncOperation.INSERT)
                )
            }
        }
    }

    // Local-First Write Operations
    suspend fun insertPersonLocally(person: Person) {
        database.withTransaction {
            personDao.insertAll(listOf(person.copy(needsSync = true)))
            syncQueueDao.insert(SyncQueue(entityId = person.id, type = SyncType.PERSON, operation = SyncOperation.INSERT))
        }
    }

    suspend fun updatePersonLocally(person: Person) {
        database.withTransaction {
            personDao.insertAll(listOf(person.copy(needsSync = true)))
            syncQueueDao.insert(SyncQueue(entityId = person.id, type = SyncType.PERSON, operation = SyncOperation.UPDATE))
        }
    }

    suspend fun archivePersonLocally(personId: String, isArchived: Boolean) {
        database.withTransaction {
            personDao.updateArchivedStatus(personId, isArchived)
            syncQueueDao.insert(SyncQueue(entityId = personId, type = SyncType.PERSON, operation = SyncOperation.UPDATE))
        }
    }

    suspend fun deletePersonLocally(personId: String) {
        database.withTransaction {
            personDao.deleteById(personId)
            syncQueueDao.insert(SyncQueue(entityId = personId, type = SyncType.PERSON, operation = SyncOperation.DELETE))
        }
    }

    suspend fun insertPaymentLocally(payment: PaymentRecord) {
        database.withTransaction {
            paymentDao.insertAllPaymentRecords(listOf(payment.copy(needsSync = true)))
            syncQueueDao.insert(SyncQueue(entityId = payment.id, type = SyncType.PAYMENT, operation = SyncOperation.INSERT))
        }
    }

    suspend fun deletePaymentLocally(paymentId: String) {
        database.withTransaction {
            paymentDao.deleteById(paymentId)
            syncQueueDao.insert(SyncQueue(entityId = paymentId, type = SyncType.PAYMENT, operation = SyncOperation.DELETE))
        }
    }

    /** خواندن یک‌باره (نه Flow) — برای استفاده داخل تراکنش و در [com.oqba26.monthlypaymentapp.data.sync.SyncManager]. */
    suspend fun getAllPersons(): List<Person> = personDao.getAllPersons()
    suspend fun getAllPayments(): List<PaymentRecord> = paymentDao.getAllPayments()

    fun getSyncQueueFlow() = syncQueueDao.getAllPendingFlow()
    suspend fun getPendingSyncItems() = syncQueueDao.getAllPending()
    suspend fun removeSyncItem(item: SyncQueue) = syncQueueDao.delete(item)

    /** ثبت تلاش ناموفق روی آیتم صف — آیتم نگه داشته می‌شود تا دور بعد. */
    suspend fun recordSyncAttempt(itemId: String, attemptCount: Int, at: Long) =
        syncQueueDao.recordAttempt(itemId, attemptCount, at)

    /** حذف چند آیتم صف با هم — برای زمانی که coalesce آن‌ها را بی‌اثر تشخیص می‌دهد. */
    suspend fun removeSyncItems(items: List<SyncQueue>) {
        database.withTransaction { items.forEach { syncQueueDao.delete(it) } }
    }
    suspend fun clearSyncQueue() = syncQueueDao.deleteAll()
}