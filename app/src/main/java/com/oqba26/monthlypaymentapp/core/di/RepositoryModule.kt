package com.oqba26.monthlypaymentapp.core.di

import android.content.Context
import com.oqba26.monthlypaymentapp.data.dao.PaymentDao
import com.oqba26.monthlypaymentapp.data.dao.PersonDao
import com.oqba26.monthlypaymentapp.data.database.AppDatabase
import com.oqba26.monthlypaymentapp.data.repository.LocalPersonRepository
import com.oqba26.monthlypaymentapp.data.repository.NetworkRepository
import com.oqba26.monthlypaymentapp.data.repository.SettingsRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideNetworkRepository(): NetworkRepository {
        return NetworkRepository()
    }

    @Provides
    @Singleton
    fun provideLocalPersonRepository(
        personDao: PersonDao,
        paymentDao: PaymentDao,
        database: AppDatabase
    ): LocalPersonRepository {
        return LocalPersonRepository(personDao, paymentDao, database)
    }

    @Provides
    @Singleton
    fun provideSettingsRepository(@ApplicationContext context: Context): SettingsRepository {
        return SettingsRepository(context)
    }
}
