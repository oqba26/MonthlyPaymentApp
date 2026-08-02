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
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.util.UUID

class NetworkRepository {

    private val supabase = ApiClient.client
    private val _personsFlow = MutableStateFlow<List<Person>>(emptyList())
    private val _paymentsFlow = MutableStateFlow<List<PaymentRecord>>(emptyList())
    private val repositoryScope = CoroutineScope(Dispatchers.IO)

    /**
     * تا وقتی حداقل یک [refresh] موفق انجام نشده باشد `false` است.
     *
     * چرا لازم است: مقدار اولیه‌ی `_personsFlow`/`_paymentsFlow` لیست خالی است و از لیست خالیِ
     * واقعی (کاربری که هنوز داده‌ای ثبت نکرده) قابل تشخیص نیست. merge کردن روی آن لیست خالیِ
     * قالبی، همه‌ی داده‌ی محلی را پاک می‌کند. مصرف‌کننده باید منتظر `true` بماند.
     */
    private val _hasServerData = MutableStateFlow(false)
    fun hasServerDataFlow(): Flow<Boolean> = _hasServerData.asStateFlow()

    // --- Realtime Operations ---
    fun observeRealtimeChanges() {
        repositoryScope.launch {
            try {
                // اطمینان از اتصال قبل از اشتراک در کانال
                supabase.realtime.connect()
                
                val myChannel = supabase.channel("db-changes")
                
                // گوش دادن به تغییرات جدول اشخاص
                myChannel.postgresChangeFlow<PostgresAction>(schema = "public") {
                    table = "persons"
                }.collect {
                    Log.d("Supabase-Realtime", "Change detected in persons table")
                    refresh()
                }

                // گوش دادن به تغییرات جدول پرداخت‌ها
                launch {
                    myChannel.postgresChangeFlow<PostgresAction>(schema = "public") {
                        table = "payments"
                    }.collect {
                        Log.d("Supabase-Realtime", "Change detected in payments table")
                        refresh()
                    }
                }
                
                myChannel.subscribe()
                Log.d("Supabase-Realtime", "Subscribed to all changes")
            } catch (e: Exception) {
                Log.e("Supabase-Realtime", "Realtime connection error, retrying...", e)
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
    /**
     * ارسال شخص به سرور با upsert.
     *
     * upsert (و نه insert) عمداً انتخاب شده تا این عملیات idempotent باشد: اگر تلاش اول
     * به سرور رسیده باشد ولی پاسخش به‌خاطر timeout به ما نرسیده باشد، تلاش دوم رکورد
     * تکراری نمی‌سازد. صف سینک متکی به همین خاصیت است تا بتواند بی‌خطر retry کند.
     *
     * اعتبارسنجی تکراری‌بودن نام اینجا انجام نمی‌شود؛ آن کار در PersonViewModel روی
     * داده‌ی محلی (که همیشه کامل است) انجام می‌گیرد. کش _personsFlow ممکن است خالی یا
     * قدیمی باشد و نتیجه‌ی غلط بدهد.
     */
    suspend fun addPerson(person: Person): Boolean {
        return try {
            val trimmedName = person.name.trim()
            if (trimmedName.isEmpty()) {
                Log.w("Supabase", "addPerson skipped: blank name")
                return false
            }

            val personToUpsert = if (person.id.isEmpty()) {
                person.copy(id = UUID.randomUUID().toString(), name = trimmedName, createdAt = System.currentTimeMillis())
            } else {
                person.copy(name = trimmedName)
            }

            supabase.from("persons").upsert(personToUpsert)
            true
        } catch (e: Exception) {
            Log.e("Supabase", "addPerson error", e)
            false
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

            supabase.from("persons").update(
                buildJsonObject {
                    put("name", trimmedName)
                    put("phoneNumber", phoneNumber?.trim())
                    put("monthlyCommitment", monthlyCommitment)
                    put("startMonth", startMonth)
                    put("startYear", startYear)
                },
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
                buildJsonObject {
                    put("isArchived", isArchived)
                }
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
                buildJsonObject {
                    put("displayOrder", displayOrder)
                }
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
    /**
     * ارسال پرداخت به سرور با upsert — به همان دلیل idempotent بودن که در [addPerson] توضیح داده شد.
     * ضمناً همین باعث می‌شود ویرایش پرداخت (SyncOperation.UPDATE) هم درست کار کند؛ قبلاً insert
     * بود و روی رکورد موجود شکست می‌خورد.
     */
    suspend fun addPayment(paymentRecord: PaymentRecord): Boolean {
        return try {
            supabase.from("payments").upsert(paymentRecord)
            true
        } catch (e: Exception) {
            Log.e("Supabase", "addPayment error", e); false
        }
    }

    suspend fun deletePayment(paymentId: String): Boolean {
        var retries = 0
        val maxRetries = 2
        
        while (retries <= maxRetries) {
            try {
                supabase.from("payments").delete {
                    filter { eq("id", paymentId) }
                }
                Log.d("Supabase", "deletePayment Success: $paymentId")
                return true
            } catch (e: Exception) {
                retries++
                Log.e("Supabase", "deletePayment error (Attempt $retries): ${e.message}", e)
                if (retries > maxRetries) return false
                kotlinx.coroutines.delay(1000L * retries) // Exponential backoff-ish
            }
        }
        return false
    }

    fun getPaymentsFlow(): Flow<List<PaymentRecord>> = _paymentsFlow.asStateFlow()

    /**
     * واکشی کامل داده‌ها از سرور.
     *
     * @return true فقط اگر **هر دو** واکشی موفق بوده باشند.
     *
     * دو تضمین مهم که فراخوان‌ها به آن تکیه می‌کنند:
     *  ۱. در صورت شکست، flow‌ها **دست‌نخورده** می‌مانند. قبلاً خطا بلعیده می‌شد و صدازننده
     *     نمی‌فهمید واکشی شکست خورده؛ نتیجه‌اش merge روی داده‌ی ناقص و پاک شدن داده‌ی محلی بود.
     *  ۲. مقداردهی flow‌ها اتمیک است — یا هر دو با هم به‌روز می‌شوند یا هیچ‌کدام. قبلاً اگر
     *     واکشی پرداخت‌ها شکست می‌خورد، لیست اشخاص جدید کنار لیست پرداخت‌های قدیمی می‌نشست.
     */
    suspend fun refresh(): Boolean {
        return try {
            val persons = supabase.from("persons").select().decodeList<Person>()
            val payments = supabase.from("payments").select {
                order("timestamp", order = Order.DESCENDING)
            }.decodeList<PaymentRecord>()

            // فقط بعد از موفقیت هر دو واکشی مقداردهی می‌کنیم
            _personsFlow.value = persons
            _paymentsFlow.value = payments
            _hasServerData.value = true

            Log.d("Supabase", "Refresh Success: ${persons.size} persons, ${payments.size} payments")
            true
        } catch (e: Exception) {
            Log.e("Supabase", "refresh error — داده‌های محلی دست‌نخورده باقی می‌مانند", e)
            false
        }
    }
}
