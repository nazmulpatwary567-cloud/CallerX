package com.example.ui.call

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.SecurePrefsManager
import com.example.domain.model.ActiveCallInfo
import com.example.domain.model.CallState
import com.example.domain.model.CallStats
import com.example.domain.repository.SettingsRepository
import com.example.service.RecordingService
import com.example.sip.SipManager
import com.example.util.NetworkTypeLabel
import com.example.util.NetworkUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class CallViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val sipManager: SipManager,
    private val settingsRepository: SettingsRepository,
    private val securePrefsManager: SecurePrefsManager
) : ViewModel() {

    val activeCall: StateFlow<ActiveCallInfo> = sipManager.activeCall
    val callState: StateFlow<CallState> = sipManager.activeCall
        .map { it.state }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CallState.IDLE)

    val callStats: StateFlow<CallStats> = sipManager.callStats
    val isMuted: StateFlow<Boolean> = sipManager.isMuted
    val isSpeakerOn: StateFlow<Boolean> = sipManager.isSpeakerOn
    val isBluetoothOn: StateFlow<Boolean> = sipManager.isBluetoothOn
    val isHeld: StateFlow<Boolean> = sipManager.isHeld
    val hasMultipleCalls: StateFlow<Boolean> = sipManager.hasMultipleCalls

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _durationSeconds = MutableStateFlow(0L)
    val durationSeconds: StateFlow<Long> = _durationSeconds.asStateFlow()

    private val _autoRecordEnabled = MutableStateFlow(false)
    val autoRecordEnabled: StateFlow<Boolean> = _autoRecordEnabled.asStateFlow()

    private val _showConsentDialog = MutableStateFlow(false)
    val showConsentDialog: StateFlow<Boolean> = _showConsentDialog.asStateFlow()

    private val _networkLabel = MutableStateFlow(NetworkUtils.detectNetworkType(context))
    val networkLabel: StateFlow<NetworkTypeLabel> = _networkLabel.asStateFlow()

    init {
        viewModelScope.launch {
            val auto = settingsRepository.getSetting(KEY_AUTO_RECORD)?.toBooleanStrictOrNull() ?: false
            _autoRecordEnabled.value = auto
        }
    }

    fun tickDuration() {
        val state = callState.value
        if (state == CallState.ACTIVE) {
            _durationSeconds.value = ((System.currentTimeMillis() - activeCall.value.startTimeMs) / 1000L)
                .coerceAtLeast(0L)
        }
        _networkLabel.value = NetworkUtils.detectNetworkType(context)
    }

    fun toggleMute() {
        sipManager.toggleMute()
    }

    fun toggleSpeaker() {
        sipManager.toggleSpeaker()
    }

    fun toggleBluetooth() {
        sipManager.toggleBluetoothAudio()
    }

    fun toggleHold() {
        sipManager.toggleHold()
    }

    fun sendDtmf(digit: Char) {
        sipManager.sendDtmf(digit)
    }

    fun addCall(number: String) {
        sipManager.addToConference(number)
    }

    fun transferCall(number: String) {
        sipManager.transferCall(number)
    }

    fun swapCalls() {
        sipManager.swapCalls()
    }

    fun hangup() {
        if (_isRecording.value) {
            stopRecording()
        }
        sipManager.hangup()
    }

    fun answerIncoming() {
        sipManager.answerCall()
    }

    fun declineIncoming() {
        sipManager.declineCall()
    }

    fun toggleAutoRecordInCall(enabled: Boolean) {
        _autoRecordEnabled.value = enabled
        viewModelScope.launch {
            settingsRepository.setSetting(KEY_AUTO_RECORD, enabled.toString())
        }
        if (enabled && callState.value == CallState.ACTIVE && !_isRecording.value) {
            requestStartRecording()
        }
    }

    fun onRecordButtonPressed() {
        if (_isRecording.value) {
            stopRecording()
        } else {
            requestStartRecording()
        }
    }

    fun maybeStartAutoRecording() {
        if (_autoRecordEnabled.value && !_isRecording.value) {
            requestStartRecording()
        }
    }

    private fun requestStartRecording() {
        if (!securePrefsManager.hasRecordingConsent()) {
            _showConsentDialog.value = true
        } else {
            startRecordingConfirmed()
        }
    }

    fun acceptConsentAndRecord() {
        securePrefsManager.setRecordingConsent(true)
        _showConsentDialog.value = false
        startRecordingConfirmed()
    }

    fun dismissConsentDialog() {
        _showConsentDialog.value = false
    }

    private fun startRecordingConfirmed() {
        val info = activeCall.value
        val logId = info.callLogId ?: 0L
        val title = info.remoteName?.takeIf { it.isNotBlank() } ?: info.remoteNumber
        if (info.isSip) {
            val dir = File(context.filesDir, "recordings").apply { mkdirs() }
            val path = File(dir, "sip_${logId}_${System.currentTimeMillis()}.wav").absolutePath
            sipManager.startRecording(path)
        }
        RecordingService.startRecording(context, logId, title)
        _isRecording.value = true
    }

    private fun stopRecording() {
        if (activeCall.value.isSip) {
            sipManager.stopRecording()
        }
        RecordingService.stopRecording(context)
        _isRecording.value = false
    }

    companion object {
        const val KEY_AUTO_RECORD = "auto_call_recording"
    }
}
