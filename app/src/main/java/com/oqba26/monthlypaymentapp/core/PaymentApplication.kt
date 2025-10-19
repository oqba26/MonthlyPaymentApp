package com.oqba26.monthlypaymentapp.core

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.oqba26.monthlypaymentapp.data.database.AppDatabase
import com.oqba26.monthlypaymentapp.data.remote.ApiClient
import com.oqba26.monthlypaymentapp.data.repository.LocalPersonRepository
import com.oqba26.monthlypaymentapp.data.repository.NetworkRepository
import com.oqba26.monthlypaymentapp.data.repository.SettingsRepository
import com.oqba26.monthlypaymentapp.viewmodel.PersonViewModel
import com.oqba26.monthlypaymentapp.viewmodel.SettingsViewModel

class PaymentApplication : Application() {

    lateinit var localPersonRepository: LocalPersonRepository
    lateinit var networkRepository: NetworkRepository
    lateinit var settingsRepository: SettingsRepository
    private lateinit var apiClient: ApiClient

    override fun onCreate() {
        super.onCreate()
        val db = AppDatabase.getInstance(this)

        settingsRepository = SettingsRepository(this)
        apiClient = ApiClient(settingsRepository)

        localPersonRepository = LocalPersonRepository(
            personDao = db.personDao(),
            paymentDao = db.paymentDao(),
            database = db
        )

        networkRepository = NetworkRepository(api = apiClient.api)
    }

    val personViewModelFactory: ViewModelProvider.Factory
        get() = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(PersonViewModel::class.java)) {
                    // ⭐️ FIX: Removed the unnecessary 'localPersonRepository' argument.
                    return PersonViewModel(networkRepository, settingsRepository) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        }

    val settingsViewModelFactory: ViewModelProvider.Factory
        get() = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
                    return SettingsViewModel(settingsRepository, localPersonRepository) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        }
}
