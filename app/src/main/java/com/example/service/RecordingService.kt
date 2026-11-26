package com.example.service

import android.app.Notification
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.MediaRecorder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.CallerXApp
import com.example.R
import com.example.domain.model.Recording
import com.example.domain.repository.CallLogRepository
import com.example.domain.repository.RecordingRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@AndroidEntryPoint
class RecordingService : Service() {

    @Inject lateinit var recordingRepository: RecordingRepository
    @Inject lateinit var callLogRepository: CallLogRepository

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var recorder: MediaRecorder? = null
    private var activeFile: File? = null
    private var activeCallLogId: Long = 0L
    private var activeTitle: String = ""
    private var startTimeMs: Long = 0L

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val callLogId = intent.getLongExtra(EXTRA_CALL_LOG_ID, 0L)
                val title = intent.getStringExtra(EXTRA_TITLE) ?: "Call Recording"
                startForegroundCompat()
                startRecordingInternal(callLogId, title)
            }

            ACTION_STOP -> {
                stopRecordingInternal()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    private fun startForegroundCompat() {
        val notification: Notification = NotificationCompat.Builder(this, CallerXApp.CHANNEL_RECORDING)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("CallerX Recording Active")
            .setContentText("Recording call audio locally in encrypted app storage")
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun startRecordingInternal(callLogId: Long, title: String) {
        if (recorder != null) return
        val dir = File(filesDir, "recordings").apply { mkdirs() }
        val ts = System.currentTimeMillis()
        val outFile = File(dir, "${callLogId}_$ts.m4a")

        activeCallLogId = callLogId
        activeTitle = title
        activeFile = outFile
        startTimeMs = ts

        val startedWithVoiceCall = tryStartRecorder(outFile, MediaRecorder.AudioSource.VOICE_CALL)
        if (!startedWithVoiceCall) {
            tryStartRecorder(outFile, MediaRecorder.AudioSource.VOICE_COMMUNICATION)
        }
    }

    private fun tryStartRecorder(outFile: File, audioSource: Int): Boolean {
        return try {
            val mr = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(this)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }
            mr.setAudioSource(audioSource)
            mr.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            mr.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            mr.setAudioEncodingBitRate(96_000)
            mr.setAudioSamplingRate(44_100)
            mr.setOutputFile(outFile.absolutePath)
            mr.prepare()
            mr.start()
            recorder = mr
            true
        } catch (t: Throwable) {
            Log.w(TAG, "AudioSource $audioSource unavailable, falling back", t)
            runCatching { recorder?.release() }
            recorder = null
            false
        }
    }

    private fun stopRecordingInternal() {
        val mr = recorder ?: return
        recorder = null
        runCatching {
            mr.stop()
        }
        runCatching {
            mr.release()
        }

        val file = activeFile ?: return
        val durationMs = (System.currentTimeMillis() - startTimeMs).coerceAtLeast(0L)
        val sizeBytes = if (file.exists()) file.length() else 0L
        val logId = activeCallLogId
        val title = activeTitle

        scope.launch {
            if (file.exists() && sizeBytes > 0L) {
                recordingRepository.insertRecording(
                    Recording(
                        callLogId = logId,
                        filePath = file.absolutePath,
                        durationMs = durationMs,
                        sizeBytes = sizeBytes,
                        timestamp = startTimeMs,
                        displayTitle = title
                    )
                )
                if (logId > 0L) {
                    callLogRepository.attachRecording(logId, file.absolutePath)
                }
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        stopRecordingInternal()
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val TAG = "CallerXRecordingService"
        private const val NOTIFICATION_ID = 1002
        const val ACTION_START = "com.example.callerx.RECORDING_START"
        const val ACTION_STOP = "com.example.callerx.RECORDING_STOP"
        const val EXTRA_CALL_LOG_ID = "extra_call_log_id"
        const val EXTRA_TITLE = "extra_title"

        fun startRecording(context: Context, callLogId: Long, title: String) {
            val intent = Intent(context, RecordingService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_CALL_LOG_ID, callLogId)
                putExtra(EXTRA_TITLE, title)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopRecording(context: Context) {
            val intent = Intent(context, RecordingService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }
}
