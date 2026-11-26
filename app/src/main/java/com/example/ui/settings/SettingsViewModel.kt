package com.example.ui.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.backup.BackupManager
import com.example.backup.BackupScheduler
import com.example.data.local.SecurePrefsManager
import com.example.domain.model.AccountStatus
import com.example.domain.model.AppThemeColor
import com.example.domain.model.BackupProvider
import com.example.domain.model.BlockedNumber
import com.example.domain.model.DarkModeOption
import com.example.domain.model.SimInfo
import com.example.domain.model.SipAccount
import com.example.domain.model.SpeedDial
import com.example.domain.repository.SettingsRepository
import com.example.domain.repository.SipRepository
import com.example.domain.repository.SpeedDialRepository
import com.example.privacy.AppLockManager
import com.example.privacy.BlockListManager
import com.example.receiver.BootReceiver
import com.example.service.SipService
import com.example.sip.SipManager
import com.example.telecom.SimCallManager
import com.example.ui.call.CallViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val sipRepository: SipRepository,
    private val settingsRepository: SettingsRepository,
    private val speedDialRepository: SpeedDialRepository,
    private val sipManager: SipManager,
    private val simCallManager: SimCallManager,
    private val appLockManager: AppLockManager,
    private val blockListManager: BlockListManager,
    private val backupManager: BackupManager,
    private val backupScheduler: BackupScheduler,
    private val securePrefsManager: SecurePrefsManager
) : ViewModel() {

    val sipAccounts: StateFlow<List<SipAccount>> = sipRepository.observeAccounts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val sipStatuses: StateFlow<Map<Long, AccountStatus>> = sipManager.allStatuses

    val blockedNumbers: StateFlow<List<BlockedNumber>> = blockListManager.observeBlockedNumbers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val speedDials: StateFlow<List<SpeedDial>> = speedDialRepository.observeSpeedDials()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val themeColor: StateFlow<AppThemeColor> = securePrefsManager.themeColor
    val darkMode: StateFlow<DarkModeOption> = securePrefsManager.darkMode
    val amoledBlack: StateFlow<Boolean> = securePrefsManager.amoledBlack

    private val _latencies = MutableStateFlow<Map<Long, Long?>>(emptyMap())
    val latencies: StateFlow<Map<Long, Long?>> = _latencies.asStateFlow()

    private val _defaultRoute = MutableStateFlow("")
    val defaultRoute: StateFlow<String> = _defaultRoute.asStateFlow()

    private val _autoRecord = MutableStateFlow(false)
    val autoRecord: StateFlow<Boolean> = _autoRecord.asStateFlow()

    private val _callWaiting = MutableStateFlow(true)
    val callWaiting: StateFlow<Boolean> = _callWaiting.asStateFlow()

    private val _callForwarding = MutableStateFlow("")
    val callForwarding: StateFlow<String> = _callForwarding.asStateFlow()

    private val _lockscreenCallUi = MutableStateFlow(true)
    val lockscreenCallUi: StateFlow<Boolean> = _lockscreenCallUi.asStateFlow()

    private val _backgroundSip = MutableStateFlow(true)
    val backgroundSip: StateFlow<Boolean> = _backgroundSip.asStateFlow()

    private val _enforceTlsSrtp = MutableStateFlow(false)
    val enforceTlsSrtp: StateFlow<Boolean> = _enforceTlsSrtp.asStateFlow()

    private val _blockUnknown = MutableStateFlow(false)
    val blockUnknown: StateFlow<Boolean> = _blockUnknown.asStateFlow()

    private val _appLockEnabled = MutableStateFlow(appLockManager.isLockEnabled())
    val appLockEnabled: StateFlow<Boolean> = _appLockEnabled.asStateFlow()

    private val _biometricEnabled = MutableStateFlow(appLockManager.isBiometricEnabled())
    val biometricEnabled: StateFlow<Boolean> = _biometricEnabled.asStateFlow()

    private val _autoLockMinutes = MutableStateFlow(appLockManager.getAutoLockMinutes())
    val autoLockMinutes: StateFlow<Int> = _autoLockMinutes.asStateFlow()

    private val _autoBackupEnabled = MutableStateFlow(securePrefsManager.isAutoBackupEnabled())
    val autoBackupEnabled: StateFlow<Boolean> = _autoBackupEnabled.asStateFlow()

    private val _backupProvider = MutableStateFlow(securePrefsManager.getBackupProvider())
    val backupProvider: StateFlow<BackupProvider> = _backupProvider.asStateFlow()

    private val _backupFreqHours = MutableStateFlow(securePrefsManager.getBackupFrequencyHours())
    val backupFreqHours: StateFlow<Int> = _backupFreqHours.asStateFlow()

    private val _lastBackupTimestamp = MutableStateFlow(securePrefsManager.getLastBackupTimestamp())
    val lastBackupTimestamp: StateFlow<Long> = _lastBackupTimestamp.asStateFlow()

    private val _activeSims = MutableStateFlow<List<SimInfo>>(emptyList())
    val activeSims: StateFlow<List<SimInfo>> = _activeSims.asStateFlow()

    private val _showRecordingConsent = MutableStateFlow(false)
    val showRecordingConsent: StateFlow<Boolean> = _showRecordingConsent.asStateFlow()

    private val _events = MutableSharedFlow<String>()
    val events: SharedFlow<String> = _events.asSharedFlow()

    init {
        loadSettings()
    }

    fun loadSettings() {
        viewModelScope.launch {
            _activeSims.value = simCallManager.getActiveSims()
            _defaultRoute.value = settingsRepository.getSetting(SimCallManager.KEY_DEFAULT_ROUTE).orEmpty()
            _autoRecord.value = settingsRepository.getSetting(CallViewModel.KEY_AUTO_RECORD)?.toBooleanStrictOrNull() ?: false
            _callWaiting.value = settingsRepository.getSetting(KEY_CALL_WAITING)?.toBooleanStrictOrNull() ?: true
            _callForwarding.value = settingsRepository.getSetting(KEY_CALL_FORWARDING).orEmpty()
            _lockscreenCallUi.value = settingsRepository.getSetting(KEY_LOCKSCREEN_CALL_UI)?.toBooleanStrictOrNull() ?: true
            _backgroundSip.value = settingsRepository.getSetting(BootReceiver.KEY_BACKGROUND_SIP)?.toBooleanStrictOrNull() ?: true
            _enforceTlsSrtp.value = settingsRepository.getSetting(SipManager.KEY_ENFORCE_TLS_SRTP)?.toBooleanStrictOrNull() ?: false
            _blockUnknown.value = settingsRepository.getSetting(com.example.domain.usecase.EvaluateIncomingCallUseCase.KEY_BLOCK_UNKNOWN)?.toBooleanStrictOrNull() ?: false
            _appLockEnabled.value = appLockManager.isLockEnabled()
            _biometricEnabled.value = appLockManager.isBiometricEnabled()
            _autoLockMinutes.value = appLockManager.getAutoLockMinutes()
        }
    }

    fun setDefaultRoute(token: String) {
        _defaultRoute.value = token
        viewModelScope.launch {
            settingsRepository.setSetting(SimCallManager.KEY_DEFAULT_ROUTE, token)
        }
    }

    fun onAutoRecordChanged(enabled: Boolean) {
        if (enabled && !securePrefsManager.hasRecordingConsent()) {
            _showRecordingConsent.value = true
            return
        }
        _autoRecord.value = enabled
        viewModelScope.launch {
            settingsRepository.setSetting(CallViewModel.KEY_AUTO_RECORD, enabled.toString())
        }
    }

    fun acceptRecordingConsent() {
        securePrefsManager.setRecordingConsent(true)
        _showRecordingConsent.value = false
        _autoRecord.value = true
        viewModelScope.launch {
            settingsRepository.setSetting(CallViewModel.KEY_AUTO_RECORD, "true")
        }
    }

    fun dismissRecordingConsent() {
        _showRecordingConsent.value = false
    }

    fun setCallWaiting(enabled: Boolean) {
        _callWaiting.value = enabled
        viewModelScope.launch { settingsRepository.setSetting(KEY_CALL_WAITING, enabled.toString()) }
    }

    fun setCallForwarding(number: String) {
        _callForwarding.value = number.trim()
        viewModelScope.launch {
            settingsRepository.setSetting(KEY_CALL_FORWARDING, number.trim())
            _events.emit("Call forwarding updated")
        }
    }

    fun setLockscreenCallUi(enabled: Boolean) {
        _lockscreenCallUi.value = enabled
        viewModelScope.launch { settingsRepository.setSetting(KEY_LOCKSCREEN_CALL_UI, enabled.toString()) }
    }

    fun setBackgroundSip(enabled: Boolean) {
        _backgroundSip.value = enabled
        viewModelScope.launch {
            settingsRepository.setSetting(BootReceiver.KEY_BACKGROUND_SIP, enabled.toString())
            if (enabled) SipService.start(context)
        }
    }

    fun setEnforceTlsSrtp(enabled: Boolean) {
        _enforceTlsSrtp.value = enabled
        viewModelScope.launch {
            settingsRepository.setSetting(SipManager.KEY_ENFORCE_TLS_SRTP, enabled.toString())
            sipManager.applyEncryptionPolicy(enabled)
            sipRepository.getAllAccounts().filter { it.enabled }.forEach { sipManager.addAccount(it) }
            _events.emit(if (enabled) "TLS + SRTP enforced for all SIP accounts" else "Standard SIP transport enabled")
        }
    }

    fun saveSipAccount(account: SipAccount) {
        viewModelScope.launch {
            val savedId = sipRepository.saveAccount(account)
            val saved = account.copy(id = if (account.id == 0L) savedId else account.id)
            if (saved.enabled) {
                SipService.start(context)
                sipManager.addAccount(saved)
            } else {
                sipManager.removeAccount(saved.id)
            }
            refreshLatency(saved.id)
            _events.emit("SIP account saved")
        }
    }

    fun deleteSipAccount(id: Long) {
        viewModelScope.launch {
            sipManager.removeAccount(id)
            sipRepository.deleteAccount(id)
            _latencies.update { it - id }
            _events.emit("SIP account removed")
        }
    }

    fun refreshLatency(accountId: Long) {
        viewModelScope.launch {
            val ms = sipManager.getServerLatency(accountId)
            _latencies.update { it + (accountId to ms) }
        }
    }

    fun refreshAllLatencies() {
        viewModelScope.launch {
            sipRepository.getAllAccounts().forEach { acc ->
                val ms = sipManager.getServerLatency(acc.id)
                _latencies.update { it + (acc.id to ms) }
            }
        }
    }

    fun setAppLockEnabled(enabled: Boolean): Boolean {
        if (enabled && !appLockManager.hasConfiguredPin()) {
            return false // UI should navigate to PinSetupScreen
        }
        appLockManager.setAppLockEnabled(enabled)
        _appLockEnabled.value = enabled
        return true
    }

    fun savePin(pin: String) {
        appLockManager.setPin(pin)
        _appLockEnabled.value = true
        viewModelScope.launch { _events.emit("6-digit App Lock PIN saved") }
    }

    fun setBiometricEnabled(enabled: Boolean) {
        appLockManager.setBiometricEnabled(enabled)
        _biometricEnabled.value = enabled
    }

    fun setAutoLockMinutes(minutes: Int) {
        appLockManager.setAutoLockMinutes(minutes)
        _autoLockMinutes.value = minutes
    }

    fun setBlockUnknown(enabled: Boolean) {
        _blockUnknown.value = enabled
        viewModelScope.launch { blockListManager.setBlockUnknownNumbers(enabled) }
    }

    fun addBlockedNumber(number: String, reason: String) {
        viewModelScope.launch {
            blockListManager.addBlockedNumber(number, reason)
            _events.emit("Number blocked")
        }
    }

    fun removeBlockedNumber(id: Long) {
        viewModelScope.launch { blockListManager.removeBlockedNumber(id) }
    }

    fun setSpeedDial(key: Int, number: String) {
        viewModelScope.launch {
            if (number.isBlank()) speedDialRepository.clearSpeedDial(key)
            else speedDialRepository.setSpeedDial(key, number)
            _events.emit("Speed dial #$key updated")
        }
    }

    fun setThemeColor(color: AppThemeColor) = securePrefsManager.setThemeColor(color)
    fun setDarkMode(option: DarkModeOption) = securePrefsManager.setDarkMode(option)
    fun setAmoledBlack(enabled: Boolean) = securePrefsManager.setAmoledBlack(enabled)

    fun setAutoBackupEnabled(enabled: Boolean): Boolean {
        if (enabled && securePrefsManager.getAutoBackupPassword().isNullOrBlank()) {
            return false
        }
        securePrefsManager.setAutoBackupEnabled(enabled)
        _autoBackupEnabled.value = enabled
        if (enabled) {
            backupScheduler.schedule(_backupFreqHours.value)
        } else {
            backupScheduler.cancel()
        }
        return true
    }

    fun enableAutoBackupWithPassword(password: String) {
        if (password.length < 6) return
        securePrefsManager.setAutoBackupPassword(password)
        securePrefsManager.setAutoBackupEnabled(true)
        _autoBackupEnabled.value = true
        backupScheduler.schedule(_backupFreqHours.value)
        viewModelScope.launch { _events.emit("Scheduled encrypted auto-backup enabled") }
    }

    fun setBackupProvider(provider: BackupProvider) {
        securePrefsManager.setBackupProvider(provider)
        _backupProvider.value = provider
    }

    fun setBackupFreqHours(hours: Int) {
        securePrefsManager.setBackupFrequencyHours(hours)
        _backupFreqHours.value = hours
        if (_autoBackupEnabled.value) {
            backupScheduler.schedule(hours)
        }
    }

    fun setCloudAccessToken(provider: BackupProvider, token: String) {
        when (provider) {
            BackupProvider.GOOGLE_DRIVE -> securePrefsManager.setGoogleDriveAccessToken(token.trim())
            BackupProvider.DROPBOX -> securePrefsManager.setDropboxAccessToken(token.trim())
            BackupProvider.LOCAL -> Unit
        }
        viewModelScope.launch { _events.emit("${provider.label} token saved securely") }
    }

    fun backupNow(password: String) {
        viewModelScope.launch {
            runCatching {
                val file = backupManager.createBackup(password)
                _lastBackupTimestamp.value = securePrefsManager.getLastBackupTimestamp()
                if (_autoBackupEnabled.value || securePrefsManager.getAutoBackupPassword().isNullOrBlank()) {
                    securePrefsManager.setAutoBackupPassword(password)
                }
                backupManager.uploadToConfiguredProvider(file).getOrThrow()
            }.onSuccess { msg ->
                _events.emit(msg)
            }.onFailure { err ->
                _events.emit(err.message ?: "Backup failed")
            }
        }
    }

    fun exportBackupToUri(uri: Uri, password: String) {
        viewModelScope.launch {
            runCatching {
                val file = backupManager.createBackup(password)
                _lastBackupTimestamp.value = securePrefsManager.getLastBackupTimestamp()
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    file.inputStream().use { input -> input.copyTo(out) }
                } ?: error("Unable to open destination file")
                file.name
            }.onSuccess { name ->
                _events.emit("Exported encrypted backup: $name")
            }.onFailure { err ->
                _events.emit(err.message ?: "Export failed")
            }
        }
    }

    fun restoreFromUri(uri: Uri, password: String) {
        viewModelScope.launch {
            runCatching {
                val temp = File(context.cacheDir, "restore_temp.callerx")
                context.contentResolver.openInputStream(uri)?.use { input ->
                    temp.outputStream().use { output -> input.copyTo(output) }
                } ?: error("Unable to read selected backup file")
                val res = backupManager.restoreBackup(temp, password).getOrThrow()
                temp.delete()
                res
            }.onSuccess { count ->
                _events.emit("Restored $count records from encrypted backup")
            }.onFailure { err ->
                _events.emit(err.message ?: "Restore failed — verify password and file")
            }
        }
    }

    companion object {
        const val KEY_CALL_WAITING = "call_waiting"
        const val KEY_CALL_FORWARDING = "call_forwarding_number"
        const val KEY_LOCKSCREEN_CALL_UI = "lockscreen_call_ui"
    }
}
