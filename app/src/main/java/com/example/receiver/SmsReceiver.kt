package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.example.domain.repository.SmsRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class SmsReceiver : BroadcastReceiver() {

    @Inject lateinit var smsRepository: SmsRepository

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return
        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (messages.isNullOrEmpty()) return

        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                messages.groupBy { it.originatingAddress.orEmpty() }.forEach { (sender, parts) ->
                    val body = parts.joinToString(separator = "") { it.messageBody.orEmpty() }
                    val ts = parts.maxOfOrNull { it.timestampMillis } ?: System.currentTimeMillis()
                    if (sender.isNotBlank() && body.isNotBlank()) {
                        smsRepository.saveIncomingSms(sender, body, ts)
                    }
                }
            } finally {
                pending.finish()
            }
        }
    }
}
