package com.example.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.model.CallLog
import com.example.domain.model.CallRoute
import com.example.domain.model.Contact
import com.example.domain.model.HistoryFilter
import com.example.domain.repository.BlockedNumberRepository
import com.example.domain.repository.CallLogRepository
import com.example.domain.repository.ContactRepository
import com.example.domain.repository.SipRepository
import com.example.domain.usecase.PlaceCallUseCase
import com.example.telecom.SimCallManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val callLogRepository: CallLogRepository,
    private val blockedNumberRepository: BlockedNumberRepository,
    private val contactRepository: ContactRepository,
    private val sipRepository: SipRepository,
    private val simCallManager: SimCallManager,
    private val placeCallUseCase: PlaceCallUseCase
) : ViewModel() {

    private val _filter = MutableStateFlow(HistoryFilter.ALL)
    val filter: StateFlow<HistoryFilter> = _filter.asStateFlow()

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _events = MutableSharedFlow<String>()
    val events: SharedFlow<String> = _events.asSharedFlow()

    val logs: StateFlow<List<CallLog>> = combine(_filter, _query) { f, q -> f to q }
        .flatMapLatest { (f, q) -> callLogRepository.observeLogs(f, q) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val missedCount: StateFlow<Int> = callLogRepository.observeMissedCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    init {
        viewModelScope.launch {
            callLogRepository.syncSystemCallLogs()
        }
    }

    fun refreshSystemLogs() {
        viewModelScope.launch {
            callLogRepository.syncSystemCallLogs()
            _events.emit("Call history synced")
        }
    }

    fun setFilter(filter: HistoryFilter) {
        _filter.value = filter
    }

    fun setQuery(query: String) {
        _query.value = query
    }

    fun deleteLog(id: Long) {
        viewModelScope.launch {
            callLogRepository.deleteLog(id)
            _events.emit("Call log deleted")
        }
    }

    fun blockNumber(number: String) {
        viewModelScope.launch {
            blockedNumberRepository.blockNumber(number, "Blocked from call history")
            _events.emit("Blocked $number")
        }
    }

    fun saveTagAndNote(number: String, contactName: String?, tag: String, note: String) {
        viewModelScope.launch {
            val existing = contactRepository.findByNumber(number)
            val tags = buildList {
                if (existing != null) addAll(existing.tags)
                if (tag.isNotBlank()) add(tag.trim())
            }.distinct()
            val updated = existing?.copy(
                tags = tags,
                note = if (note.isNotBlank()) note.trim() else existing.note
            ) ?: Contact(
                name = contactName?.takeIf { it.isNotBlank() } ?: number,
                number = number,
                tags = tags,
                note = note.trim()
            )
            contactRepository.upsertContact(updated)
            _events.emit("Saved note/tag for ${updated.name}")
        }
    }

    fun callBack(log: CallLog) {
        viewModelScope.launch {
            val route = if (log.route == com.example.domain.model.CallRouteType.SIP) {
                val acc = log.sipAccountId?.let { sipRepository.getAccountById(it) }
                    ?: sipRepository.getAllAccounts().firstOrNull { it.enabled }
                if (acc != null) {
                    CallRoute.Sip(acc.id, acc.displayName.ifBlank { acc.username }, acc.domain)
                } else {
                    _events.emit("No active SIP account available")
                    return@launch
                }
            } else {
                val sims = simCallManager.getActiveSims()
                val sim = sims.firstOrNull { it.subscriptionId == log.simSubscriptionId }
                    ?: sims.firstOrNull()
                if (sim != null) {
                    CallRoute.Sim(sim.subscriptionId, sim.slotIndex, sim.carrierName)
                } else {
                    _events.emit("No active SIM card available")
                    return@launch
                }
            }
            placeCallUseCase(log.number, route).onFailure {
                _events.emit(it.message ?: "Call failed")
            }
        }
    }
}
