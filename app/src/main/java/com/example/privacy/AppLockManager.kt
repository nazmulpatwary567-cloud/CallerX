package com.example.privacy

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.example.data.local.SecurePrefsManager
import com.example.util.CryptoUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppLockManager @Inject constructor(
    private val securePrefsManager: SecurePrefsManager
) {
    private val _isLocked = MutableStateFlow(
        securePrefsManager.isAppLockEnabled() && securePrefsManager.getPinHash() != null
    )
    val isLocked: StateFlow<Boolean> = _isLocked.asStateFlow()

    private var backgroundTimestampMs: Long = 0L

    fun isLockEnabled(): Boolean = securePrefsManager.isAppLockEnabled()
    fun hasConfiguredPin(): Boolean =
        securePrefsManager.getPinHash() != null && securePrefsManager.getPinSalt() != null

    fun isBiometricEnabled(): Boolean = securePrefsManager.isBiometricEnabled()
    fun getAutoLockMinutes(): Int = securePrefsManager.getAutoLockMinutes()

    fun setPin(pin: String) {
        require(pin.length == 6 && pin.all { it.isDigit() }) { "PIN must be 6 digits" }
        val salt = CryptoUtils.generateSaltBase64()
        val hash = CryptoUtils.hashPinSha256(pin, salt)
        securePrefsManager.savePinCredentials(hash, salt)
        _isLocked.value = false
    }

    fun setAppLockEnabled(enabled: Boolean) {
        securePrefsManager.setAppLockEnabled(enabled)
        if (!enabled) {
            _isLocked.value = false
        }
    }

    fun setBiometricEnabled(enabled: Boolean) {
        securePrefsManager.setBiometricEnabled(enabled)
    }

    fun setAutoLockMinutes(minutes: Int) {
        securePrefsManager.setAutoLockMinutes(minutes)
    }

    fun verifyPin(pin: String): Boolean {
        val hash = securePrefsManager.getPinHash() ?: return false
        val salt = securePrefsManager.getPinSalt() ?: return false
        val ok = CryptoUtils.verifyPinSha256(pin, salt, hash)
        if (ok) {
            _isLocked.value = false
        }
        return ok
    }

    fun lockNow() {
        if (isLockEnabled() && hasConfiguredPin()) {
            _isLocked.value = true
        }
    }

    fun unlockWithBiometric() {
        _isLocked.value = false
    }

    fun onAppBackgrounded() {
        backgroundTimestampMs = System.currentTimeMillis()
    }

    fun onAppForegrounded() {
        if (!isLockEnabled() || !hasConfiguredPin()) return
        if (backgroundTimestampMs == 0L) {
            _isLocked.value = true
            return
        }
        val elapsedMinutes = (System.currentTimeMillis() - backgroundTimestampMs) / 60_000L
        if (elapsedMinutes >= getAutoLockMinutes()) {
            _isLocked.value = true
        }
    }

    fun canUseBiometrics(context: Context): Boolean {
        val mgr = BiometricManager.from(context)
        return mgr.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK) ==
            BiometricManager.BIOMETRIC_SUCCESS
    }

    fun showBiometricPrompt(
        activity: FragmentActivity,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val executor = ContextCompat.getMainExecutor(activity)
        val prompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    _isLocked.value = false
                    onSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    onError(errString.toString())
                }
            }
        )
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock CallerX")
            .setSubtitle("Authenticate with biometrics to access your calls and messages")
            .setNegativeButtonText("Use PIN")
            .build()
        prompt.authenticate(info)
    }
}
