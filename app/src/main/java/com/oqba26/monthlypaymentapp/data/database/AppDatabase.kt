package com.oqba26.monthlypaymentapp.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import com.oqba26.monthlypaymentapp.data.dao.PaymentDao
import com.oqba26.monthlypaymentapp.data.dao.PersonDao
import com.oqba26.monthlypaymentapp.data.model.BackupData
import com.oqba26.monthlypaymentapp.data.model.PaymentRecord
import com.oqba26.monthlypaymentapp.data.model.Person
import java.util.UUID

@Database(
    entities = [Person::class, PaymentRecord::class],
    version = 11, // حتماً نسخه را بالا ببری که اسکیمای جدید اعمال شود
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun personDao(): PersonDao
    abstract fun paymentDao(): PaymentDao

    @Transaction
    open suspend fun performRestore(backupData: BackupData) {
        // تمیزکاری قبل از درج بکاپ
        val safePersons = backupData.persons.map { p ->
            val name = p.name.trim()
            val id = if (p.id.isNotBlank()) p.id else UUID.randomUUID().toString()
            Person(id = id, name = name)
        }
        personDao().deleteAll()
        paymentDao().deleteAllPaymentRecords()
        personDao().insertAll(safePersons)
        paymentDao().insertAllPaymentRecords(backupData.payments)
    }

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase = getDatabase(context)

        private fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "monthly_payment_db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}