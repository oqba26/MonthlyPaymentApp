package com.oqba26.monthlypaymentapp.core.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.oqba26.monthlypaymentapp.MainActivity
import com.oqba26.monthlypaymentapp.R
import com.oqba26.monthlypaymentapp.data.model.PaymentRecord
import com.oqba26.monthlypaymentapp.utils.formatNumberAsPersian
import com.oqba26.monthlypaymentapp.utils.formatTimestampToPersianDateTime
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PaymentNotificationManager @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    companion object {
        const val CHANNEL_PAYMENT_ALERTS = "payment_alerts_channel"
        const val OLD_FOREGROUND_SERVICE_NOTIFICATION_ID = 9001
    }

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        createNotificationChannels()
        // حذف اعلان و کانال قدیمی سرویس دائم
        try {
            notificationManager.cancel(OLD_FOREGROUND_SERVICE_NOTIFICATION_ID)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                notificationManager.deleteNotificationChannel("realtime_sync_channel")
            }
        } catch (_: Exception) { }
    }

    fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // کانال اعلان‌های پرداخت با اولویت بالا همراه با صدا و ویبره
            val alertSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .build()

            val alertChannel = NotificationChannel(
                CHANNEL_PAYMENT_ALERTS,
                "اعلان‌های پرداخت جدید",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = "اطلاع‌رسانی هنگام ثبت پرداخت‌های جدید توسط اوپراتورها"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 200, 500)
                setSound(alertSound, audioAttributes)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                setShowBadge(true)
            }
            notificationManager.createNotificationChannel(alertChannel)
        }
    }

    @android.annotation.SuppressLint("MissingPermission")
    fun showPaymentNotification(payment: PaymentRecord, personName: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (androidx.core.content.ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.POST_NOTIFICATIONS,
                ) != android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
        }
        val formattedAmount = formatNumberAsPersian(payment.amount.toLong())
        val formattedDateTime = formatTimestampToPersianDateTime(payment.timestamp)

        val mainMessage = "مبلغ $formattedAmount تومان برای $personName در $formattedDateTime ثبت شد."
        val fullMessage = if (!payment.description.isNullOrBlank()) {
            "$mainMessage\n(${payment.description})"
        } else {
            mainMessage
        }

        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("PERSON_ID", payment.personId)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            payment.id.hashCode(),
            launchIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val notification = NotificationCompat.Builder(context, CHANNEL_PAYMENT_ALERTS)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("ثبت پرداخت جدید: $personName")
            .setContentText(mainMessage)
            .setStyle(NotificationCompat.BigTextStyle().bigText(fullMessage))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setSound(soundUri)
            .setVibrate(longArrayOf(0, 500, 200, 500))
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setCategory(NotificationCompat.CATEGORY_EVENT)
            .build()

        notificationManager.notify(payment.id.hashCode(), notification)
    }
}
