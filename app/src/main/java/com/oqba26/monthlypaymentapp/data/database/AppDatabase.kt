package com.oqba26.monthlypaymentapp.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.oqba26.monthlypaymentapp.data.dao.PaymentDao
import com.oqba26.monthlypaymentapp.data.dao.PersonDao
import com.oqba26.monthlypaymentapp.data.dao.SyncQueueDao
import com.oqba26.monthlypaymentapp.data.model.PaymentRecord
import com.oqba26.monthlypaymentapp.data.model.Person
import com.oqba26.monthlypaymentapp.data.model.SyncQueue

/**
 * دیتابیس محلی اپ.
 *
 * ساخت instance اینجا انجام نمی‌شود؛ تنها مسیر ساخت،
 * [com.oqba26.monthlypaymentapp.core.di.DatabaseModule] است. قبلاً علاوه بر هیلت یک
 * `INSTANCE` دستی هم اینجا بود — دو مسیر ساخت یعنی ریسک دو اتصال جدا به یک فایل دیتابیس.
 *
 * از نسخه ۱۸، `exportSchema = true` است و اسکیما در `app/schemas/` ذخیره می‌شود.
 * مهاجرت‌ها در [Migrations.kt] هستند.
 */
@Database(
    entities = [Person::class, PaymentRecord::class, SyncQueue::class],
    version = 18,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun personDao(): PersonDao
    abstract fun paymentDao(): PaymentDao
    abstract fun syncQueueDao(): SyncQueueDao

    companion object {
        const val DATABASE_NAME = "monthly_payment_db"
    }
}
