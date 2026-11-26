package com.example

import android.app.Activity
import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.os.Bundle
import com.example.privacy.AppLockManager
import com.example.sip.SipManager
import com.example.telecom.CallerXConnectionService
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class CallerXApp : Application() {

    @Inject lateinit var sipManager: SipManager
    @Inject lateinit var appLockManager: AppLockManager

    private var startedActivityCount = 0
    private var isChangingConfig = false

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
        sipManager.init(this)
        CallerXConnectionService.registerPhoneAccount(this)

        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
            override fun onActivityStarted(activity: Activity) {
                if (startedActivityCount == 0 && !isChangingConfig) {
                    appLockManager.onAppForegrounded()
                }
                startedActivityCount++
                isChangingConfig = false
            }

            override fun onActivityResumed(activity: Activity) {}
            override fun onActivityPaused(activity: Activity) {}
            override fun onActivityStopped(activity: Activity) {
                isChangingConfig = activity.isChangingConfigurations
                startedActivityCount = (startedActivityCount - 1).coerceAtLeast(0)
                if (startedActivityCount == 0 && !isChangingConfig) {
                    appLockManager.onAppBackgrounded()
                }
            }

            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
            override fun onActivityDestroyed(activity: Activity) {}
        })
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NotificationManager::class.java) ?: return

            val sipChannel = NotificationChannel(
                CHANNEL_SIP,
                getString(R.string.sip_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.sip_channel_desc)
                setShowBadge(false)
            }

            val incomingChannel = NotificationChannel(
                CHANNEL_INCOMING_CALL,
                getString(R.string.call_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = getString(R.string.call_channel_desc)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            }

            val recordingChannel = NotificationChannel(
                CHANNEL_RECORDING,
                getString(R.string.recording_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.recording_channel_desc)
            }

            nm.createNotificationChannels(listOf(sipChannel, incomingChannel, recordingChannel))
        }
    }

    companion object {
        const val CHANNEL_SIP = "callerx_sip_status"
        const val CHANNEL_INCOMING_CALL = "callerx_incoming_call"
        const val CHANNEL_RECORDING = "callerx_recording"
    }
}
