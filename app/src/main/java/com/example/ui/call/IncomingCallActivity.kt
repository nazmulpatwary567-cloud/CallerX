package com.example.ui.call

import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.model.CallState
import com.example.ui.components.RouteBadge
import com.example.ui.theme.CallerXTheme
import com.example.ui.theme.SignalGreen
import com.example.ui.theme.SignalRed
import com.example.ui.theme.ThemeViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class IncomingCallActivity : ComponentActivity() {

    private val callViewModel: CallViewModel by viewModels()
    private val themeViewModel: ThemeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }

        setContent {
            val color by themeViewModel.themeColor.collectAsStateWithLifecycle()
            val darkMode by themeViewModel.darkMode.collectAsStateWithLifecycle()
            val amoled by themeViewModel.amoledBlack.collectAsStateWithLifecycle()

            CallerXTheme(themeColor = color, darkMode = darkMode, amoledBlack = amoled) {
                IncomingCallScreen(
                    viewModel = callViewModel,
                    onClose = { finish() }
                )
            }
        }
    }
}

@Composable
fun IncomingCallScreen(
    viewModel: CallViewModel,
    onClose: () -> Unit
) {
    val activeCall by viewModel.activeCall.collectAsStateWithLifecycle()
    val callState by viewModel.callState.collectAsStateWithLifecycle()

    LaunchedEffect(callState) {
        if (callState == CallState.ENDED || callState == CallState.ACTIVE) {
            onClose()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF0A1D2E), Color(0xFF050910))
                )
            )
            .safeDrawingPadding()
            .padding(32.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(Modifier.height(24.dp))
                RouteBadge(
                    label = activeCall.routeLabel.ifBlank { "Incoming SIP Call" },
                    isSip = activeCall.isSip
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "Incoming Call",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color(0xFF7CC4FF)
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(104.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Incoming Caller",
                        tint = Color.White,
                        modifier = Modifier.size(52.dp)
                    )
                }
                Spacer(Modifier.height(20.dp))
                Text(
                    text = activeCall.remoteName?.takeIf { it.isNotBlank() }
                        ?: activeCall.remoteNumber.ifBlank { "Unknown Caller" },
                    style = MaterialTheme.typography.displayMedium,
                    color = Color.White
                )
                if (!activeCall.remoteName.isNullOrBlank()) {
                    Text(
                        text = activeCall.remoteNumber,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White.copy(alpha = 0.72f)
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    FloatingActionButton(
                        onClick = {
                            viewModel.declineIncoming()
                            onClose()
                        },
                        containerColor = SignalRed,
                        contentColor = Color.White,
                        shape = CircleShape,
                        modifier = Modifier
                            .size(72.dp)
                            .testTag("decline_call_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CallEnd,
                            contentDescription = "Decline Call",
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("Decline", color = Color.White)
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    FloatingActionButton(
                        onClick = {
                            viewModel.answerIncoming()
                            onClose()
                        },
                        containerColor = SignalGreen,
                        contentColor = Color.White,
                        shape = CircleShape,
                        modifier = Modifier
                            .size(72.dp)
                            .testTag("answer_call_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Call,
                            contentDescription = "Answer Call",
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("Answer", color = Color.White)
                }
            }
        }
    }
}
