package com.oqba26.monthlypaymentapp.data.repository

import android.util.Log
import com.oqba26.monthlypaymentapp.data.model.AuthRequest
import com.oqba26.monthlypaymentapp.data.model.AuthResponse
import com.oqba26.monthlypaymentapp.data.model.PaymentRecord
import com.oqba26.monthlypaymentapp.data.model.Person
import com.oqba26.monthlypaymentapp.data.remote.ApiService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import retrofit2.HttpException

class NetworkRepository(private val api: ApiService) {

    private val _personsFlow = MutableStateFlow<List<Person>>(emptyList())
    private val _paymentsFlow = MutableStateFlow<List<PaymentRecord>>(emptyList())

    // --- Auth Operations ---
    suspend fun register(request: AuthRequest): AuthResponse? {
        return try {
            api.register(request)
        } catch (e: HttpException) {
            Log.e("API", "register HttpException: ${e.code()}", e)
            null
        } catch (e: Exception) {
            Log.e("API", "register error", e)
            null
        }
    }

    suspend fun login(request: AuthRequest): AuthResponse? {
        return try {
            api.login(request)
        } catch (e: HttpException) {
            Log.e("API", "login HttpException: ${e.code()}", e)
            null
        } catch (e: Exception) {
            Log.e("API", "login error", e)
            null
        }
    }

    // --- Person Operations ---
    suspend fun addPerson(person: Person): Int? {
        return try {
            val trimmed = person.name.trim()
            if (trimmed.isEmpty()) return 400 // Bad request from client side
            val resp = api.addPerson(person.copy(name = trimmed))
            if (resp.isSuccessful) refresh()
            resp.code() // Return the actual HTTP status code
        } catch (e: HttpException) {
            e.code() // Return the HTTP error code (e.g., 409)
        } catch (e: Exception) {
            Log.e("API", "addPerson error", e)
            null // Return null for non-HTTP errors
        }
    }

    suspend fun deletePersonAndPayments(personId: String): Boolean {
        return try {
            val resp = api.deletePersonAndPayments(personId)
            if (resp.isSuccessful) refresh()
            resp.isSuccessful
        } catch (e: Exception) {
            Log.e("API", "deletePersonAndPayments error", e); false
        }
    }

    suspend fun updatePerson(personId: String, name: String): Boolean {
        return try {
            val trimmed = name.trim()
            if (trimmed.isEmpty()) return false
            val personUpdate = Person(id = personId, name = trimmed)
            val resp = api.updatePerson(personId, personUpdate)
            if (resp.isSuccessful) refresh()
            resp.isSuccessful
        } catch (e: Exception) {
            Log.e("API", "updatePerson error", e); false
        }
    }

    fun getPersonsFlow(): Flow<List<Person>> = _personsFlow.asStateFlow()

    // --- Payment Operations ---
    suspend fun addPayment(paymentRecord: PaymentRecord): Boolean {
        return try {
            val resp = api.addPayment(paymentRecord)
            if (resp.isSuccessful) refresh()
            resp.isSuccessful
        } catch (e: Exception) {
            Log.e("API", "addPayment error", e); false
        }
    }

    suspend fun deletePayment(paymentId: String): Boolean {
        return try {
            val resp = api.deletePayment(paymentId)
            if (resp.isSuccessful) refresh()
            resp.isSuccessful
        } catch (e: Exception) {
            Log.e("API", "deletePayment error", e); false
        }
    }

    fun getPaymentsFlow(): Flow<List<PaymentRecord>> = _paymentsFlow.asStateFlow()

    suspend fun refresh() {
        try {
            val persons = api.getPersons()
            _personsFlow.value = persons
        } catch (e: Exception) {
            Log.e("API", "refresh persons error", e)
        }
        try {
            val payments = api.getPayments(null)
            _paymentsFlow.value = payments.sortedByDescending { it.timestamp }
        } catch (e: Exception) {
            Log.e("API", "refresh payments error", e)
        }
    }
}
