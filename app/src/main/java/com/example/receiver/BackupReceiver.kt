package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.backup.BackupManager
import com.example.data.local.SecurePrefsManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class BackupReceiver : BroadcastReceiver() {

    @Inject lateinit var backupManager: BackupManager
    @Inject lateinit var securePrefsManager: SecurePrefsManager

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_RUN_AUTO_BACKUP) return
        if (!securePrefsManager.isAutoBackupEnabled()) return
        val password = securePrefsManager.getAutoBackupPassword() ?: return
        if (password.length < 6) return

        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val file = backupManager.createBackup(password)
                backupManager.uploadToConfiguredProvider(file)
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val ACTION_RUN_AUTO_BACKUP = "com.example.callerx.ACTION_RUN_AUTO_BACKUP"
    }
}
