package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.backup.BackupScheduler
import com.example.data.local.SecurePrefsManager
import com.example.domain.repository.SettingsRepository
import com.example.service.SipService
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject lateinit var settingsRepository: SettingsRepository
    @Inject lateinit var securePrefsManager: SecurePrefsManager
    @Inject lateinit var backupScheduler: BackupScheduler

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val enabled = settingsRepository.getSetting(KEY_BACKGROUND_SIP)
                    ?.toBooleanStrictOrNull() ?: true
                if (enabled) {
                    SipService.start(context.applicationContext)
                }
                if (securePrefsManager.isAutoBackupEnabled()) {
                    backupScheduler.schedule(securePrefsManager.getBackupFrequencyHours())
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val KEY_BACKGROUND_SIP = "background_sip_enabled"
    }
}
