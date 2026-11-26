package com.example.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

object PermissionUtils {

    fun requiredRuntimePermissions(): List<String> {
        val list = mutableListOf(
            Manifest.permission.CALL_PHONE,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.READ_PHONE_NUMBERS,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.READ_SMS,
            Manifest.permission.SEND_SMS,
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.WRITE_CALL_LOG
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            list.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            list.add(Manifest.permission.BLUETOOTH_CONNECT)
        }
        return list
    }

    fun hasPermission(context: Context, permission: String): Boolean {
        return ContextCompat.checkSelfPermission(context, permission) ==
            PackageManager.PERMISSION_GRANTED
    }

    fun missingPermissions(context: Context): List<String> {
        return requiredRuntimePermissions().filterNot { hasPermission(context, it) }
    }

    fun permissionLabel(permission: String): String {
        return when (permission) {
            Manifest.permission.CALL_PHONE -> "Make & manage SIM calls"
            Manifest.permission.READ_PHONE_STATE -> "Detect dual-SIM slots & call state"
            Manifest.permission.READ_PHONE_NUMBERS -> "Display SIM line numbers"
            Manifest.permission.RECORD_AUDIO -> "VoIP microphone & call recording"
            Manifest.permission.READ_CONTACTS -> "Match caller IDs & T9 search"
            Manifest.permission.READ_SMS -> "Display SMS inbox threads"
            Manifest.permission.SEND_SMS -> "Send SMS replies"
            Manifest.permission.READ_CALL_LOG -> "Sync device call log"
            Manifest.permission.WRITE_CALL_LOG -> "Save call history entries"
            Manifest.permission.POST_NOTIFICATIONS -> "Incoming call & SIP status notifications"
            Manifest.permission.BLUETOOTH_CONNECT -> "Route call audio to Bluetooth headsets"
            else -> permission.substringAfterLast('.')
        }
    }
}
