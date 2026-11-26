package com.example.ui.dialer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.model.AccountStatus
import com.example.domain.model.CallRoute
import com.example.domain.model.Contact
import com.example.domain.model.SpeedDial
import com.example.domain.repository.SipRepository
import com.example.domain.repository.SpeedDialRepository
import com.example.domain.usecase.PlaceCallUseCase
import com.example.domain.usecase.SearchContactsT9UseCase
import com.example.sip.SipManager
import com.example.telecom.SimCallManager
import com.example.util.PhoneUtils
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
class DialerViewModel @Inject constructor(
    private val searchContactsT9UseCase: SearchContactsT9UseCase,
    private val placeCallUseCase: PlaceCallUseCase,
    private val speedDialRepository: SpeedDialRepository,
    private val sipRepository: SipRepository,
    private val sipManager: SipManager,
    private val simCallManager: SimCallManager
) : ViewModel() {

    private val _number = MutableStateFlow("")
    val number: StateFlow<String> = _number.asStateFlow()

    private val _showRoutePicker = MutableStateFlow(false)
    val showRoutePicker: StateFlow<Boolean> = _showRoutePicker.asStateFlow()

    private val _availableRoutes = MutableStateFlow<List<CallRoute>>(emptyList())
    val availableRoutes: StateFlow<List<CallRoute>> = _availableRoutes.asStateFlow()

    private val _events = MutableSharedFlow<String>()
    val events: SharedFlow<String> = _events.asSharedFlow()

    val matchedContacts: StateFlow<List<Contact>> = _number
        .flatMapLatest { query -> searchContactsT9UseCase(query) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val exactMatchedName: StateFlow<String?> = combine(_number, matchedContacts) { num, list ->
        if (num.isBlank()) null
        else list.firstOrNull { PhoneUtils.numbersMatch(it.number, num) }?.name
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val speedDials: StateFlow<List<SpeedDial>> = speedDialRepository.observeSpeedDials()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val sipStatuses: StateFlow<Map<Long, AccountStatus>> = sipManager.allStatuses

    fun setNumber(value: String) {
        _number.value = value
    }

    fun appendDigit(digit: String) {
        _number.value += digit
    }

    fun backspace() {
        if (_number.value.isNotEmpty()) {
            _number.value = _number.value.dropLast(1)
        }
    }

    fun clear() {
        _number.value = ""
    }

    fun paste(text: String) {
        val cleaned = text.trim()
        if (cleaned.isNotEmpty()) {
            _number.value = cleaned
        }
    }

    fun onVoicemailLongPress(fallbackNumber: String) {
        val vm = simCallManager.getVoicemailNumber(fallbackNumber)
        _number.value = vm
        onCallPressed()
    }

    fun onSpeedDialLongPress(key: Int) {
        viewModelScope.launch {
            val sd = speedDialRepository.getSpeedDial(key)
            if (sd == null || sd.number.isBlank()) {
                _events.emit("Speed dial #$key is not set")
            } else {
                _number.value = sd.number
                onCallPressed()
            }
        }
    }

    fun onCallPressed() {
        val target = _number.value.trim()
        if (target.isEmpty()) {
            viewModelScope.launch { _events.emit("Enter a phone number or SIP URI") }
            return
        }
        viewModelScope.launch {
            val routes = loadAvailableRoutes()
            _availableRoutes.value = routes
            val defaultToken = simCallManager.getDefaultRoute()
            val defaultRoute = resolveRouteToken(defaultToken, routes)
            if (defaultRoute != null) {
                initiateCall(defaultRoute)
            } else if (routes.size == 1) {
                initiateCall(routes.first())
            } else {
                _showRoutePicker.value = true
            }
        }
    }

    fun dismissRoutePicker() {
        _showRoutePicker.value = false
    }

    fun selectRouteAndCall(route: CallRoute, rememberAsDefault: Boolean) {
        _showRoutePicker.value = false
        viewModelScope.launch {
            if (rememberAsDefault) {
                val token = when (route) {
                    is CallRoute.Sim -> "SIM:${route.subscriptionId}"
                    is CallRoute.Sip -> "SIP:${route.accountId}"
                }
                simCallManager.setDefaultRoute(token)
            }
            initiateCall(route)
        }
    }

    fun initiateCall(route: CallRoute) {
        val target = _number.value.trim()
        if (target.isEmpty()) return
        viewModelScope.launch {
            val result = placeCallUseCase(target, route)
            result.onFailure { err ->
                _events.emit(err.message ?: "Unable to place call")
            }
        }
    }

    private suspend fun loadAvailableRoutes(): List<CallRoute> {
        val sims = simCallManager.getActiveSims().map {
            CallRoute.Sim(it.subscriptionId, it.slotIndex, it.carrierName)
        }
        val sipAccounts = sipRepository.getAllAccounts()
            .filter { it.enabled }
            .map { CallRoute.Sip(it.id, it.displayName.ifBlank { it.username }, it.domain) }
        return sims + sipAccounts
    }

    private fun resolveRouteToken(token: String?, routes: List<CallRoute>): CallRoute? {
        if (token.isNullOrBlank()) return null
        return when {
            token.startsWith("SIM:") -> {
                val subId = token.removePrefix("SIM:").toIntOrNull() ?: return null
                routes.filterIsInstance<CallRoute.Sim>().firstOrNull { it.subscriptionId == subId }
            }
            token.startsWith("SIP:") -> {
                val accId = token.removePrefix("SIP:").toLongOrNull() ?: return null
                routes.filterIsInstance<CallRoute.Sip>().firstOrNull { it.accountId == accId }
            }
            else -> null
        }
    }
}
