package com.example.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class SipTransport {
    UDP,
    TCP,
    TLS,
    WSS
}

enum class AccountStatus {
    ONLINE,
    OFFLINE,
    REGISTERING,
    ERROR
}

@Serializable
enum class CallType {
    INCOMING,
    OUTGOING,
    MISSED
}

@Serializable
enum class CallRouteType {
    SIM1,
    SIM2,
    SIP
}

enum class CallState {
    IDLE,
    DIALING,
    RINGING,
    ACTIVE,
    HELD,
    ENDED
}

enum class HistoryFilter {
    ALL,
    MISSED,
    SIP,
    SIM
}

enum class AppThemeColor(val label: String, val previewHex: Long) {
    BLUE("Cobalt Blue", 0xFF1565C0),
    PURPLE("Electric Violet", 0xFF6A1B9A),
    GREEN("Signal Emerald", 0xFF00897B),
    ORANGE("Solar Amber", 0xFFEF6C00),
    RED("Crimson Signal", 0xFFC62828),
    TEAL("Cyber Teal", 0xFF00838F),
    PINK("Neon Rose", 0xFFAD1457),
    INDIGO("Deep Indigo", 0xFF283593),
    AMBER("Warm Gold", 0xFFF9A825)
}

enum class DarkModeOption(val label: String) {
    SYSTEM("System"),
    LIGHT("Light"),
    DARK("Dark")
}

enum class BackupProvider(val label: String) {
    LOCAL("Local (.callerx)"),
    GOOGLE_DRIVE("Google Drive"),
    DROPBOX("Dropbox")
}

@Serializable
data class SipAccount(
    val id: Long = 0,
    val username: String,
    val password: String,
    val domain: String,
    val port: Int = 5060,
    val transport: SipTransport = SipTransport.TLS,
    val enabled: Boolean = true,
    val displayName: String = ""
)

@Serializable
data class CallLog(
    val id: Long = 0,
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

@Serializable
data class Contact(
    val id: Long = 0,
    val name: String,
    val number: String,
    val tags: List<String> = emptyList(),
    val note: String = "",
    val totalTalkTimeSeconds: Long = 0L,
    val photoUri: String? = null
)

@Serializable
data class BlockedNumber(
    val id: Long = 0,
    val number: String,
    val reason: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@Serializable
data class SmsMessage(
    val id: Long = 0,
    val address: String,
    val body: String,
    val timestamp: Long,
    val isRead: Boolean = false,
    val threadId: Long,
    val isOutgoing: Boolean = false
)

data class SmsThread(
    val threadId: Long,
    val address: String,
    val contactName: String? = null,
    val lastMessage: String,
    val timestamp: Long,
    val unreadCount: Int
)

@Serializable
data class Recording(
    val id: Long = 0,
    val callLogId: Long,
    val filePath: String,
    val durationMs: Long,
    val sizeBytes: Long,
    val timestamp: Long,
    val displayTitle: String = ""
)

@Serializable
data class SpeedDial(
    val key: Int,
    val number: String
)

data class SimInfo(
    val subscriptionId: Int,
    val slotIndex: Int,
    val carrierName: String,
    val phoneNumber: String = ""
) {
    val routeType: CallRouteType
        get() = if (slotIndex == 0) CallRouteType.SIM1 else CallRouteType.SIM2
}

sealed class CallRoute {
    data class Sim(
        val subscriptionId: Int,
        val slotIndex: Int,
        val carrierName: String
    ) : CallRoute() {
        val routeType: CallRouteType
            get() = if (slotIndex == 0) CallRouteType.SIM1 else CallRouteType.SIM2
        val label: String
            get() = "SIM ${slotIndex + 1} · $carrierName"
    }

    data class Sip(
        val accountId: Long,
        val displayName: String,
        val domain: String
    ) : CallRoute() {
        val label: String
            get() = displayName.ifBlank { domain }
    }
}

data class CallStats(
    val jitterMs: Float = 0f,
    val packetLossPercent: Float = 0f,
    val qualityBars: Int = 5
)

data class ActiveCallInfo(
    val callId: String = "",
    val remoteNumber: String = "",
    val remoteName: String? = null,
    val state: CallState = CallState.IDLE,
    val routeLabel: String = "",
    val isSip: Boolean = false,
    val sipAccountId: Long? = null,
    val simSubscriptionId: Int? = null,
    val startTimeMs: Long = System.currentTimeMillis(),
    val callLogId: Long? = null
)
