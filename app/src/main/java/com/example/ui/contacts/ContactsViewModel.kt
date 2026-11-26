package com.example.ui.contacts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.model.CallRoute
import com.example.domain.model.Contact
import com.example.domain.repository.BlockedNumberRepository
import com.example.domain.repository.ContactRepository
import com.example.domain.repository.SipRepository
import com.example.domain.usecase.PlaceCallUseCase
import com.example.domain.usecase.SearchContactsT9UseCase
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
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ContactsViewModel @Inject constructor(
    private val contactRepository: ContactRepository,
    private val searchContactsT9UseCase: SearchContactsT9UseCase,
    private val blockedNumberRepository: BlockedNumberRepository,
    private val sipRepository: SipRepository,
    private val simCallManager: SimCallManager,
    private val placeCallUseCase: PlaceCallUseCase
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _selectedContact = MutableStateFlow<Contact?>(null)
    val selectedContact: StateFlow<Contact?> = _selectedContact.asStateFlow()

    private val _availableRoutes = MutableStateFlow<List<CallRoute>>(emptyList())
    val availableRoutes: StateFlow<List<CallRoute>> = _availableRoutes.asStateFlow()

    private val _events = MutableSharedFlow<String>()
    val events: SharedFlow<String> = _events.asSharedFlow()

    val contacts: StateFlow<List<Contact>> = _query
        .flatMapLatest { q -> searchContactsT9UseCase(q) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch {
            contactRepository.syncDeviceContacts()
        }
    }

    fun setQuery(q: String) {
        _query.value = q
    }

    fun syncContacts() {
        viewModelScope.launch {
            contactRepository.syncDeviceContacts()
            _events.emit("Contacts synced")
        }
    }

    fun loadContact(contactId: Long) {
        viewModelScope.launch {
            _selectedContact.value = contactRepository.getContactById(contactId)
        }
    }

    fun saveNewContact(name: String, number: String, tagsCsv: String, note: String) {
        if (name.isBlank() || number.isBlank()) return
        viewModelScope.launch {
            val tags = tagsCsv.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            contactRepository.upsertContact(
                Contact(name = name.trim(), number = number.trim(), tags = tags, note = note.trim())
            )
            _events.emit("Contact saved")
        }
    }

    fun updateContactDetails(contact: Contact) {
        viewModelScope.launch {
            contactRepository.upsertContact(contact)
            _selectedContact.value = contact
            _events.emit("Contact updated")
        }
    }

    fun deleteContact(id: Long) {
        viewModelScope.launch {
            contactRepository.deleteContact(id)
            _events.emit("Contact deleted")
        }
    }

    fun blockNumber(number: String) {
        viewModelScope.launch {
            blockedNumberRepository.blockNumber(number, "Blocked from contacts")
            _events.emit("Blocked $number")
        }
    }

    fun refreshRoutes() {
        viewModelScope.launch {
            val sims = simCallManager.getActiveSims().map {
                CallRoute.Sim(it.subscriptionId, it.slotIndex, it.carrierName)
            }
            val sips = sipRepository.getAllAccounts()
                .filter { it.enabled }
                .map { CallRoute.Sip(it.id, it.displayName.ifBlank { it.username }, it.domain) }
            _availableRoutes.value = sims + sips
        }
    }

    fun placeCall(number: String, route: CallRoute) {
        viewModelScope.launch {
            placeCallUseCase(number, route).onFailure {
                _events.emit(it.message ?: "Call failed")
            }
        }
    }
}
