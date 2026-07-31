package com.oqba26.monthlypaymentapp.data.repository

import android.util.Log
import com.oqba26.monthlypaymentapp.data.model.AuthRequest
import com.oqba26.monthlypaymentapp.data.model.AuthResponse
import com.oqba26.monthlypaymentapp.data.model.PaymentRecord
import com.oqba26.monthlypaymentapp.data.model.Person
import com.oqba26.monthlypaymentapp.data.remote.ApiClient
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.builtin.Email
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class NetworkRepository {

    private val supabase = ApiClient.client
    private val _personsFlow = MutableStateFlow<List<Person>>(emptyList())
    private val _paymentsFlow = MutableStateFlow<List<PaymentRecord>>(emptyList())
    private val repositoryScope = CoroutineScope(Dispatchers.IO)

    // --- Realtime Operations ---
    fun observeRealtimeChanges() {
        repositoryScope.launch {
            try {
                supabase.realtime.connect()
                val myChannel = supabase.channel("db-changes")
                
                // گوش دادن به تغییرات جدول اشخاص
                launch {
                    myChannel.postgresChangeFlow<PostgresAction>(schema = "public") {
                        table = "persons"
                    }.collect {
                        refresh()
                    }
                }

                // گوش دادن به تغییرات جدول پرداخت‌ها
                launch {
                    myChannel.postgresChangeFlow<PostgresAction>(schema = "public") {
                        table = "payments"
                    }.collect {
                        refresh()
                    }
                }
                
                myChannel.subscribe()
            } catch (e: Exception) {
                Log.e("Supabase", "Realtime connection error", e)
            }
        }
    }

    // --- Auth Operations ---
    suspend fun register(request: AuthRequest): AuthResponse? {
        return try {
            supabase.auth.signUpWith(Email) {
                email = request.email
                password = request.password
            }
            val session = supabase.auth.currentSessionOrNull()
            AuthResponse(token = session?.accessToken ?: "success", userId = session?.user?.id)
        } catch (e: Exception) {
            Log.e("Supabase", "register error", e)
            null
        }
    }

    suspend fun login(request: AuthRequest): AuthResponse? {
        return try {
            supabase.auth.signInWith(Email) {
                email = request.email
                password = request.password
            }
            val session = supabase.auth.currentSessionOrNull()
            AuthResponse(token = session?.accessToken ?: "", userId = session?.user?.id)
        } catch (e: Exception) {
            Log.e("Supabase", "login error", e)
            null
        }
    }

    // --- Person Operations ---
    suspend fun addPerson(person: Person): Int? {
        return try {
            val trimmedName = person.name.trim()
            if (trimmedName.isEmpty()) return 400

            // بررسی تکراری بودن نام در لیست موجود
            val isDuplicate = _personsFlow.value.any { it.name.trim().equals(trimmedName, ignoreCase = true) }
            if (isDuplicate) return 409
            
            val personToInsert = if (person.id.isEmpty()) {
                person.copy(id = UUID.randomUUID().toString(), name = trimmedName, createdAt = System.currentTimeMillis())
            } else {
                person.copy(name = trimmedName)
            }
            
            supabase.from("persons").insert(personToInsert)
            201 
        } catch (e: Exception) {
            Log.e("Supabase", "addPerson error", e)
            null
        }
    }

    suspend fun deletePersonAndPayments(personId: String): Boolean {
        return try {
            supabase.from("persons").delete {
                filter { eq("id", personId) }
            }
            true
        } catch (e: Exception) {
            Log.e("Supabase", "deletePerson error", e); false
        }
    }

    suspend fun updatePerson(personId: String, name: String, phoneNumber: String?): Int {
        return try {
            val trimmedName = name.trim()
            if (trimmedName.isEmpty()) return 400

            // بررسی تکراری بودن نام (به جز خودِ این شخص)
            val isDuplicate = _personsFlow.value.any { 
                it.id != personId && it.name.trim().equals(trimmedName, ignoreCase = true) 
            }
            if (isDuplicate) return 409

            supabase.from("persons").update(
                mapOf(
                    "name" to trimmedName,
                    "phoneNumber" to phoneNumber?.trim()
                )
            ) {
                filter { eq("id", personId) }
            }
            200
        } catch (e: Exception) {
            Log.e("Supabase", "updatePerson error", e)
            500
        }
    }

    suspend fun updatePersonArchivedStatus(personId: String, isArchived: Boolean): Boolean {
        return try {
            supabase.from("persons").update(
                mapOf("isArchived" to isArchived)
            ) {
                filter { eq("id", personId) }
            }
            true
        } catch (e: Exception) {
            Log.e("Supabase", "updateArchived error", e); false
        }
    }

    suspend fun updatePersonDisplayOrder(personId: String, displayOrder: Long): Boolean {
        return try {
            supabase.from("persons").update(
                mapOf("displayOrder" to displayOrder)
            ) {
                filter { eq("id", personId) }
            }
            true
        } catch (e: Exception) {
            Log.e("Supabase", "updateOrder error", e); false
        }
    }

    fun getPersonsFlow(): Flow<List<Person>> = _personsFlow.asStateFlow()

    // --- Payment Operations ---
    suspend fun addPayment(paymentRecord: PaymentRecord): Boolean {
        return try {
            supabase.from("payments").insert(paymentRecord)
            true
        } catch (e: Exception) {
            Log.e("Supabase", "addPayment error", e); false
        }
    }

    suspend fun deletePayment(paymentId: String): Boolean {
        return try {
            supabase.from("payments").delete {
                filter { eq("id", paymentId) }
            }
            true
        } catch (e: Exception) {
            Log.e("Supabase", "deletePayment error", e); false
        }
    }

    fun getPaymentsFlow(): Flow<List<PaymentRecord>> = _paymentsFlow.asStateFlow()

    suspend fun refresh() {
        try {
            // دریافت و تبدیل خودکار لیست اشخاص
            val persons = supabase.from("persons").select().decodeList<Person>()
            _personsFlow.value = persons
            
            // دریافت و تبدیل خودکار لیست پرداخت‌ها
            val payments = supabase.from("payments").select {
                order("timestamp", order = Order.DESCENDING)
            }.decodeList<PaymentRecord>()
            _paymentsFlow.value = payments
            
            Log.d("Supabase", "Refresh Success: ${persons.size} persons, ${payments.size} payments")
        } catch (e: Exception) {
            Log.e("Supabase", "refresh error", e)
        }
    }
}
