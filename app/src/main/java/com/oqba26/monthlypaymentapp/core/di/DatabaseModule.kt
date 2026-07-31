package com.oqba26.monthlypaymentapp.core.di

import android.content.Context
import com.oqba26.monthlypaymentapp.data.dao.PaymentDao
import com.oqba26.monthlypaymentapp.data.dao.PersonDao
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
        return AppDatabase.getInstance(context)
    }

    @Provides
    fun providePersonDao(database: AppDatabase): PersonDao {
        return database.personDao()
    }

    @Provides
    fun providePaymentDao(database: AppDatabase): PaymentDao {
        return database.paymentDao()
    }
}
