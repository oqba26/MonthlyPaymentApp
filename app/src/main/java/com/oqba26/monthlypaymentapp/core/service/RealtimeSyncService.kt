package com.oqba26.monthlypaymentapp.core.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.oqba26.monthlypaymentapp.core.notification.PaymentNotificationManager
import com.oqba26.monthlypaymentapp.data.model.PaymentRecord
import com.oqba26.monthlypaymentapp.data.model.Person
import com.oqba26.monthlypaymentapp.data.remote.ApiClient
import com.oqba26.monthlypaymentapp.data.repository.LocalPersonRepository
import com.oqba26.monthlypaymentapp.data.repository.NetworkRepository
import com.oqba26.monthlypaymentapp.data.repository.SettingsRepository
import com.oqba26.monthlypaymentapp.utils.getDeviceHardwareId
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Collections
import javax.inject.Inject

@AndroidEntryPoint
class RealtimeSyncService : Service() {

    @Inject
    lateinit var networkRepository: NetworkRepository

    @Inject
    lateinit var localPersonRepository: LocalPersonRepository

    @Inject
    lateinit var settingsRepository: SettingsRepository

    @Inject
    lateinit var notificationManager: PaymentNotificationManager

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val notifiedPaymentIds = Collections.synchronizedSet(mutableSetOf<String>())

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "RealtimeSyncService ایجاد شد")

        val notification = notificationManager.getForegroundServiceNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceCompat.startForeground(
                this,
                PaymentNotificationManager.FOREGROUND_SERVICE_NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
            )
        } else {
            startForeground(PaymentNotificationManager.FOREGROUND_SERVICE_NOTIFICATION_ID, notification)
        }

        listenToRealtimeEvents()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "RealtimeSyncService onStartCommand")
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "RealtimeSyncService متوقف شد")
        serviceScope.cancel()
    }

    private fun listenToRealtimeEvents() {
        serviceScope.launch {
            // ۱. اطمینان از برقراری ریل‌تایم در NetworkRepository
            networkRepository.observeRealtimeChanges()

            // ۲. شنود رویدادها
            networkRepository.realtimeEvents.collect { (event, action, recordJson) ->
                try {
                    if ((event == "payments") && (action == "create")) {
                        handlePaymentCreated(recordJson)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "خطا در پردازش رویداد ریل‌تایم", e)
                }
            }
        }
    }

    private suspend fun handlePaymentCreated(recordJson: String) {
        if (recordJson.isBlank()) return
        val payment = try {
            ApiClient.pocketBase.json.decodeFromString<PaymentRecord>(recordJson)
        } catch (e: Exception) {
            Log.e(TAG, "خطا در دیکود پرداخت دریافتی: $recordJson", e)
            return
        }

        // اگر قبلاً اعلان این پرداخت ارسال شده است، تکرار نشود
        if (notifiedPaymentIds.contains(payment.id)) return

        // بررسی اینکه آیا پرداخت در همین دستگاه ثبت شده بود
        val existingLocally = localPersonRepository.getPaymentById(payment.id)
        if (existingLocally != null) {
            // پرداخت متعلق به همین دستگاه بوده و قبلاً در دیتابیس محلی ذخیره شده است
            notifiedPaymentIds.add(payment.id)
            return
        }

        // بررسی دسترسی: فقط در صورت فعال بودن اعلان و مدیر/ارشد بودن دستگاه، نوتیفیکیشن نمایش داده شود
        val notificationsEnabled = settingsRepository.realtimeNotificationsEnabledFlow.first()
        val isMaster = settingsRepository.isMasterDeviceFlow.first()
        val deviceId = getDeviceHardwareId(applicationContext)
        val devices = networkRepository.getDevicesFlow().first()
        val isDeviceAdmin = devices.any { (it.deviceId == deviceId) && it.isAdmin }

        if (notificationsEnabled && (isMaster || isDeviceAdmin)) {
            notifiedPaymentIds.add(payment.id)

            // دریافت نام مشتری
            var personName: String? = localPersonRepository.getPersonById(payment.personId)?.name
            if (personName.isNullOrBlank()) {
                val remotePersons = ApiClient.pocketBase.getRecords<Person>(
                    "persons",
                    filter = "id='${payment.personId}'",
                )
                personName = remotePersons.firstOrNull()?.name
            }

            notificationManager.showPaymentNotification(
                payment = payment,
                personName = personName ?: "مشتری",
            )
        }

        // ذخیره/ادغام در دیتابیس محلی
        localPersonRepository.mergeFromServer(emptyList(), listOf(payment))
    }

    companion object {
        private const val TAG = "RealtimeSyncService"

        fun start(context: Context) {
            val intent = Intent(context, RealtimeSyncService::class.java)
            try {
                ContextCompat.startForegroundService(context, intent)
            } catch (e: Exception) {
                Log.e(TAG, "خطا در اجرای RealtimeSyncService", e)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, RealtimeSyncService::class.java)
            try {
                context.stopService(intent)
            } catch (e: Exception) {
                Log.e(TAG, "خطا در توقف RealtimeSyncService", e)
            }
        }
    }
}
