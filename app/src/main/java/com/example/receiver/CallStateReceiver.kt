package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager
import com.example.domain.model.CallLog
import com.example.domain.model.CallRouteType
import com.example.domain.model.CallType
import com.example.domain.repository.CallLogRepository
import com.example.domain.repository.ContactRepository
import com.example.domain.usecase.EvaluateIncomingCallUseCase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class CallStateReceiver : BroadcastReceiver() {

    @Inject lateinit var callLogRepository: CallLogRepository
    @Inject lateinit var contactRepository: ContactRepository
    @Inject lateinit var evaluateIncomingCallUseCase: EvaluateIncomingCallUseCase

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != TelephonyManager.ACTION_PHONE_STATE_CHANGED) return
        val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE) ?: return
        @Suppress("DEPRECATION")
        val incomingNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)

        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                when (state) {
                    TelephonyManager.EXTRA_STATE_RINGING -> {
                        val wasRingingBefore = lastState == TelephonyManager.EXTRA_STATE_RINGING
                        if (!wasRingingBefore) {
                            callStartMs = System.currentTimeMillis()
                            lastNumber = incomingNumber
                            val blocked = evaluateIncomingCallUseCase.shouldBlockIncoming(
                                rawNumber = incomingNumber,
                                route = CallRouteType.SIM1
                            )
                            wasBlocked = blocked
                        }
                    }

                    TelephonyManager.EXTRA_STATE_OFFHOOK -> {
                        callAnswered = true
                        if (callStartMs == 0L) callStartMs = System.currentTimeMillis()
                    }

                    TelephonyManager.EXTRA_STATE_IDLE -> {
                        if (lastState == TelephonyManager.EXTRA_STATE_RINGING && !callAnswered && !wasBlocked) {
                            val num = lastNumber.orEmpty().ifBlank { "Unknown" }
                            val contact = if (num != "Unknown") contactRepository.findByNumber(num) else null
                            callLogRepository.insertLog(
                                CallLog(
                                    number = num,
                                    contactName = contact?.name,
                                    type = CallType.MISSED,
                                    route = CallRouteType.SIM1,
                                    routeLabel = "SIM 1",
                                    startTime = callStartMs.takeIf { it > 0 } ?: System.currentTimeMillis(),
                                    duration = 0L
                                )
                            )
                        } else if (lastState == TelephonyManager.EXTRA_STATE_OFFHOOK && !wasBlocked) {
                            val num = lastNumber.orEmpty()
                            if (num.isNotBlank()) {
                                val durationSec = ((System.currentTimeMillis() - callStartMs) / 1000L).coerceAtLeast(0L)
                                val contact = contactRepository.findByNumber(num)
                                callLogRepository.insertLog(
                                    CallLog(
                                        number = num,
                                        contactName = contact?.name,
                                        type = CallType.INCOMING,
                                        route = CallRouteType.SIM1,
                                        routeLabel = "SIM 1",
                                        startTime = callStartMs,
                                        duration = durationSec
                                    )
                                )
                                contactRepository.addTalkTime(num, durationSec)
                            }
                        }
                        callStartMs = 0L
                        callAnswered = false
                        wasBlocked = false
                        lastNumber = null
                    }
                }
                lastState = state
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        private var lastState: String = TelephonyManager.EXTRA_STATE_IDLE
        private var lastNumber: String? = null
        private var callStartMs: Long = 0L
        private var callAnswered: Boolean = false
        private var wasBlocked: Boolean = false
    }
}
