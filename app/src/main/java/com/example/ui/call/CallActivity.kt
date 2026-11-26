package com.example.ui.call

import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.CallerXTheme
import com.example.ui.theme.ThemeViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CallActivity : ComponentActivity() {

    private val callViewModel: CallViewModel by viewModels()
    private val themeViewModel: ThemeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        configureLockscreenFlags()

        setContent {
            val color by themeViewModel.themeColor.collectAsStateWithLifecycle()
            val darkMode by themeViewModel.darkMode.collectAsStateWithLifecycle()
            val amoled by themeViewModel.amoledBlack.collectAsStateWithLifecycle()

            CallerXTheme(themeColor = color, darkMode = darkMode, amoledBlack = amoled) {
                CallScreen(
                    viewModel = callViewModel,
                    onCallEnded = { finish() }
                )
            }
        }
    }

    private fun configureLockscreenFlags() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }
    }
}
