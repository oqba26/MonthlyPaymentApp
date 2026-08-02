package com.oqba26.monthlypaymentapp.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

enum class SyncOperation {
    INSERT, UPDATE, DELETE
}

enum class SyncType {
    PERSON, PAYMENT
}

@Entity(tableName = "sync_queue")
data class SyncQueue(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val entityId: String,
    val type: SyncType,
    val operation: SyncOperation,
    val createdAt: Long = System.currentTimeMillis(),

    /** تعداد تلاش‌های ناموفق. مبنای محاسبه‌ی فاصله‌ی تلاش بعدی (backoff) است. */
    val attemptCount: Int = 0,

    /** زمان آخرین تلاش (میلی‌ثانیه). صفر یعنی هنوز تلاشی نشده. */
    val lastAttemptAt: Long = 0L
)
