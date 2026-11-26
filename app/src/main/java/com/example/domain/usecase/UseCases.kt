package com.example.domain.usecase

import com.example.domain.model.CallLog
import com.example.domain.model.CallRoute
import com.example.domain.model.CallRouteType
import com.example.domain.model.CallType
import com.example.domain.model.Contact
import com.example.domain.repository.BlockedNumberRepository
import com.example.domain.repository.CallLogRepository
import com.example.domain.repository.ContactRepository
import com.example.domain.repository.SettingsRepository
import com.example.sip.SipManager
import com.example.telecom.SimCallManager
import com.example.util.PhoneUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class SearchContactsT9UseCase @Inject constructor(
    private val contactRepository: ContactRepository
) {
    operator fun invoke(query: String): Flow<List<Contact>> {
        return contactRepository.observeContacts().map { contacts ->
            if (query.isBlank()) contacts
            else contacts.filter { PhoneUtils.matchesT9(it, query) }
        }
    }
}

class PlaceCallUseCase @Inject constructor(
    private val sipManager: SipManager,
    private val simCallManager: SimCallManager,
    private val callLogRepository: CallLogRepository,
    private val contactRepository: ContactRepository
) {
    suspend operator fun invoke(number: String, route: CallRoute): Result<Long> {
        val cleanNumber = PhoneUtils.normalizeNumber(number)
        if (cleanNumber.isBlank()) return Result.failure(IllegalArgumentException("Enter a valid number"))

        val contact = contactRepository.findByNumber(cleanNumber)
        val now = System.currentTimeMillis()

        return when (route) {
            is CallRoute.Sim -> {
                val placed = simCallManager.placeSimCall(cleanNumber, route.subscriptionId)
                if (!placed) {
                    return Result.failure(IllegalStateException("CALL_PHONE permission or SIM slot unavailable"))
                }
                val logId = callLogRepository.insertLog(
                    CallLog(
                        number = cleanNumber,
                        contactName = contact?.name,
                        type = CallType.OUTGOING,
                        route = route.routeType,
                        simSubscriptionId = route.subscriptionId,
                        routeLabel = route.label,
                        startTime = now
                    )
                )
                Result.success(logId)
            }

            is CallRoute.Sip -> {
                val logId = callLogRepository.insertLog(
                    CallLog(
                        number = cleanNumber,
                        contactName = contact?.name,
                        type = CallType.OUTGOING,
                        route = CallRouteType.SIP,
                        sipAccountId = route.accountId,
                        routeLabel = route.label,
                        startTime = now
                    )
                )
                sipManager.makeCall(
                    number = cleanNumber,
                    accountId = route.accountId,
                    contactName = contact?.name,
                    callLogId = logId
                )
                Result.success(logId)
            }
        }
    }
}

class EvaluateIncomingCallUseCase @Inject constructor(
    private val blockedNumberRepository: BlockedNumberRepository,
    private val settingsRepository: SettingsRepository,
    private val callLogRepository: CallLogRepository,
    private val contactRepository: ContactRepository
) {
    suspend fun shouldBlockIncoming(rawNumber: String?, route: CallRouteType, sipAccountId: Long? = null): Boolean {
        val number = rawNumber?.trim().orEmpty()
        val blockUnknown = settingsRepository.getSetting(KEY_BLOCK_UNKNOWN)?.toBooleanStrictOrNull() ?: false

        val blocked = when {
            PhoneUtils.isUnknownOrHiddenNumber(number) && blockUnknown -> true
            number.isNotBlank() && blockedNumberRepository.isBlocked(number) -> true
            else -> false
        }

        if (blocked) {
            val contact = if (number.isNotBlank()) contactRepository.findByNumber(number) else null
            callLogRepository.insertLog(
                CallLog(
                    number = number.ifBlank { "Unknown" },
                    contactName = contact?.name,
                    type = CallType.MISSED,
                    route = route,
                    sipAccountId = sipAccountId,
                    startTime = System.currentTimeMillis(),
                    duration = 0L,
                    isBlocked = true
                )
            )
        }
        return blocked
    }

    companion object {
        const val KEY_BLOCK_UNKNOWN = "block_unknown_numbers"
    }
}
