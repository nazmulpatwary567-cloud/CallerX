package com.example.telecom

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.telecom.Connection
import android.telecom.ConnectionRequest
import android.telecom.ConnectionService
import android.telecom.DisconnectCause
import android.telecom.PhoneAccount
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager

class CallerXConnectionService : ConnectionService() {

    override fun onCreateOutgoingConnection(
        connectionManagerPhoneAccount: PhoneAccountHandle?,
        request: ConnectionRequest?
    ): Connection {
        val connection = CallerXSipConnection()
        request?.address?.let { uri ->
            connection.setAddress(uri, TelecomManager.PRESENTATION_ALLOWED)
        }
        connection.setDialing()
        return connection
    }

    override fun onCreateIncomingConnection(
        connectionManagerPhoneAccount: PhoneAccountHandle?,
        request: ConnectionRequest?
    ): Connection {
        val connection = CallerXSipConnection()
        request?.address?.let { uri ->
            connection.setAddress(uri, TelecomManager.PRESENTATION_ALLOWED)
        }
        connection.setRinging()
        return connection
    }

    private class CallerXSipConnection : Connection() {
        init {
            connectionProperties = PROPERTY_SELF_MANAGED
            audioModeIsVoip = true
            connectionCapabilities = CAPABILITY_HOLD or CAPABILITY_SUPPORT_HOLD or CAPABILITY_MUTE
        }

        override fun onAnswer() {
            setActive()
        }

        override fun onReject() {
            setDisconnected(DisconnectCause(DisconnectCause.REJECTED))
            destroy()
        }

        override fun onDisconnect() {
            setDisconnected(DisconnectCause(DisconnectCause.LOCAL))
            destroy()
        }

        override fun onHold() {
            setOnHold()
        }

        override fun onUnhold() {
            setActive()
        }
    }

    companion object {
        private const val SIP_ACCOUNT_ID = "callerx_sip_phone_account"

        fun getPhoneAccountHandle(context: Context): PhoneAccountHandle {
            return PhoneAccountHandle(
                ComponentName(context, CallerXConnectionService::class.java),
                SIP_ACCOUNT_ID
            )
        }

        fun registerPhoneAccount(context: Context) {
            val tm = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager ?: return
            val handle = getPhoneAccountHandle(context)
            val account = PhoneAccount.builder(handle, "CallerX SIP")
                .setCapabilities(
                    PhoneAccount.CAPABILITY_SELF_MANAGED or
                        PhoneAccount.CAPABILITY_SUPPORTS_VIDEO_CALLING
                )
                .addSupportedUriScheme(PhoneAccount.SCHEME_SIP)
                .addSupportedUriScheme(PhoneAccount.SCHEME_TEL)
                .setShortDescription("CallerX VoIP / SIP")
                .build()
            runCatching { tm.registerPhoneAccount(account) }
        }
    }
}
