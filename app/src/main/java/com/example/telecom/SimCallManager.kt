package com.example.telecom

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager
import android.telephony.SubscriptionManager
import androidx.core.content.ContextCompat
import com.example.domain.model.SimInfo
import com.example.domain.repository.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SimCallManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository
) {
    @SuppressLint("MissingPermission")
    fun getActiveSims(): List<SimInfo> {
        val hasPhoneState = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_PHONE_STATE
        ) == PackageManager.PERMISSION_GRANTED
        if (!hasPhoneState) return emptyList()

        val sm = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as? SubscriptionManager
            ?: return emptyList()

        return try {
            val active = sm.activeSubscriptionInfoList.orEmpty()
            active.map { info ->
                SimInfo(
                    subscriptionId = info.subscriptionId,
                    slotIndex = info.simSlotIndex,
                    carrierName = info.carrierName?.toString().orEmpty().ifBlank { "SIM ${info.simSlotIndex + 1}" },
                    phoneNumber = runCatching {
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                            sm.getPhoneNumber(info.subscriptionId)
                        } else {
                            @Suppress("DEPRECATION")
                            info.number.orEmpty()
                        }
                    }.getOrDefault("")
                )
            }.sortedBy { it.slotIndex }
        } catch (_: SecurityException) {
            emptyList()
        }
    }

    @SuppressLint("MissingPermission")
    fun placeSimCall(number: String, subscriptionId: Int): Boolean {
        val hasCallPhone = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CALL_PHONE
        ) == PackageManager.PERMISSION_GRANTED
        if (!hasCallPhone) return false

        val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
            ?: return false

        val uri = Uri.fromParts("tel", number, null)
        val matchedHandle = findPhoneAccountHandleForSubId(telecomManager, subscriptionId)

        return try {
            val extras = Bundle()
            if (matchedHandle != null) {
                extras.putParcelable(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, matchedHandle)
            }
            telecomManager.placeCall(uri, extras)
            true
        } catch (_: Exception) {
            try {
                val intent = Intent(Intent.ACTION_CALL, uri).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
                true
            } catch (_: Exception) {
                false
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun findPhoneAccountHandleForSubId(
        telecomManager: TelecomManager,
        subscriptionId: Int
    ): PhoneAccountHandle? {
        val hasPhoneState = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_PHONE_STATE
        ) == PackageManager.PERMISSION_GRANTED
        if (!hasPhoneState) return null

        val handles = runCatching { telecomManager.callCapablePhoneAccounts }.getOrNull().orEmpty()
        return handles.firstOrNull { it.id == subscriptionId.toString() }
            ?: handles.getOrNull(
                getActiveSims().indexOfFirst { it.subscriptionId == subscriptionId }.coerceAtLeast(0)
            )
    }

    suspend fun getDefaultRoute(): String? = settingsRepository.getSetting(KEY_DEFAULT_ROUTE)

    suspend fun setDefaultRoute(routeToken: String) {
        settingsRepository.setSetting(KEY_DEFAULT_ROUTE, routeToken)
    }

    suspend fun clearDefaultRoute() {
        settingsRepository.setSetting(KEY_DEFAULT_ROUTE, "")
    }

    @SuppressLint("MissingPermission")
    fun getVoicemailNumber(fallback: String = "*86"): String {
        val hasPhoneState = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_PHONE_STATE
        ) == PackageManager.PERMISSION_GRANTED
        if (!hasPhoneState) return fallback

        val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as? android.telephony.TelephonyManager
        val carrierVm = runCatching { tm?.voiceMailNumber }.getOrNull()
        return carrierVm?.takeIf { it.isNotBlank() } ?: fallback
    }

    companion object {
        const val KEY_DEFAULT_ROUTE = "default_call_route"
    }
}
