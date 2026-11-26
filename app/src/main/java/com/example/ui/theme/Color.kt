package com.example.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import com.example.domain.model.AppThemeColor

val SignalGreen = Color(0xFF00C853)
val SignalAmber = Color(0xFFFFB300)
val SignalRed = Color(0xFFFF3D00)
val SignalCyan = Color(0xFF00E5FF)

fun colorSchemeFor(
    themeColor: AppThemeColor,
    darkTheme: Boolean,
    amoledBlack: Boolean
): ColorScheme {
    val base = when (themeColor) {
        AppThemeColor.BLUE -> if (darkTheme) {
            darkColorScheme(
                primary = Color(0xFF7CC4FF),
                onPrimary = Color(0xFF003258),
                primaryContainer = Color(0xFF00497D),
                onPrimaryContainer = Color(0xFFD1E4FF),
                secondary = Color(0xFF64DFDF),
                onSecondary = Color(0xFF003738),
                tertiary = Color(0xFF80E27E),
                background = Color(0xFF0B1220),
                surface = Color(0xFF10192A),
                surfaceVariant = Color(0xFF1A263C),
                onBackground = Color(0xFFE6EDF7),
                onSurface = Color(0xFFE6EDF7)
            )
        } else {
            lightColorScheme(
                primary = Color(0xFF1565C0),
                onPrimary = Color.White,
                primaryContainer = Color(0xFFD6E6FF),
                onPrimaryContainer = Color(0xFF001D3D),
                secondary = Color(0xFF00838F),
                onSecondary = Color.White,
                tertiary = Color(0xFF00897B),
                background = Color(0xFFF6F9FC),
                surface = Color.White,
                surfaceVariant = Color(0xFFE8EFF7)
            )
        }

        AppThemeColor.PURPLE -> if (darkTheme) {
            darkColorScheme(
                primary = Color(0xFFD8B4FE),
                onPrimary = Color(0xFF3B0764),
                primaryContainer = Color(0xFF581C87),
                onPrimaryContainer = Color(0xFFF3E8FF),
                secondary = Color(0xFF67E8F9),
                background = Color(0xFF100B1C),
                surface = Color(0xFF181128),
                surfaceVariant = Color(0xFF251B3B)
            )
        } else {
            lightColorScheme(
                primary = Color(0xFF6A1B9A),
                onPrimary = Color.White,
                primaryContainer = Color(0xFFF3E5F5),
                onPrimaryContainer = Color(0xFF2E004E),
                secondary = Color(0xFF00838F),
                background = Color(0xFFFAF7FD),
                surface = Color.White,
                surfaceVariant = Color(0xFFEEE6F7)
            )
        }

        AppThemeColor.GREEN -> if (darkTheme) {
            darkColorScheme(
                primary = Color(0xFF4ADE80),
                onPrimary = Color(0xFF003919),
                primaryContainer = Color(0xFF005227),
                onPrimaryContainer = Color(0xFFBBF7D0),
                secondary = Color(0xFF38BDF8),
                background = Color(0xFF081410),
                surface = Color(0xFF0E1E18),
                surfaceVariant = Color(0xFF162D24)
            )
        } else {
            lightColorScheme(
                primary = Color(0xFF00897B),
                onPrimary = Color.White,
                primaryContainer = Color(0xFFC8F7EE),
                onPrimaryContainer = Color(0xFF00201C),
                secondary = Color(0xFF0288D1),
                background = Color(0xFFF4FBF8),
                surface = Color.White,
                surfaceVariant = Color(0xFFE0F2EC)
            )
        }

        AppThemeColor.ORANGE -> if (darkTheme) {
            darkColorScheme(
                primary = Color(0xFFFFB74D),
                onPrimary = Color(0xFF4A2500),
                primaryContainer = Color(0xFF6A3800),
                onPrimaryContainer = Color(0xFFFFE0B2),
                secondary = Color(0xFF4DD0E1),
                background = Color(0xFF16100A),
                surface = Color(0xFF20170F),
                surfaceVariant = Color(0xFF2E2116)
            )
        } else {
            lightColorScheme(
                primary = Color(0xFFEF6C00),
                onPrimary = Color.White,
                primaryContainer = Color(0xFFFFE0B2),
                onPrimaryContainer = Color(0xFF3E1D00),
                secondary = Color(0xFF00838F),
                background = Color(0xFFFFF9F4),
                surface = Color.White,
                surfaceVariant = Color(0xFFFAEBDC)
            )
        }

        AppThemeColor.RED -> if (darkTheme) {
            darkColorScheme(
                primary = Color(0xFFFF8A80),
                onPrimary = Color(0xFF5F0007),
                primaryContainer = Color(0xFF8E0012),
                onPrimaryContainer = Color(0xFFFFDAD6),
                secondary = Color(0xFF80DEEA),
                background = Color(0xFF170B0D),
                surface = Color(0xFF211114),
                surfaceVariant = Color(0xFF31191E)
            )
        } else {
            lightColorScheme(
                primary = Color(0xFFC62828),
                onPrimary = Color.White,
                primaryContainer = Color(0xFFFFCDD2),
                onPrimaryContainer = Color(0xFF410002),
                secondary = Color(0xFF00838F),
                background = Color(0xFFFFF6F6),
                surface = Color.White,
                surfaceVariant = Color(0xFFFCE4E6)
            )
        }

        AppThemeColor.TEAL -> if (darkTheme) {
            darkColorScheme(
                primary = Color(0xFF26C6DA),
                onPrimary = Color(0xFF00363D),
                primaryContainer = Color(0xFF004F58),
                onPrimaryContainer = Color(0xFFB2EBF2),
                secondary = Color(0xFF69F0AE),
                background = Color(0xFF071316),
                surface = Color(0xFF0D1D22),
                surfaceVariant = Color(0xFF142A31)
            )
        } else {
            lightColorScheme(
                primary = Color(0xFF00838F),
                onPrimary = Color.White,
                primaryContainer = Color(0xFFB2EBF2),
                onPrimaryContainer = Color(0xFF002024),
                secondary = Color(0xFF00897B),
                background = Color(0xFFF3FAFB),
                surface = Color.White,
                surfaceVariant = Color(0xFFDFF1F4)
            )
        }

        AppThemeColor.PINK -> if (darkTheme) {
            darkColorScheme(
                primary = Color(0xFFF48FB1),
                onPrimary = Color(0xFF560027),
                primaryContainer = Color(0xFF880E4F),
                onPrimaryContainer = Color(0xFFFCE4EC),
                secondary = Color(0xFF80DEEA),
                background = Color(0xFF170B12),
                surface = Color(0xFF22101A),
                surfaceVariant = Color(0xFF331827)
            )
        } else {
            lightColorScheme(
                primary = Color(0xFFAD1457),
                onPrimary = Color.White,
                primaryContainer = Color(0xFFF8BBD0),
                onPrimaryContainer = Color(0xFF3E001D),
                secondary = Color(0xFF00838F),
                background = Color(0xFFFFF5F9),
                surface = Color.White,
                surfaceVariant = Color(0xFFFAE3ED)
            )
        }

        AppThemeColor.INDIGO -> if (darkTheme) {
            darkColorScheme(
                primary = Color(0xFF9FA8DA),
                onPrimary = Color(0xFF1A237E),
                primaryContainer = Color(0xFF283593),
                onPrimaryContainer = Color(0xFFE8EAF6),
                secondary = Color(0xFF4DD0E1),
                background = Color(0xFF0A0D1C),
                surface = Color(0xFF11162B),
                surfaceVariant = Color(0xFF1A213E)
            )
        } else {
            lightColorScheme(
                primary = Color(0xFF283593),
                onPrimary = Color.White,
                primaryContainer = Color(0xFFC5CAE9),
                onPrimaryContainer = Color(0xFF000C5C),
                secondary = Color(0xFF00838F),
                background = Color(0xFFF5F6FC),
                surface = Color.White,
                surfaceVariant = Color(0xFFE4E7F6)
            )
        }

        AppThemeColor.AMBER -> if (darkTheme) {
            darkColorScheme(
                primary = Color(0xFFFFD54F),
                onPrimary = Color(0xFF3F2E00),
                primaryContainer = Color(0xFF5D4300),
                onPrimaryContainer = Color(0xFFFFECB3),
                secondary = Color(0xFF4DD0E1),
                background = Color(0xFF161208),
                surface = Color(0xFF201A0C),
                surfaceVariant = Color(0xFF2E2512)
            )
        } else {
            lightColorScheme(
                primary = Color(0xFFF9A825),
                onPrimary = Color(0xFF221A00),
                primaryContainer = Color(0xFFFFECB3),
                onPrimaryContainer = Color(0xFF2A2000),
                secondary = Color(0xFF00838F),
                background = Color(0xFFFFFBF2),
                surface = Color.White,
                surfaceVariant = Color(0xFFFAF1D8)
            )
        }
    }

    return if (darkTheme && amoledBlack) {
        base.copy(
            background = Color.Black,
            surface = Color.Black,
            surfaceContainerLowest = Color.Black,
            surfaceContainerLow = Color(0xFF080808),
            surfaceContainer = Color(0xFF0D0D0D),
            surfaceContainerHigh = Color(0xFF141414),
            surfaceContainerHighest = Color(0xFF1C1C1C)
        )
    } else {
        base
    }
}
