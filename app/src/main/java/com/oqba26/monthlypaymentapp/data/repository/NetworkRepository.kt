package com.oqba26.monthlypaymentapp.data.repository

import android.util.Log
import com.oqba26.monthlypaymentapp.data.model.AuthRequest
import com.oqba26.monthlypaymentapp.data.model.AuthResponse
import com.oqba26.monthlypaymentapp.data.model.PaymentRecord
import com.oqba26.monthlypaymentapp.data.model.Person
import com.oqba26.monthlypaymentapp.data.remote.ApiClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class NetworkRepository(
    private val settingsRepository: SettingsRepository? = null
) {

    private val pocketBase = ApiClient.pocketBase
    private val _personsFlow = MutableStateFlow<List<Person>>(emptyList())
    private val _paymentsFlow = MutableStateFlow<List<PaymentRecord>>(emptyList())
    private val repositoryScope = CoroutineScope(Dispatchers.IO)

    /**
     * تا وقتی حداقل یک [refresh] موفق انجام نشده باشد `false` است.
     */
    private val _hasServerData = MutableStateFlow(false)
    fun hasServerDataFlow(): Flow<Boolean> = _hasServerData.asStateFlow()

    init {
        settingsRepository?.authTokenFlow?.let { authFlow ->
            repositoryScope.launch {
                authFlow.collect { token ->
                    pocketBase.authToken = token
                }
            }
        }
    }

    // --- Realtime Operations ---
    fun observeRealtimeChanges() {
        repositoryScope.launch {
            try {
                pocketBase.listenRealtime(listOf("persons", "payments")).collect { event ->
                    Log.d("PocketBase-Realtime", "Change detected in PocketBase: $event")
                    refresh()
                }
            } catch (e: Exception) {
                Log.e("PocketBase-Realtime", "Realtime connection error, retrying...", e)
            }
        }
    }

    // --- Auth Operations ---
    suspend fun register(request: AuthRequest): AuthResponse? {
        return try {
            pocketBase.createUser("users", request.email, request.password)
            val authResp = pocketBase.authWithPassword("users", request.email, request.password)
            val userId = authResp.record?.get("id")?.toString()?.replace("\"", "")
            AuthResponse(token = authResp.token, userId = userId)
        } catch (e: Exception) {
            Log.e("PocketBase", "register error", e)
            null
        }
    }

    suspend fun login(request: AuthRequest): AuthResponse? {
        return try {
            val authResp = pocketBase.authWithPassword("users", request.email, request.password)
            val userId = authResp.record?.get("id")?.toString()?.replace("\"", "")
            AuthResponse(token = authResp.token, userId = userId)
        } catch (e: Exception) {
            Log.e("PocketBase", "login error", e)
            null
        }
    }

    // --- Person Operations ---
    suspend fun addPerson(person: Person): Boolean {
        return try {
            val trimmedName = person.name.trim()
            if (trimmedName.isEmpty()) {
                Log.w("PocketBase", "addPerson skipped: blank name")
                return false
            }

            val personToUpsert = if (person.id.isEmpty()) {
                person.copy(id = UUID.randomUUID().toString(), name = trimmedName, createdAt = System.currentTimeMillis())
            } else {
                person.copy(name = trimmedName)
            }

            pocketBase.upsertRecord("persons", "id='${personToUpsert.id}'", personToUpsert)
            true
        } catch (e: Exception) {
            Log.e("PocketBase", "addPerson error", e)
            false
        }
    }

    suspend fun deletePersonAndPayments(personId: String): Boolean {
        return try {
            pocketBase.deleteRecordByFilter("payments", "personId='$personId'")
            pocketBase.deleteRecordByFilter("persons", "id='$personId'")
            true
        } catch (e: Exception) {
            Log.e("PocketBase", "deletePerson error", e)
            false
        }
    }

    suspend fun updatePerson(
        personId: String, 
        name: String, 
        phoneNumber: String?, 
        monthlyCommitment: Double,
        startMonth: Int,
        startYear: Int
    ): Int {
        return try {
            val trimmedName = name.trim()
            if (trimmedName.isEmpty()) return 400

            val currentPerson = _personsFlow.value.find { it.id == personId }
            val currentCategory = currentPerson?.category ?: "salary"

            // بررسی تکراری بودن نام در همان دسته‌بندی (به جز خودِ این شخص)
            val isDuplicate = _personsFlow.value.any { 
                it.id != personId && 
                it.category == currentCategory && 
                it.name.trim().equals(trimmedName, ignoreCase = true) 
            }
            if (isDuplicate) return 409

            val updatedPerson = currentPerson?.copy(
                name = trimmedName,
                phoneNumber = phoneNumber?.trim(),
                monthlyCommitment = monthlyCommitment,
                startMonth = startMonth,
                startYear = startYear
            )
            if (updatedPerson != null) {
                pocketBase.upsertRecord("persons", "id='$personId'", updatedPerson)
            }
            200
        } catch (e: Exception) {
            Log.e("PocketBase", "updatePerson error", e)
            500
        }
    }

    suspend fun updatePersonArchivedStatus(personId: String, isArchived: Boolean): Boolean {
        return try {
            val currentPerson = _personsFlow.value.find { it.id == personId }
            if (currentPerson != null) {
                pocketBase.upsertRecord("persons", "id='$personId'", currentPerson.copy(isArchived = isArchived))
            }
            true
        } catch (e: Exception) {
            Log.e("PocketBase", "updateArchived error", e)
            false
        }
    }

    suspend fun updatePersonDisplayOrder(personId: String, displayOrder: Long): Boolean {
        return try {
            val currentPerson = _personsFlow.value.find { it.id == personId }
            if (currentPerson != null) {
                pocketBase.upsertRecord("persons", "id='$personId'", currentPerson.copy(displayOrder = displayOrder))
            }
            true
        } catch (e: Exception) {
            Log.e("PocketBase", "updateOrder error", e)
            false
        }
    }

    fun getPersonsFlow(): Flow<List<Person>> = _personsFlow.asStateFlow()

    // --- Payment Operations ---
    suspend fun addPayment(paymentRecord: PaymentRecord): Boolean {
        return try {
            pocketBase.upsertRecord("payments", "id='${paymentRecord.id}'", paymentRecord)
            true
        } catch (e: Exception) {
            Log.e("PocketBase", "addPayment error", e)
            false
        }
    }

    suspend fun deletePayment(paymentId: String): Boolean {
        var retries = 0
        val maxRetries = 2
        
        while (retries <= maxRetries) {
            try {
                pocketBase.deleteRecordByFilter("payments", "id='$paymentId'")
                Log.d("PocketBase", "deletePayment Success: $paymentId")
                return true
            } catch (e: Exception) {
                retries++
                Log.e("PocketBase", "deletePayment error (Attempt $retries): ${e.message}", e)
                if (retries > maxRetries) return false
                delay(1000L * retries)
            }
        }
        return false
    }

    fun getPaymentsFlow(): Flow<List<PaymentRecord>> = _paymentsFlow.asStateFlow()

    /**
     * واکشی کامل داده‌ها از سرور.
     */
    suspend fun refresh(): Boolean {
        return try {
            val persons = pocketBase.getRecords<Person>("persons")
            val payments = pocketBase.getRecords<PaymentRecord>("payments", sort = "-timestamp")

            _personsFlow.value = persons
            _paymentsFlow.value = payments
            _hasServerData.value = true

            Log.d("PocketBase", "Refresh Success: ${persons.size} persons, ${payments.size} payments")
            true
        } catch (e: Exception) {
            Log.e("PocketBase", "refresh error", e)
            false
        }
    }
}
