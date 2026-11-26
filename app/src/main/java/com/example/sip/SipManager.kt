package com.example.sip

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.util.Log
import com.example.data.remote.SipPingService
import com.example.domain.model.AccountStatus
import com.example.domain.model.ActiveCallInfo
import com.example.domain.model.CallRouteType
import com.example.domain.model.CallState
import com.example.domain.model.CallStats
import com.example.domain.model.CallType
import com.example.domain.model.SipAccount
import com.example.domain.model.SipTransport
import com.example.domain.repository.CallLogRepository
import com.example.domain.repository.ContactRepository
import com.example.domain.repository.SettingsRepository
import com.example.domain.repository.SipRepository
import com.example.domain.usecase.EvaluateIncomingCallUseCase
import com.example.ui.call.CallActivity
import com.example.ui.call.IncomingCallActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.linphone.core.Account
import org.linphone.core.Call
import org.linphone.core.Core
import org.linphone.core.CoreListenerStub
import org.linphone.core.Factory
import org.linphone.core.MediaEncryption
import org.linphone.core.Reason
import org.linphone.core.RegistrationState
import org.linphone.core.TransportType
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SipManager @Inject constructor(
    private val sipRepository: SipRepository,
    private val settingsRepository: SettingsRepository,
    private val callLogRepository: CallLogRepository,
    private val contactRepository: ContactRepository,
    private val evaluateIncomingCallUseCase: EvaluateIncomingCallUseCase,
    private val sipPingService: SipPingService
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private var appContext: Context? = null
    private var core: Core? = null
    private var iterateJob: Job? = null
    private var statsJob: Job? = null

    private val accountMap = mutableMapOf<Long, Account>()
    private val accountStatuses = mutableMapOf<Long, MutableStateFlow<AccountStatus>>()

    private val _allStatuses = MutableStateFlow<Map<Long, AccountStatus>>(emptyMap())
    val allStatuses: StateFlow<Map<Long, AccountStatus>> = _allStatuses.asStateFlow()

    private val _activeCall = MutableStateFlow(ActiveCallInfo())
    val activeCall: StateFlow<ActiveCallInfo> = _activeCall.asStateFlow()

    private val _callStats = MutableStateFlow(CallStats())
    val callStats: StateFlow<CallStats> = _callStats.asStateFlow()

    private val _isMuted = MutableStateFlow(false)
    val isMuted: StateFlow<Boolean> = _isMuted.asStateFlow()

    private val _isSpeakerOn = MutableStateFlow(false)
    val isSpeakerOn: StateFlow<Boolean> = _isSpeakerOn.asStateFlow()

    private val _isBluetoothOn = MutableStateFlow(false)
    val isBluetoothOn: StateFlow<Boolean> = _isBluetoothOn.asStateFlow()

    private val _isHeld = MutableStateFlow(false)
    val isHeld: StateFlow<Boolean> = _isHeld.asStateFlow()

    private val _hasMultipleCalls = MutableStateFlow(false)
    val hasMultipleCalls: StateFlow<Boolean> = _hasMultipleCalls.asStateFlow()

    private val _isSipRecording = MutableStateFlow(false)
    val isSipRecording: StateFlow<Boolean> = _isSipRecording.asStateFlow()

    private val coreListener = object : CoreListenerStub() {
        override fun onAccountRegistrationStateChanged(
            core: Core,
            account: Account,
            state: RegistrationState?,
            message: String
        ) {
            val id = accountMap.entries.firstOrNull { it.value == account }?.key ?: return
            val mapped = when (state) {
                RegistrationState.Ok -> AccountStatus.ONLINE
                RegistrationState.Progress, RegistrationState.Refreshing -> AccountStatus.REGISTERING
                RegistrationState.Failed -> AccountStatus.ERROR
                RegistrationState.Cleared, RegistrationState.None, null -> AccountStatus.OFFLINE
            }
            updateAccountStatus(id, mapped)
        }

        override fun onCallStateChanged(
            core: Core,
            call: Call,
            state: Call.State?,
            message: String
        ) {
            _hasMultipleCalls.value = core.callsNb > 1
            val remote = call.remoteAddress?.username
                ?: call.remoteAddress?.asStringUriOnly()
                ?: "Unknown"

            when (state) {
                Call.State.IncomingReceived, Call.State.IncomingEarlyMedia -> {
                    scope.launch {
                        val localUri = call.callLog?.localAddress?.asStringUriOnly()
                        val accountId = accountMap.entries.firstOrNull {
                            it.value.params.identityAddress?.asStringUriOnly() == localUri
                        }?.key ?: accountMap.keys.firstOrNull()
                        val shouldBlock = evaluateIncomingCallUseCase.shouldBlockIncoming(
                            rawNumber = remote,
                            route = CallRouteType.SIP,
                            sipAccountId = accountId
                        )
                        if (shouldBlock) {
                            call.decline(Reason.Declined)
                            return@launch
                        }

                        val callWaitingEnabled = settingsRepository.getSetting("call_waiting")
                            ?.toBooleanStrictOrNull() ?: true
                        if (!callWaitingEnabled && core.callsNb > 1) {
                            call.decline(Reason.Busy)
                            return@launch
                        }

                        val forwardTo = settingsRepository.getSetting("call_forwarding_number")
                            ?.trim().orEmpty()
                        if (forwardTo.isNotBlank()) {
                            runCatching { call.redirect(forwardTo) }
                            return@launch
                        }

                        val contact = contactRepository.findByNumber(remote)
                        val logId = callLogRepository.insertLog(
                            com.example.domain.model.CallLog(
                                number = remote,
                                contactName = contact?.name,
                                type = CallType.INCOMING,
                                route = CallRouteType.SIP,
                                sipAccountId = accountId,
                                routeLabel = "SIP",
                                startTime = System.currentTimeMillis()
                            )
                        )
                        _activeCall.value = ActiveCallInfo(
                            callId = call.callLog?.callId ?: System.currentTimeMillis().toString(),
                            remoteNumber = remote,
                            remoteName = contact?.name,
                            state = CallState.RINGING,
                            routeLabel = "SIP",
                            isSip = true,
                            sipAccountId = accountId,
                            callLogId = logId
                        )

                        val lockscreenUiEnabled = settingsRepository.getSetting("lockscreen_call_ui")
                            ?.toBooleanStrictOrNull() ?: true
                        val keyguard = appContext?.getSystemService(Context.KEYGUARD_SERVICE) as? android.app.KeyguardManager
                        val isDeviceLocked = keyguard?.isKeyguardLocked == true
                        if (lockscreenUiEnabled || !isDeviceLocked) {
                            launchIncomingActivity()
                        } else {
                            postIncomingNotification(contact?.name ?: remote)
                        }
                    }
                }

                Call.State.OutgoingInit, Call.State.OutgoingProgress -> {
                    _activeCall.update { it.copy(state = CallState.DIALING, remoteNumber = remote) }
                }

                Call.State.OutgoingRinging, Call.State.OutgoingEarlyMedia -> {
                    _activeCall.update { it.copy(state = CallState.RINGING, remoteNumber = remote) }
                }

                Call.State.Connected, Call.State.StreamsRunning -> {
                    _isHeld.value = false
                    _activeCall.update {
                        if (it.state != CallState.ACTIVE) {
                            it.copy(state = CallState.ACTIVE, startTimeMs = System.currentTimeMillis())
                        } else {
                            it.copy(state = CallState.ACTIVE)
                        }
                    }
                    startCallStatsPolling()
                }

                Call.State.Paused, Call.State.Pausing, Call.State.PausedByRemote -> {
                    _isHeld.value = true
                    _activeCall.update { it.copy(state = CallState.HELD) }
                }

                Call.State.End, Call.State.Released, Call.State.Error -> {
                    stopCallStatsPolling()
                    val current = _activeCall.value
                    val durationSec = if (current.state == CallState.ACTIVE || current.state == CallState.HELD) {
                        ((System.currentTimeMillis() - current.startTimeMs) / 1000L).coerceAtLeast(0L)
                    } else 0L

                    scope.launch {
                        current.callLogId?.let { logId ->
                            callLogRepository.updateDuration(logId, durationSec)
                            if (durationSec > 0) {
                                contactRepository.addTalkTime(current.remoteNumber, durationSec)
                            }
                        }
                    }

                    _isMuted.value = false
                    _isHeld.value = false
                    _isSipRecording.value = false
                    if (core.callsNb == 0) {
                        _isSpeakerOn.value = false
                        _isBluetoothOn.value = false
                        _hasMultipleCalls.value = false
                    }
                    _activeCall.update { it.copy(state = CallState.ENDED) }
                }

                else -> Unit
            }
        }
    }

    fun init(context: Context) {
        if (core != null) return
        appContext = context.applicationContext
        try {
            val factory = Factory.instance()
            factory.setDebugMode(false, "CallerX-SIP")
            val createdCore = factory.createCore(null, null, context.applicationContext)
            createdCore.addListener(coreListener)
            createdCore.start()
            core = createdCore

            iterateJob = scope.launch {
                while (isActive) {
                    createdCore.iterate()
                    delay(40L)
                }
            }

            scope.launch {
                val enforceSrtp = settingsRepository.getSetting(KEY_ENFORCE_TLS_SRTP)
                    ?.toBooleanStrictOrNull() ?: false
                applyEncryptionPolicy(enforceSrtp)
                sipRepository.getAllAccounts().filter { it.enabled }.forEach {
                    addAccount(it)
                }
            }
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to initialize LinphoneCore", t)
        }
    }

    suspend fun applyEncryptionPolicy(enforceTlsSrtp: Boolean) {
        val c = core ?: return
        c.mediaEncryption = if (enforceTlsSrtp) MediaEncryption.SRTP else MediaEncryption.None
        c.isMediaEncryptionMandatory = enforceTlsSrtp
    }

    fun addAccount(account: SipAccount) {
        val c = core ?: return
        scope.launch {
            val enforceTlsSrtp = settingsRepository.getSetting(KEY_ENFORCE_TLS_SRTP)
                ?.toBooleanStrictOrNull() ?: false

            removeAccountInternal(account.id)
            updateAccountStatus(account.id, if (account.enabled) AccountStatus.REGISTERING else AccountStatus.OFFLINE)

            try {
                val factory = Factory.instance()
                val effectiveTransport = if (enforceTlsSrtp) SipTransport.TLS else account.transport
                val transportType = when (effectiveTransport) {
                    SipTransport.UDP -> TransportType.Udp
                    SipTransport.TCP -> TransportType.Tcp
                    SipTransport.TLS -> TransportType.Tls
                    SipTransport.WSS -> runCatching { TransportType.valueOf("Wss") }
                        .getOrDefault(TransportType.Tls)
                }

                if (effectiveTransport == SipTransport.TLS || enforceTlsSrtp) {
                    c.mediaEncryption = MediaEncryption.SRTP
                }

                val authInfo = factory.createAuthInfo(
                    account.username,
                    null,
                    account.password,
                    null,
                    null,
                    account.domain
                )
                c.addAuthInfo(authInfo)

                val params = c.createAccountParams()
                val identity = factory.createAddress("sip:${account.username}@${account.domain}")
                val server = factory.createAddress("sip:${account.domain}:${account.port}")
                server?.transport = transportType

                params.identityAddress = identity
                params.serverAddress = server
                params.isRegisterEnabled = account.enabled

                val linphoneAccount = c.createAccount(params)
                c.addAccount(linphoneAccount)
                if (c.defaultAccount == null) {
                    c.defaultAccount = linphoneAccount
                }
                accountMap[account.id] = linphoneAccount
            } catch (t: Throwable) {
                Log.e(TAG, "Failed to register SIP account ${account.username}@${account.domain}", t)
                updateAccountStatus(account.id, AccountStatus.ERROR)
            }
        }
    }

    fun removeAccount(id: Long) {
        removeAccountInternal(id)
        accountStatuses.remove(id)
        _allStatuses.update { it - id }
    }

    private fun removeAccountInternal(id: Long) {
        val c = core ?: return
        accountMap.remove(id)?.let { existing ->
            runCatching { c.removeAccount(existing) }
        }
    }

    fun getAccountStatus(id: Long): StateFlow<AccountStatus> {
        return accountStatuses.getOrPut(id) {
            MutableStateFlow(_allStatuses.value[id] ?: AccountStatus.OFFLINE)
        }.asStateFlow()
    }

    private fun updateAccountStatus(id: Long, status: AccountStatus) {
        accountStatuses.getOrPut(id) { MutableStateFlow(status) }.value = status
        _allStatuses.update { it + (id to status) }
    }

    suspend fun getServerLatency(id: Long): Long? {
        val account = sipRepository.getAccountById(id) ?: return null
        return sipPingService.pingServer(account)
    }

    fun makeCall(
        number: String,
        accountId: Long,
        contactName: String? = null,
        callLogId: Long? = null
    ) {
        val c = core ?: return
        val account = accountMap[accountId] ?: c.defaultAccount
        if (account != null) {
            c.defaultAccount = account
        }
        val target = if (number.contains("@")) "sip:$number" else number
        val address = c.interpretUrl(target) ?: return
        val params = c.createCallParams(null) ?: return
        scope.launch {
            val enforceSrtp = settingsRepository.getSetting(KEY_ENFORCE_TLS_SRTP)
                ?.toBooleanStrictOrNull() ?: false
            if (enforceSrtp) {
                params.mediaEncryption = MediaEncryption.SRTP
            }
            val call = c.inviteAddressWithParams(address, params)
            _activeCall.value = ActiveCallInfo(
                callId = call?.callLog?.callId ?: System.currentTimeMillis().toString(),
                remoteNumber = number,
                remoteName = contactName,
                state = CallState.DIALING,
                routeLabel = account?.params?.identityAddress?.domain ?: "SIP",
                isSip = true,
                sipAccountId = accountId,
                callLogId = callLogId
            )
            launchCallActivity()
        }
    }

    fun answerCall() {
        val call = core?.currentCall ?: core?.calls?.firstOrNull() ?: return
        runCatching { call.accept() }
        launchCallActivity()
    }

    fun declineCall() {
        val call = core?.currentCall ?: core?.calls?.firstOrNull() ?: return
        runCatching { call.decline(Reason.Declined) }
    }

    fun hangup() {
        val c = core ?: return
        val call = c.currentCall ?: c.calls.firstOrNull()
        if (call != null) {
            runCatching { call.terminate() }
        } else {
            runCatching { c.terminateAllCalls() }
        }
    }

    fun toggleMute(): Boolean {
        val c = core ?: return false
        val newMuted = !_isMuted.value
        c.isMicEnabled = !newMuted
        _isMuted.value = newMuted
        return newMuted
    }

    fun toggleSpeaker(): Boolean {
        val ctx = appContext ?: return false
        val am = ctx.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return false
        val next = !_isSpeakerOn.value
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            if (next) {
                val speaker = am.availableCommunicationDevices.firstOrNull {
                    it.type == android.media.AudioDeviceInfo.TYPE_BUILTIN_SPEAKER
                }
                if (speaker != null) am.setCommunicationDevice(speaker)
            } else {
                am.clearCommunicationDevice()
            }
        } else {
            @Suppress("DEPRECATION")
            am.isSpeakerphoneOn = next
        }
        if (next) {
            _isBluetoothOn.value = false
        }
        _isSpeakerOn.value = next
        return next
    }

    fun toggleBluetoothAudio(): Boolean {
        val ctx = appContext ?: return false
        val am = ctx.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return false
        val next = !_isBluetoothOn.value
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            if (next) {
                val btDevice = am.availableCommunicationDevices.firstOrNull {
                    it.type == android.media.AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
                        it.type == android.media.AudioDeviceInfo.TYPE_BLE_HEADSET ||
                        it.type == android.media.AudioDeviceInfo.TYPE_HEARING_AID
                }
                if (btDevice != null && am.setCommunicationDevice(btDevice)) {
                    _isSpeakerOn.value = false
                    _isBluetoothOn.value = true
                    return true
                }
                return false
            } else {
                am.clearCommunicationDevice()
                _isBluetoothOn.value = false
                return false
            }
        } else {
            @Suppress("DEPRECATION")
            if (next) {
                runCatching {
                    am.startBluetoothSco()
                    am.isBluetoothScoOn = true
                    am.isSpeakerphoneOn = false
                }
                _isSpeakerOn.value = false
                _isBluetoothOn.value = true
            } else {
                runCatching {
                    am.isBluetoothScoOn = false
                    am.stopBluetoothSco()
                }
                _isBluetoothOn.value = false
            }
            return _isBluetoothOn.value
        }
    }

    fun toggleHold(): Boolean {
        val call = core?.currentCall ?: core?.calls?.firstOrNull() ?: return false
        return if (_isHeld.value) {
            runCatching { call.resume() }
            _isHeld.value = false
            false
        } else {
            runCatching { call.pause() }
            _isHeld.value = true
            true
        }
    }

    fun startRecording(path: String) {
        val call = core?.currentCall ?: return
        runCatching {
            call.params.recordFile = path
            call.startRecording()
            _isSipRecording.value = true
        }
    }

    fun stopRecording() {
        val call = core?.currentCall ?: return
        runCatching {
            call.stopRecording()
            _isSipRecording.value = false
        }
    }

    fun transferCall(number: String) {
        val c = core ?: return
        val call = c.currentCall ?: return
        val address = c.interpretUrl(number) ?: return
        runCatching { call.transferTo(address) }
    }

    fun addToConference(callId: String) {
        val c = core ?: return
        val calls = c.calls
        if (calls.size > 1) {
            runCatching { c.addAllToConference() }
        } else if (callId.isNotBlank()) {
            val target = c.interpretUrl(callId) ?: return
            runCatching { c.inviteAddress(target) }
        }
    }

    fun swapCalls() {
        val c = core ?: return
        val calls = c.calls
        if (calls.size < 2) return
        val current = c.currentCall
        calls.forEach { call ->
            if (call == current) call.pause() else call.resume()
        }
    }

    fun sendDtmf(digit: Char) {
        core?.currentCall?.sendDtmf(digit)
    }

    private fun startCallStatsPolling() {
        statsJob?.cancel()
        statsJob = scope.launch {
            while (isActive) {
                val call = core?.currentCall
                if (call != null) {
                    val audioStats = call.audioStats
                    val jitter = audioStats?.jitterBufferSizeMs ?: 0f
                    val loss = audioStats?.receiverLossRate ?: 0f
                    val bars = when {
                        loss > 5f || jitter > 120f -> 1
                        loss > 2.5f || jitter > 80f -> 2
                        loss > 1f || jitter > 50f -> 3
                        loss > 0.3f || jitter > 25f -> 4
                        else -> 5
                    }
                    _callStats.value = CallStats(jitter, loss, bars)
                }
                delay(2_000L)
            }
        }
    }

    private fun stopCallStatsPolling() {
        statsJob?.cancel()
        statsJob = null
        _callStats.value = CallStats()
    }

    private fun launchCallActivity() {
        val ctx = appContext ?: return
        val intent = Intent(ctx, CallActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        ctx.startActivity(intent)
    }

    private fun launchIncomingActivity() {
        val ctx = appContext ?: return
        val intent = Intent(ctx, IncomingCallActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        ctx.startActivity(intent)
    }

    private fun postIncomingNotification(callerTitle: String) {
        val ctx = appContext ?: return
        val nm = ctx.getSystemService(android.app.NotificationManager::class.java) ?: return
        val answerIntent = android.app.PendingIntent.getService(
            ctx,
            21,
            Intent(ctx, com.example.service.SipService::class.java).apply {
                action = com.example.service.SipService.ACTION_ANSWER_CALL
            },
            android.app.PendingIntent.FLAG_IMMUTABLE or android.app.PendingIntent.FLAG_UPDATE_CURRENT
        )
        val declineIntent = android.app.PendingIntent.getService(
            ctx,
            22,
            Intent(ctx, com.example.service.SipService::class.java).apply {
                action = com.example.service.SipService.ACTION_DECLINE_CALL
            },
            android.app.PendingIntent.FLAG_IMMUTABLE or android.app.PendingIntent.FLAG_UPDATE_CURRENT
        )
        val notification = androidx.core.app.NotificationCompat.Builder(
            ctx,
            com.example.CallerXApp.CHANNEL_INCOMING_CALL
        )
            .setSmallIcon(com.example.R.drawable.ic_launcher_foreground)
            .setContentTitle("Incoming SIP Call")
            .setContentText(callerTitle)
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
            .addAction(0, "Answer", answerIntent)
            .addAction(0, "Decline", declineIntent)
            .setAutoCancel(true)
            .build()
        nm.notify(1003, notification)
    }

    fun shutdown() {
        iterateJob?.cancel()
        statsJob?.cancel()
        runCatching { core?.stop() }
        core = null
    }

    companion object {
        private const val TAG = "CallerXSipManager"
        const val KEY_ENFORCE_TLS_SRTP = "enforce_tls_srtp"
    }
}
