package com.oqba26.monthlypaymentapp.core.di

import android.content.Context
import androidx.room.Room
import com.oqba26.monthlypaymentapp.data.dao.PaymentDao
import com.oqba26.monthlypaymentapp.data.dao.PersonDao
import com.oqba26.monthlypaymentapp.data.database.ALL_MIGRATIONS
import com.oqba26.monthlypaymentapp.data.database.AppDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java,
            AppDatabase.DATABASE_NAME
        )
            .addMigrations(*ALL_MIGRATIONS)
            // فقط نسخه‌های ۱ تا ۱۷ اجازه‌ی پاک شدن دارند — آن‌ها قبل از فعال شدن
            // exportSchema ساخته شده‌اند و تاریخچه‌ی اسکیمایشان را نداریم.
            // از ۱۸ به بعد مهاجرت اجباری است: تغییر اسکیمای بدون مهاجرت باعث خطا
            // می‌شود، نه پاک شدن بی‌صدای داده‌ی کاربر.
            .fallbackToDestructiveMigrationFrom(*(1..16).toList().toIntArray())
            .build()
    }

    @Provides
    fun providePersonDao(database: AppDatabase): PersonDao {
        return database.personDao()
    }

    @Provides
    fun providePaymentDao(database: AppDatabase): PaymentDao {
        return database.paymentDao()
    }

    @Provides
    fun provideSyncQueueDao(database: AppDatabase): com.oqba26.monthlypaymentapp.data.dao.SyncQueueDao {
        return database.syncQueueDao()
    }
}
