package com.example.util

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat

enum class NetworkTypeLabel(val displayName: String) {
    WIFI("Wi-Fi"),
    FIVE_G("5G"),
    FOUR_G("4G"),
    THREE_G("3G"),
    TWO_G("2G"),
    CELLULAR("Cellular"),
    OFFLINE("Offline")
}

object NetworkUtils {

    @SuppressLint("MissingPermission")
    fun detectNetworkType(context: Context): NetworkTypeLabel {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return NetworkTypeLabel.OFFLINE
        val active = cm.activeNetwork ?: return NetworkTypeLabel.OFFLINE
        val caps = cm.getNetworkCapabilities(active) ?: return NetworkTypeLabel.OFFLINE

        if (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
        ) {
            return NetworkTypeLabel.WIFI
        }

        if (caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
            val hasPhonePerm = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_PHONE_STATE
            ) == PackageManager.PERMISSION_GRANTED

            if (!hasPhonePerm) return NetworkTypeLabel.CELLULAR

            val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
                ?: return NetworkTypeLabel.CELLULAR

            val dataType = try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    tm.dataNetworkType
                } else {
                    @Suppress("DEPRECATION")
                    tm.networkType
                }
            } catch (_: SecurityException) {
                return NetworkTypeLabel.CELLULAR
            }

            return when (dataType) {
                TelephonyManager.NETWORK_TYPE_NR -> NetworkTypeLabel.FIVE_G
                TelephonyManager.NETWORK_TYPE_LTE -> NetworkTypeLabel.FOUR_G
                TelephonyManager.NETWORK_TYPE_UMTS,
                TelephonyManager.NETWORK_TYPE_EVDO_0,
                TelephonyManager.NETWORK_TYPE_EVDO_A,
                TelephonyManager.NETWORK_TYPE_HSDPA,
                TelephonyManager.NETWORK_TYPE_HSUPA,
                TelephonyManager.NETWORK_TYPE_HSPA,
                TelephonyManager.NETWORK_TYPE_EVDO_B,
                TelephonyManager.NETWORK_TYPE_EHRPD,
                TelephonyManager.NETWORK_TYPE_HSPAP,
                TelephonyManager.NETWORK_TYPE_TD_SCDMA -> NetworkTypeLabel.THREE_G
                TelephonyManager.NETWORK_TYPE_GPRS,
                TelephonyManager.NETWORK_TYPE_EDGE,
                TelephonyManager.NETWORK_TYPE_CDMA,
                TelephonyManager.NETWORK_TYPE_1xRTT,
                TelephonyManager.NETWORK_TYPE_IDEN,
                TelephonyManager.NETWORK_TYPE_GSM -> NetworkTypeLabel.TWO_G
                else -> NetworkTypeLabel.CELLULAR
            }
        }

        return NetworkTypeLabel.OFFLINE
    }

    fun isNetworkAvailable(context: Context): Boolean {
        return detectNetworkType(context) != NetworkTypeLabel.OFFLINE
    }
}
