package com.example.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.CallerXApp
import com.example.MainActivity
import com.example.R
import com.example.domain.model.AccountStatus
import com.example.sip.SipManager
import com.example.ui.call.IncomingCallActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class SipService : Service() {

    @Inject lateinit var sipManager: SipManager

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var statusJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        sipManager.init(this)
        startForegroundCompat("SIP Connecting...")

        statusJob = scope.launch {
            sipManager.allStatuses.collectLatest { map ->
                val onlineCount = map.values.count { it == AccountStatus.ONLINE }
                val text = when {
                    onlineCount > 0 -> "SIP Online · $onlineCount account(s) registered"
                    map.values.any { it == AccountStatus.REGISTERING } -> "Registering SIP accounts..."
                    map.isEmpty() -> "No SIP accounts configured"
                    else -> "SIP Offline"
                }
                val nm = getSystemService(android.app.NotificationManager::class.java)
                nm?.notify(NOTIFICATION_ID, buildStatusNotification(text))
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_ANSWER_CALL -> sipManager.answerCall()
            ACTION_DECLINE_CALL -> sipManager.declineCall()
            ACTION_STOP -> {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
        }
        return START_STICKY
    }

    private fun startForegroundCompat(statusText: String) {
        val notification = buildStatusNotification(statusText)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_PHONE_CALL
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun buildStatusNotification(statusText: String): Notification {
        val openIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val incomingIntent = PendingIntent.getActivity(
            this,
            1,
            Intent(this, IncomingCallActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CallerXApp.CHANNEL_SIP)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("CallerX SIP")
            .setContentText(statusText)
            .setOngoing(true)
            .setContentIntent(openIntent)
            .addAction(0, "Call UI", incomingIntent)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        statusJob?.cancel()
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val NOTIFICATION_ID = 1001
        const val ACTION_ANSWER_CALL = "com.example.callerx.ACTION_ANSWER_SIP"
        const val ACTION_DECLINE_CALL = "com.example.callerx.ACTION_DECLINE_SIP"
        const val ACTION_STOP = "com.example.callerx.ACTION_STOP_SIP"

        fun start(context: Context) {
            val intent = Intent(context, SipService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }
}
