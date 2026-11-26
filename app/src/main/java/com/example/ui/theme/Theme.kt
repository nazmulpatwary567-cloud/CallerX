package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import com.example.data.local.SecurePrefsManager
import com.example.domain.model.AppThemeColor
import com.example.domain.model.DarkModeOption
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

val CallerXShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@HiltViewModel
class ThemeViewModel @Inject constructor(
    private val securePrefsManager: SecurePrefsManager
) : ViewModel() {
    val themeColor: StateFlow<AppThemeColor> = securePrefsManager.themeColor
    val darkMode: StateFlow<DarkModeOption> = securePrefsManager.darkMode
    val amoledBlack: StateFlow<Boolean> = securePrefsManager.amoledBlack

    fun setThemeColor(color: AppThemeColor) = securePrefsManager.setThemeColor(color)
    fun setDarkMode(option: DarkModeOption) = securePrefsManager.setDarkMode(option)
    fun setAmoledBlack(enabled: Boolean) = securePrefsManager.setAmoledBlack(enabled)
}

@Composable
fun CallerXTheme(
    themeColor: AppThemeColor = AppThemeColor.BLUE,
    darkMode: DarkModeOption = DarkModeOption.SYSTEM,
    amoledBlack: Boolean = false,
    content: @Composable () -> Unit
) {
    val isDark = when (darkMode) {
        DarkModeOption.SYSTEM -> isSystemInDarkTheme()
        DarkModeOption.LIGHT -> false
        DarkModeOption.DARK -> true
    }

    MaterialTheme(
        colorScheme = colorSchemeFor(themeColor, isDark, amoledBlack),
        typography = CallerXTypography,
        shapes = CallerXShapes,
        content = content
    )
}
