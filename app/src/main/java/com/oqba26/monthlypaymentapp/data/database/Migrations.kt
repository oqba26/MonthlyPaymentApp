package com.oqba26.monthlypaymentapp.data.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * مهاجرت‌های اسکیمای دیتابیس.
 *
 * تا نسخه ۱۷، اپ از `fallbackToDestructiveMigration()` استفاده می‌کرد؛ یعنی هر تغییر اسکیما
 * کل داده‌ی کاربر را پاک می‌کرد. از نسخه ۱۸ به بعد مهاجرت اجباری است: اگر اسکیما تغییر کند
 * و مهاجرتش نوشته نشده باشد، اپ در زمان بیلد/تست خطا می‌دهد — نه اینکه بی‌صدا داده را پاک کند.
 *
 * هنگام اضافه کردن نسخه‌ی جدید:
 *  ۱. `version` را در [AppDatabase] یکی زیاد کن.
 *  ۲. یک `MIGRATION_<قبلی>_<جدید>` اینجا بنویس و به `addMigrations` در `DatabaseModule` اضافه کن.
 *  ۳. فایل JSON تولیدشده در پوشه‌ی `app/schemas` را کامیت کن (مبنای تست مهاجرت است).
 */

/**
 * ۱۷ → ۱۸: دو ستون برای retry با backoff به صف سینک اضافه می‌شود.
 *
 * جدول‌های `persons` و `payments` دست‌نخورده‌اند، پس هیچ داده‌ی کاربری در این مهاجرت
 * جابه‌جا یا بازنویسی نمی‌شود.
 */
val MIGRATION_17_18 = object : Migration(17, 18) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE sync_queue ADD COLUMN attemptCount INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE sync_queue ADD COLUMN lastAttemptAt INTEGER NOT NULL DEFAULT 0")
    }
}

/** همه‌ی مهاجرت‌های شناخته‌شده — به `Room.databaseBuilder(...).addMigrations(*ALL_MIGRATIONS)` داده می‌شود. */
val ALL_MIGRATIONS = arrayOf(MIGRATION_17_18)
