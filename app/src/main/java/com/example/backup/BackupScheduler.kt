package com.example.backup

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.example.receiver.BackupReceiver
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackupScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun schedule(intervalHours: Int) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intervalMs = TimeUnit.HOURS.toMillis(intervalHours.coerceAtLeast(1).toLong())
        val triggerAt = System.currentTimeMillis() + intervalMs
        val pi = buildPendingIntent()
        am.cancel(pi)
        am.setInexactRepeating(
            AlarmManager.RTC_WAKEUP,
            triggerAt,
            intervalMs,
            pi
        )
    }

    fun cancel() {
        val am = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        am.cancel(buildPendingIntent())
    }

    private fun buildPendingIntent(): PendingIntent {
        val intent = Intent(context, BackupReceiver::class.java).apply {
            action = BackupReceiver.ACTION_RUN_AUTO_BACKUP
        }
        return PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    companion object {
        private const val REQUEST_CODE = 4401
    }
}
