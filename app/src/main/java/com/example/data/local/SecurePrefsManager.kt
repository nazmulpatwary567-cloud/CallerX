package com.example.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.example.domain.model.AppThemeColor
import com.example.domain.model.BackupProvider
import com.example.domain.model.DarkModeOption
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SecurePrefsManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences by lazy {
        try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            EncryptedSharedPreferences.create(
                context,
                "callerx_secure_prefs",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (_: Throwable) {
            context.getSharedPreferences("callerx_secure_prefs_fallback", Context.MODE_PRIVATE)
        }
    }

    private val _themeColor = MutableStateFlow(loadThemeColor())
    val themeColor: StateFlow<AppThemeColor> = _themeColor.asStateFlow()

    private val _darkMode = MutableStateFlow(loadDarkMode())
    val darkMode: StateFlow<DarkModeOption> = _darkMode.asStateFlow()

    private val _amoledBlack = MutableStateFlow(prefs.getBoolean(KEY_AMOLED_BLACK, false))
    val amoledBlack: StateFlow<Boolean> = _amoledBlack.asStateFlow()

    fun setThemeColor(color: AppThemeColor) {
        prefs.edit().putString(KEY_THEME_COLOR, color.name).apply()
        _themeColor.value = color
    }

    fun setDarkMode(option: DarkModeOption) {
        prefs.edit().putString(KEY_DARK_MODE, option.name).apply()
        _darkMode.value = option
    }

    fun setAmoledBlack(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AMOLED_BLACK, enabled).apply()
        _amoledBlack.value = enabled
    }

    private fun loadThemeColor(): AppThemeColor {
        val saved = prefs.getString(KEY_THEME_COLOR, AppThemeColor.BLUE.name)
        return runCatching { AppThemeColor.valueOf(saved ?: AppThemeColor.BLUE.name) }
            .getOrDefault(AppThemeColor.BLUE)
    }

    private fun loadDarkMode(): DarkModeOption {
        val saved = prefs.getString(KEY_DARK_MODE, DarkModeOption.SYSTEM.name)
        return runCatching { DarkModeOption.valueOf(saved ?: DarkModeOption.SYSTEM.name) }
            .getOrDefault(DarkModeOption.SYSTEM)
    }

    // Recording Consent
    fun hasRecordingConsent(): Boolean = prefs.getBoolean(KEY_RECORDING_CONSENT, false)
    fun setRecordingConsent(accepted: Boolean) {
        prefs.edit().putBoolean(KEY_RECORDING_CONSENT, accepted).apply()
    }

    // App Lock PIN & Biometrics
    fun isAppLockEnabled(): Boolean = prefs.getBoolean(KEY_APP_LOCK_ENABLED, false)
    fun setAppLockEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_APP_LOCK_ENABLED, enabled).apply()
    }

    fun isBiometricEnabled(): Boolean = prefs.getBoolean(KEY_BIOMETRIC_ENABLED, false)
    fun setBiometricEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_BIOMETRIC_ENABLED, enabled).apply()
    }

    fun getPinHash(): String? = prefs.getString(KEY_PIN_HASH, null)
    fun getPinSalt(): String? = prefs.getString(KEY_PIN_SALT, null)
    fun savePinCredentials(hash: String, salt: String) {
        prefs.edit()
            .putString(KEY_PIN_HASH, hash)
            .putString(KEY_PIN_SALT, salt)
            .putBoolean(KEY_APP_LOCK_ENABLED, true)
            .apply()
    }

    fun getAutoLockMinutes(): Int = prefs.getInt(KEY_AUTO_LOCK_MINUTES, 1)
    fun setAutoLockMinutes(minutes: Int) {
        prefs.edit().putInt(KEY_AUTO_LOCK_MINUTES, minutes.coerceIn(0, 60)).apply()
    }

    // Cloud Backup Tokens & Settings
    fun isAutoBackupEnabled(): Boolean = prefs.getBoolean(KEY_AUTO_BACKUP_ENABLED, false)
    fun setAutoBackupEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_BACKUP_ENABLED, enabled).apply()
    }

    fun getBackupProvider(): BackupProvider {
        val raw = prefs.getString(KEY_BACKUP_PROVIDER, BackupProvider.LOCAL.name)
        return runCatching { BackupProvider.valueOf(raw ?: BackupProvider.LOCAL.name) }
            .getOrDefault(BackupProvider.LOCAL)
    }

    fun setBackupProvider(provider: BackupProvider) {
        prefs.edit().putString(KEY_BACKUP_PROVIDER, provider.name).apply()
    }

    fun getBackupFrequencyHours(): Int = prefs.getInt(KEY_BACKUP_FREQ_HOURS, 24)
    fun setBackupFrequencyHours(hours: Int) {
        prefs.edit().putInt(KEY_BACKUP_FREQ_HOURS, hours).apply()
    }

    fun getGoogleDriveAccessToken(): String? = prefs.getString(KEY_DRIVE_TOKEN, null)
    fun setGoogleDriveAccessToken(token: String?) {
        prefs.edit().putString(KEY_DRIVE_TOKEN, token).apply()
    }

    fun getDropboxAccessToken(): String? = prefs.getString(KEY_DROPBOX_TOKEN, null)
    fun setDropboxAccessToken(token: String?) {
        prefs.edit().putString(KEY_DROPBOX_TOKEN, token).apply()
    }

    fun getLastBackupTimestamp(): Long = prefs.getLong(KEY_LAST_BACKUP_TS, 0L)
    fun setLastBackupTimestamp(ts: Long) {
        prefs.edit().putLong(KEY_LAST_BACKUP_TS, ts).apply()
    }

    fun getAutoBackupPassword(): String? = prefs.getString(KEY_AUTO_BACKUP_PASSWORD, null)
    fun setAutoBackupPassword(password: String?) {
        prefs.edit().putString(KEY_AUTO_BACKUP_PASSWORD, password).apply()
    }

    companion object {
        private const val KEY_THEME_COLOR = "theme_color"
        private const val KEY_DARK_MODE = "dark_mode"
        private const val KEY_AMOLED_BLACK = "amoled_black"
        private const val KEY_RECORDING_CONSENT = "recording_consent_accepted"
        private const val KEY_APP_LOCK_ENABLED = "app_lock_enabled"
        private const val KEY_BIOMETRIC_ENABLED = "biometric_enabled"
        private const val KEY_PIN_HASH = "pin_hash"
        private const val KEY_PIN_SALT = "pin_salt"
        private const val KEY_AUTO_LOCK_MINUTES = "auto_lock_minutes"
        private const val KEY_AUTO_BACKUP_ENABLED = "auto_backup_enabled"
        private const val KEY_BACKUP_PROVIDER = "backup_provider"
        private const val KEY_BACKUP_FREQ_HOURS = "backup_freq_hours"
        private const val KEY_DRIVE_TOKEN = "drive_access_token"
        private const val KEY_DROPBOX_TOKEN = "dropbox_access_token"
        private const val KEY_LAST_BACKUP_TS = "last_backup_ts"
        private const val KEY_AUTO_BACKUP_PASSWORD = "auto_backup_password"
    }
}
