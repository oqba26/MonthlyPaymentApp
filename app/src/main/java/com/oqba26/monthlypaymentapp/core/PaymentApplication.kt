package com.oqba26.monthlypaymentapp.core

import android.app.Application
import android.util.Log
import com.oqba26.monthlypaymentapp.BuildConfig
import com.oqba26.monthlypaymentapp.core.manager.BackupManager
import com.oqba26.monthlypaymentapp.data.repository.SettingsRepository
import com.oqba26.monthlypaymentapp.data.sync.SyncManager
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class PaymentApplication : Application() {
    @Inject
    lateinit var syncManager: SyncManager

    @Inject
    lateinit var backupManager: BackupManager

    @Inject
    lateinit var settingsRepository: SettingsRepository

    private val appScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onCreate() {
        super.onCreate()
        syncManager.startSync()
        snapshotOnUpgrade()
    }

    /**
     * اولین اجرا بعد از هر ارتقای نسخه، یک نسخه‌ی پشتیبان می‌گیرد.
     *
     * ارتقای نسخه همان لحظه‌ای است که مهاجرت دیتابیس اجرا می‌شود؛ اگر آنجا چیزی خراب شد،
     * این فایل تنها راه برگشت داده‌ی کاربر است.
     */
    private fun snapshotOnUpgrade() {
        appScope.launch {
            try {
                val current = BuildConfig.VERSION_CODE
                val last = settingsRepository.getLastRunVersionCode()
                if (last != current) {
                    if (last != null) {
                        // اجرای اول بعد از نصب، داده‌ای برای پشتیبان‌گیری ندارد؛ فقط
                        // ارتقای واقعی (last != null) ارزش snapshot دارد.
                        backupManager.createSnapshot(BackupManager.REASON_APP_UPGRADE)
                    }
                    settingsRepository.saveLastRunVersionCode(current)
                }
            } catch (e: Exception) {
                Log.e("PaymentApplication", "snapshot ارتقای نسخه ناموفق بود", e)
            }
        }
    }
}
