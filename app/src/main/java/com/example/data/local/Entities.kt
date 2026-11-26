package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.domain.model.CallRouteType
import com.example.domain.model.CallType
import com.example.domain.model.SipTransport

@Entity(tableName = "sip_accounts")
data class SipAccountEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val username: String,
    val password: String,
    val domain: String,
    val port: Int = 5060,
    val transport: SipTransport = SipTransport.TLS,
    val enabled: Boolean = true,
    val displayName: String = ""
)

@Entity(tableName = "call_logs")
data class CallLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val number: String,
    val contactName: String? = null,
    val type: CallType,
    val route: CallRouteType,
    val simSubscriptionId: Int? = null,
    val sipAccountId: Long? = null,
    val routeLabel: String = route.name,
    val startTime: Long,
    val duration: Long = 0L,
    val recordingPath: String? = null,
    val isBlocked: Boolean = false
)

@Entity(tableName = "contacts")
data class ContactEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val number: String,
    val tags: String = "",
    val note: String = "",
    val totalTalkTimeSeconds: Long = 0L,
    val photoUri: String? = null
)

@Entity(tableName = "blocked_numbers")
data class BlockedNumberEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val number: String,
    val reason: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "sms_messages")
data class SmsEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val address: String,
    val body: String,
    val timestamp: Long,
    val isRead: Boolean = false,
    val threadId: Long,
    val isOutgoing: Boolean = false
)

@Entity(tableName = "recordings")
data class RecordingEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val callLogId: Long,
    val filePath: String,
    val durationMs: Long,
    val sizeBytes: Long,
    val timestamp: Long,
    val displayTitle: String = ""
)

@Entity(tableName = "speed_dials")
data class SpeedDialEntity(
    @PrimaryKey val key: Int,
    val number: String
)

@Entity(tableName = "settings")
data class SettingsEntity(
    @PrimaryKey val key: String,
    val value: String
)
