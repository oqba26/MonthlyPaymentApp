package com.oqba26.monthlypaymentapp.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.oqba26.monthlypaymentapp.data.model.SyncQueue
import kotlinx.coroutines.flow.Flow

@Dao
interface SyncQueueDao {
    @Query("SELECT * FROM sync_queue ORDER BY createdAt ASC")
    fun getAllPendingFlow(): Flow<List<SyncQueue>>

    @Query("SELECT * FROM sync_queue ORDER BY createdAt ASC")
    suspend fun getAllPending(): List<SyncQueue>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: SyncQueue)

    @Delete
    suspend fun delete(item: SyncQueue)

    /**
     * ثبت یک تلاش ناموفق. آیتم **حذف نمی‌شود** — فقط عقب می‌افتد تا دور بعد.
     */
    @Query("UPDATE sync_queue SET attemptCount = :count, lastAttemptAt = :at WHERE id = :id")
    suspend fun recordAttempt(id: String, count: Int, at: Long)

    @Query("DELETE FROM sync_queue WHERE entityId = :entityId")
    suspend fun deleteByEntityId(entityId: String)

    @Query("DELETE FROM sync_queue")
    suspend fun deleteAll()
}
