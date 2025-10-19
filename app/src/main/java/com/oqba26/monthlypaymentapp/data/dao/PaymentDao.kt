package com.oqba26.monthlypaymentapp.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.oqba26.monthlypaymentapp.data.model.PaymentRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface PaymentDao {

    @Query("SELECT * FROM payments ORDER BY timestamp DESC")
    fun getAllPaymentsFlow(): Flow<List<PaymentRecord>>

    @Query("SELECT * FROM payments WHERE personId = :personId ORDER BY timestamp DESC")
    fun getPaymentsForPersonFlow(personId: String): Flow<List<PaymentRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllPaymentRecords(payments: List<PaymentRecord>)

    @Query("DELETE FROM payments")
    suspend fun deleteAllPaymentRecords()
}