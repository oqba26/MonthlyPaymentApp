package com.oqba26.monthlypaymentapp.data.remote

import com.oqba26.monthlypaymentapp.data.model.AuthRequest
import com.oqba26.monthlypaymentapp.data.model.AuthResponse
import com.oqba26.monthlypaymentapp.data.model.PaymentRecord
import com.oqba26.monthlypaymentapp.data.model.Person
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {

    // ⭐️ NEW: Auth Routes
    @POST("auth/register")
    suspend fun register(@Body request: AuthRequest): AuthResponse

    @POST("auth/login")
    suspend fun login(@Body request: AuthRequest): AuthResponse

    // Persons (AuthInterceptor توکن را به هدر اضافه می‌کند)
    @GET("persons")
    suspend fun getPersons(): List<Person>

    @POST("persons")
    suspend fun addPerson(@Body person: Person): Response<Unit>

    // ⭐️ NEW: مسیر PUT برای ویرایش نام
    @PUT("persons/{id}")
    suspend fun updatePerson(@Path("id") personId: String, @Body person: Person): Response<Unit>

    @DELETE("persons/{id}")
    suspend fun deletePersonAndPayments(@Path("id") personId: String): Response<Unit>

    // Payments
    @GET("payments")
    suspend fun getPayments(
        @Query("personId") personId: String? = null
    ): List<PaymentRecord>

    @POST("payments")
    suspend fun addPayment(@Body paymentRecord: PaymentRecord): Response<Unit>

    @DELETE("payments/{id}")
    suspend fun deletePayment(@Path("id") paymentId: String): Response<Unit>
}