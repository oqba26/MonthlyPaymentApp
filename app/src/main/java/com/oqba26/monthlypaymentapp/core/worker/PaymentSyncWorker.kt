package com.oqba26.monthlypaymentapp.core.worker

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.oqba26.monthlypaymentapp.core.notification.PaymentNotificationManager
import com.oqba26.monthlypaymentapp.data.model.PaymentRecord
import com.oqba26.monthlypaymentapp.data.model.Person
import com.oqba26.monthlypaymentapp.data.remote.ApiClient
import com.oqba26.monthlypaymentapp.data.repository.LocalPersonRepository
import com.oqba26.monthlypaymentapp.data.repository.NetworkRepository
import com.oqba26.monthlypaymentapp.data.repository.SettingsRepository
import com.oqba26.monthlypaymentapp.utils.getDeviceHardwareId
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

class PaymentSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface PaymentSyncWorkerEntryPoint {
        fun localPersonRepository(): LocalPersonRepository
        fun settingsRepository(): SettingsRepository
        fun networkRepository(): NetworkRepository
        fun paymentNotificationManager(): PaymentNotificationManager
    }

    override suspend fun doWork(): Result {
        return try {
            val entryPoint = EntryPointAccessors.fromApplication(
                applicationContext,
                PaymentSyncWorkerEntryPoint::class.java,
            )
            val settingsRepo = entryPoint.settingsRepository()
            val localRepo = entryPoint.localPersonRepository()
            val networkRepo = entryPoint.networkRepository()
            val notifManager = entryPoint.paymentNotificationManager()

            val token = settingsRepo.authTokenFlow.first() ?: return Result.success()
            ApiClient.pocketBase.authToken = token

            val notificationsEnabled = settingsRepo.realtimeNotificationsEnabledFlow.first()
            if (!notificationsEnabled) {
                return Result.success()
            }

            val isMaster = settingsRepo.isMasterDeviceFlow.first()
            val deviceId = getDeviceHardwareId(applicationContext)
            val devices = networkRepo.getDevicesFlow().first()
            val isDeviceAdmin = devices.any { (_, devId, _, isAdmin) -> (devId == deviceId) && isAdmin }

            if (!isMaster && !isDeviceAdmin) {
                return Result.success()
            }

            // واکشی آخرین پرداخت‌ها از سرور
            val remotePayments = ApiClient.pocketBase.getRecords<PaymentRecord>(
                "payments",
                sort = "-timestamp",
                perPage = 30,
            )
            if (remotePayments.isEmpty()) {
                return Result.success()
            }

            val now = System.currentTimeMillis()
            val oneDayAgo = now - (24 * 60 * 60 * 1000L)

            val remotePersons = ApiClient.pocketBase.getRecords<Person>("persons")
            val personsMap = remotePersons.associateBy(Person::id)

            for (payment in remotePayments) {
                // فقط پرداخت‌های اخیر (مثلاً ظرف ۲۴ ساعت گذشته)
                if (payment.timestamp < oneDayAgo) continue

                // آیا در دیتابیس محلی این گوشی وجود دارد؟
                val localPayment = localRepo.getPaymentById(payment.id)
                if (localPayment == null) {
                    // پرداخت جدید از طرف اوپراتور دیگر
                    val personName = personsMap[payment.personId]?.name
                        ?: localRepo.getPersonById(payment.personId)?.name
                        ?: "مشتری"

                    notifManager.showPaymentNotification(payment, personName)
                }
            }

            // ادغام در دیتابیس محلی
            localRepo.mergeFromServer(remotePersons, remotePayments)

            Result.success()
        } catch (e: Exception) {
            Log.e("PaymentSyncWorker", "خطا در همگام‌سازی دوره‌ای پرداخت‌ها", e)
            Result.retry()
        }
    }

    companion object {
        private const val WORK_NAME = "PaymentPeriodicSyncWork"

        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = PeriodicWorkRequestBuilder<PaymentSyncWorker>(15, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}
