package com.example.ui.sms

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.model.Contact
import com.example.domain.model.SmsMessage
import com.example.domain.model.SmsThread
import com.example.domain.repository.ContactRepository
import com.example.domain.repository.SmsRepository
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
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class SmsViewModel @Inject constructor(
    private val smsRepository: SmsRepository,
    private val contactRepository: ContactRepository
) : ViewModel() {

    private val _activeThreadId = MutableStateFlow(0L)
    val activeThreadId: StateFlow<Long> = _activeThreadId.asStateFlow()

    private val _events = MutableSharedFlow<String>()
    val events: SharedFlow<String> = _events.asSharedFlow()

    val threads: StateFlow<List<SmsThread>> = smsRepository.observeThreads()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val contacts: StateFlow<List<Contact>> = contactRepository.observeContacts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val messages: StateFlow<List<SmsMessage>> = _activeThreadId
        .flatMapLatest { tid -> smsRepository.observeMessagesForThread(tid) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch {
            smsRepository.syncSystemSms()
        }
    }

    fun syncSms() {
        viewModelScope.launch {
            smsRepository.syncSystemSms()
            _events.emit("Messages refreshed")
        }
    }

    fun openThread(threadId: Long, address: String) {
        val resolved = if (threadId > 0L) threadId else computeThreadId(address)
        _activeThreadId.value = resolved
        viewModelScope.launch {
            smsRepository.markThreadRead(resolved)
        }
    }

    fun sendSms(address: String, body: String) {
        viewModelScope.launch {
            val result = smsRepository.sendSms(address, body)
            result.onSuccess { msg ->
                _activeThreadId.value = msg.threadId
            }.onFailure { err ->
                _events.emit(err.message ?: "Failed to send SMS")
            }
        }
    }

    fun computeThreadId(address: String): Long {
        val clean = PhoneUtils.normalizeNumber(address)
        val digits = clean.filter { it.isDigit() }.takeLast(10)
        return (digits.ifEmpty { clean }).hashCode().toLong() and 0x7FFFFFFFL
    }
}
