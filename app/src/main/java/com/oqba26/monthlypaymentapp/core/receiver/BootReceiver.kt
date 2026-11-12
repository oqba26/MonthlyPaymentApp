package com.oqba26.monthlypaymentapp.core.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.oqba26.monthlypaymentapp.core.service.RealtimeSyncService
import com.oqba26.monthlypaymentapp.data.repository.SettingsRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject
    lateinit var settingsRepository: SettingsRepository

    override fun onReceive(context: Context, intent: Intent) {
        if ((intent.action == Intent.ACTION_BOOT_COMPLETED) ||
            (intent.action == Intent.ACTION_MY_PACKAGE_REPLACED)
        ) {
            CoroutineScope(Dispatchers.IO).launch {
                val token = settingsRepository.authTokenFlow.first()
                val isEnabled = settingsRepository.realtimeNotificationsEnabledFlow.first()
                if (token != null && isEnabled) {
                    RealtimeSyncService.start(context)
                }
            }
        }
    }
}
