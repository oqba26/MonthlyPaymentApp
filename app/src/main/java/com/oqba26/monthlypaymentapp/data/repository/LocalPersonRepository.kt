package com.oqba26.monthlypaymentapp.data.repository

import androidx.room.withTransaction
import com.oqba26.monthlypaymentapp.data.dao.PaymentDao
import com.oqba26.monthlypaymentapp.data.dao.PersonDao
import com.oqba26.monthlypaymentapp.data.database.AppDatabase
import com.oqba26.monthlypaymentapp.data.model.BackupData
import com.oqba26.monthlypaymentapp.data.model.PaymentRecord
import com.oqba26.monthlypaymentapp.data.model.Person
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.util.UUID

class LocalPersonRepository(
    private val personDao: PersonDao,
    private val paymentDao: PaymentDao,
    private val database: AppDatabase
) {
    // Reads
    fun getAllPersonsFlow(): Flow<List<Person>> = personDao.getAllPersonsFlow()
    fun getAllPaymentsFlow(): Flow<List<PaymentRecord>> = paymentDao.getAllPaymentsFlow()
    fun getPersonByIdFlow(personId: String): Flow<Person?> = personDao.getPersonByIdFlow(personId)
    fun getPaymentsForPersonFlow(personId: String): Flow<List<PaymentRecord>> =
        paymentDao.getPaymentsForPersonFlow(personId)

    suspend fun syncPersons(persons: List<Person>) {
        val current = getAllPersonsFlow().first()
        val currentByName = current.associateBy { normalize(it.name) }

        val safePersons = persons.map { p ->
            val name = p.name.trim()
            val norm = normalize(name)
            val resolvedId = when {
                p.id.isNotBlank() -> p.id
                currentByName.containsKey(norm) -> currentByName[norm]!!.id
                else -> UUID.randomUUID().toString()
            }
            Person(id = resolvedId, name = name)
        }

        personDao.insertAll(safePersons)
    }

    suspend fun syncPayments(payments: List<PaymentRecord>) =
        paymentDao.insertAllPaymentRecords(payments)

    // ⭐️ تابع جدید و اصلاح شده برای همگام‌سازی کامل
    suspend fun syncAll(persons: List<Person>, payments: List<PaymentRecord>) {
        database.withTransaction {

            // ۱. اجرای منطق حل شناسه (ID Resolution)
            val current = getAllPersonsFlow().first()
            val currentByName = current.associateBy { normalize(it.name) }

            val safePersons = persons.map { p ->
                val name = p.name.trim()
                val norm = normalize(name)
                val resolvedId = when {
                    p.id.isNotBlank() -> p.id
                    currentByName.containsKey(norm) -> currentByName[norm]!!.id
                    else -> UUID.randomUUID().toString()
                }
                Person(id = resolvedId, name = name)
            }

            // ۲. حذف کامل تمام داده‌های محلی
            personDao.deleteAll()
            paymentDao.deleteAllPaymentRecords()

            // ۳. درج داده‌های جدید
            personDao.insertAll(safePersons)
            paymentDao.insertAllPaymentRecords(payments)
        }
    }

    // Backup/Restore
    suspend fun getDataForBackup(): BackupData {
        val persons = getAllPersonsFlow().first()
        val payments = getAllPaymentsFlow().first()
        return BackupData(persons = persons, payments = payments)
    }

    suspend fun restoreBackup(backupData: BackupData) {
        database.withTransaction {
            personDao.deleteAll()
            paymentDao.deleteAllPaymentRecords()

            val safePersons = backupData.persons.map { p ->
                val name = p.name.trim()
                val id = p.id.ifBlank { UUID.randomUUID().toString() }
                Person(id = id, name = name)
            }

            personDao.insertAll(safePersons)
            paymentDao.insertAllPaymentRecords(backupData.payments)
        }
    }

    private fun normalize(name: String): String = name.trim().lowercase()
}