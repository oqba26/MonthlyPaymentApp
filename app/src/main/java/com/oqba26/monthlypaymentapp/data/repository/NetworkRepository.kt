package com.oqba26.monthlypaymentapp.data.repository

import android.util.Log
import com.oqba26.monthlypaymentapp.data.model.AuthRequest
import com.oqba26.monthlypaymentapp.data.model.AuthResponse
import com.oqba26.monthlypaymentapp.data.model.DeviceRecord
import com.oqba26.monthlypaymentapp.data.model.PaymentRecord
import com.oqba26.monthlypaymentapp.data.model.Person
import com.oqba26.monthlypaymentapp.data.remote.ApiClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Collections
import java.util.UUID
import com.oqba26.monthlypaymentapp.data.remote.PocketBaseRealtimeEvent
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlin.time.Duration.Companion.milliseconds

class NetworkRepository(
    settingsRepository: SettingsRepository? = null,
) {

    private val pocketBase = ApiClient.pocketBase
    private val _personsFlow = MutableStateFlow<List<Person>>(emptyList())
    private val _paymentsFlow = MutableStateFlow<List<PaymentRecord>>(emptyList())
    private val _devicesFlow = MutableStateFlow<List<DeviceRecord>>(emptyList())
    fun getDevicesFlow(): Flow<List<DeviceRecord>> = _devicesFlow.asStateFlow()

    private val _realtimeEvents = MutableSharedFlow<PocketBaseRealtimeEvent>(extraBufferCapacity = 64)
    val realtimeEvents: SharedFlow<PocketBaseRealtimeEvent> = _realtimeEvents.asSharedFlow()

    @Volatile
    private var isObservingRealtime = false

    private val deletedPaymentIds = Collections.synchronizedSet(mutableSetOf<String>())
    private val deletedPersonIds = Collections.synchronizedSet(mutableSetOf<String>())
    private val repositoryScope = CoroutineScope(Dispatchers.IO)

    /**
     * تا وقتی حداقل یک [refresh] موفق انجام نشده باشد `false` است.
     */
    private val _hasServerData = MutableStateFlow(value = false)
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
        if (isObservingRealtime) return
        isObservingRealtime = true
        repositoryScope.launch {
            while (true) {
                try {
                    pocketBase.listenRealtimeEvents(listOf("persons", "payments", "devices")).collect { event ->
                        Log.d("PocketBase-Realtime", "Change detected in PocketBase: ${event.event} / ${event.action}")
                        _realtimeEvents.emit(event)
                        refresh()
                    }
                } catch (e: Exception) {
                    Log.e("PocketBase-Realtime", "Realtime connection error, retrying...", e)
                }
                delay(3000.milliseconds)
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
        deletedPersonIds.add(personId)
        _personsFlow.update { list -> list.filterNot { it.id == personId } }
        _paymentsFlow.update { list -> list.filterNot { it.personId == personId } }

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
        deletedPaymentIds.add(paymentId)
        _paymentsFlow.update { list -> list.filterNot { it.id == paymentId } }

        val maxRetries = 2
        for (retries in 1..maxRetries) {
            try {
                val ok = pocketBase.deleteRecordByFilter("payments", "id='$paymentId'")
                if (ok) {
                    _paymentsFlow.update { list -> list.filterNot { it.id == paymentId } }
                    Log.d("PocketBase", "deletePayment Success: $paymentId")
                    return true
                } else {
                    Log.w("PocketBase", "deletePayment failed on attempt $retries: $paymentId")
                }
            } catch (e: Exception) {
                Log.e("PocketBase", "deletePayment error (Attempt $retries): ${e.message}", e)
                if (retries < maxRetries) {
                    delay((1000L * retries).milliseconds)
                }
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
            val rawPersons = pocketBase.getRecords<Person>("persons")
            val rawPayments = pocketBase.getRecords<PaymentRecord>("payments", sort = "-timestamp")

            // حذف رکوردهایی که سرور دیگر آن‌ها را برنمی‌گرداند از لیست حذفیات موقت
            deletedPersonIds.removeAll { id -> rawPersons.none { it.id == id } }
            deletedPaymentIds.removeAll { id -> rawPayments.none { it.id == id } }

            val persons = rawPersons.filterNot { it.id in deletedPersonIds }
            val payments = rawPayments.filterNot { it.id in deletedPaymentIds || it.personId in deletedPersonIds }

            _personsFlow.value = persons
            _paymentsFlow.value = payments

            val rawDevices = try {
                pocketBase.getRecords<DeviceRecord>("devices", sort = "-lastActive")
            } catch (e: Exception) {
                Log.w("PocketBase", "Failed to fetch devices: ${e.message}")
                emptyList()
            }
            _devicesFlow.value = rawDevices

            _hasServerData.value = true

            Log.d("PocketBase", "Refresh Success: ${persons.size} persons, ${payments.size} payments, ${rawDevices.size} devices")
            true
        } catch (e: Exception) {
            Log.e("PocketBase", "refresh error", e)
            false
        }
    }

    suspend fun registerOrUpdateCurrentDevice(deviceId: String, deviceName: String, isMaster: Boolean = false): DeviceRecord? {
        return try {
            val existing = pocketBase.getRecords<DeviceRecord>("devices", filter = "deviceId='$deviceId'")
            if (existing.isNotEmpty()) {
                val current = existing.first()
                val updated = current.copy(
                    deviceName = deviceName,
                    lastActive = System.currentTimeMillis(),
                    isAdmin = isMaster || current.isAdmin,
                    allowBackup = isMaster || current.allowBackup,
                    allowLogout = isMaster || current.allowLogout,
                )
                pocketBase.updateRecord("devices", current.id, updated)
                refresh()
                updated
            } else {
                val newRecord = DeviceRecord(
                    deviceId = deviceId,
                    deviceName = deviceName,
                    isAdmin = isMaster,
                    allowBackup = isMaster,
                    allowLogout = isMaster,
                    lastActive = System.currentTimeMillis()
                )
                val created = pocketBase.createRecord("devices", newRecord)
                refresh()
                created
            }
        } catch (e: Exception) {
            Log.e("PocketBase", "registerOrUpdateCurrentDevice error", e)
            null
        }
    }

    suspend fun updateDevicePermissions(recordId: String, isAdmin: Boolean, allowBackup: Boolean, allowLogout: Boolean): Boolean {
        return try {
            val existing = _devicesFlow.value.find { it.id == recordId } ?: return false
            val updated = existing.copy(
                isAdmin = isAdmin,
                allowBackup = allowBackup,
                allowLogout = allowLogout
            )
            pocketBase.updateRecord("devices", recordId, updated)
            refresh()
            true
        } catch (e: Exception) {
            Log.e("PocketBase", "updateDevicePermissions error", e)
            false
        }
    }

    suspend fun deleteDeviceRecord(recordId: String): Boolean {
        return try {
            pocketBase.deleteRecordByFilter("devices", "id='$recordId'")
            refresh()
            true
        } catch (e: Exception) {
            Log.e("PocketBase", "deleteDeviceRecord error", e)
            false
        }
    }
}
