package com.oqba26.monthlypaymentapp.core.service

import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder

class RealtimeSyncService : Service() {

    override fun onCreate() {
        super.onCreate()
        stopSelf()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        stopSelf()
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        fun stop(context: Context) {
            try {
                val notifManager = context.getSystemService(NOTIFICATION_SERVICE) as? NotificationManager
                notifManager?.cancel(9001)
                context.stopService(Intent(context, RealtimeSyncService::class.java))
            } catch (_: Exception) { }
        }
    }
}
