package com.example.ui.call

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PhoneForwarded
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.SwapCalls
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.model.CallState
import com.example.ui.components.RecordingConsentDialog
import com.example.ui.components.RouteBadge
import com.example.ui.components.SignalQualityBars
import com.example.ui.theme.SignalRed
import com.example.util.DurationUtils
import kotlinx.coroutines.delay

@Composable
fun CallScreen(
    viewModel: CallViewModel,
    onCallEnded: () -> Unit
) {
    val activeCall by viewModel.activeCall.collectAsStateWithLifecycle()
    val callState by viewModel.callState.collectAsStateWithLifecycle()
    val callStats by viewModel.callStats.collectAsStateWithLifecycle()
    val isMuted by viewModel.isMuted.collectAsStateWithLifecycle()
    val isSpeakerOn by viewModel.isSpeakerOn.collectAsStateWithLifecycle()
    val isBluetoothOn by viewModel.isBluetoothOn.collectAsStateWithLifecycle()
    val isHeld by viewModel.isHeld.collectAsStateWithLifecycle()
    val hasMultipleCalls by viewModel.hasMultipleCalls.collectAsStateWithLifecycle()
    val isRecording by viewModel.isRecording.collectAsStateWithLifecycle()
    val durationSeconds by viewModel.durationSeconds.collectAsStateWithLifecycle()
    val autoRecord by viewModel.autoRecordEnabled.collectAsStateWithLifecycle()
    val showConsent by viewModel.showConsentDialog.collectAsStateWithLifecycle()
    val networkLabel by viewModel.networkLabel.collectAsStateWithLifecycle()

    var showDtmfPad by remember { mutableStateOf(false) }
    var showAddCallDialog by remember { mutableStateOf(false) }
    var addCallNumber by remember { mutableStateOf("") }
    var showTransferDialog by remember { mutableStateOf(false) }
    var transferNumber by remember { mutableStateOf("") }

    LaunchedEffect(callState) {
        if (callState == CallState.ACTIVE) {
            viewModel.maybeStartAutoRecording()
        } else if (callState == CallState.ENDED) {
            delay(600L)
            onCallEnded()
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            viewModel.tickDuration()
            delay(1_000L)
        }
    }

    if (showConsent) {
        RecordingConsentDialog(
            onAccept = viewModel::acceptConsentAndRecord,
            onDecline = viewModel::dismissConsentDialog
        )
    }

    if (showAddCallDialog) {
        AlertDialog(
            onDismissRequest = { showAddCallDialog = false },
            title = { Text("Add Call to Conference") },
            text = {
                OutlinedTextField(
                    value = addCallNumber,
                    onValueChange = { addCallNumber = it },
                    label = { Text("Number or SIP URI") },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (addCallNumber.isNotBlank()) {
                        viewModel.addCall(addCallNumber.trim())
                        addCallNumber = ""
                        showAddCallDialog = false
                    }
                }) { Text("Add") }
            },
            dismissButton = {
                TextButton(onClick = { showAddCallDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showTransferDialog) {
        AlertDialog(
            onDismissRequest = { showTransferDialog = false },
            title = { Text("Transfer Call") },
            text = {
                OutlinedTextField(
                    value = transferNumber,
                    onValueChange = { transferNumber = it },
                    label = { Text("Destination Number or SIP URI") },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (transferNumber.isNotBlank()) {
                        viewModel.transferCall(transferNumber.trim())
                        transferNumber = ""
                        showTransferDialog = false
                    }
                }) { Text("Transfer") }
            },
            dismissButton = {
                TextButton(onClick = { showTransferDialog = false }) { Text("Cancel") }
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF0A1628), Color(0xFF050A14))
                )
            )
            .safeDrawingPadding()
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top bar: network chip, quality bars, auto-record toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = networkLabel.displayName,
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                    SignalQualityBars(bars = callStats.qualityBars)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Auto-Rec",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.78f)
                    )
                    Spacer(Modifier.width(6.dp))
                    Switch(
                        checked = autoRecord,
                        onCheckedChange = viewModel::toggleAutoRecordInCall
                    )
                }
            }

            // Center: Avatar, Caller Info, Route Badge, Status & Timer
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.10f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Caller",
                        tint = Color.White,
                        modifier = Modifier.size(44.dp)
                    )
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    text = activeCall.remoteName?.takeIf { it.isNotBlank() }
                        ?: activeCall.remoteNumber.ifBlank { "Unknown" },
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White
                )
                if (!activeCall.remoteName.isNullOrBlank()) {
                    Text(
                        text = activeCall.remoteNumber,
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
                Spacer(Modifier.height(10.dp))
                RouteBadge(
                    label = activeCall.routeLabel.ifBlank { if (activeCall.isSip) "SIP" else "SIM" },
                    isSip = activeCall.isSip
                )
                Spacer(Modifier.height(14.dp))
                Text(
                    text = when (callState) {
                        CallState.DIALING, CallState.IDLE -> "Calling..."
                        CallState.RINGING -> "Ringing..."
                        CallState.ACTIVE -> "Connected"
                        CallState.HELD -> "On Hold"
                        CallState.ENDED -> "Call Ended"
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color(0xFF7CC4FF)
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = DurationUtils.formatMmSs(durationSeconds),
                    style = MaterialTheme.typography.displayMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Medium
                )
            }

            // Optional inline DTMF keypad
            if (showDtmfPad) {
                val keys = listOf(
                    listOf('1', '2', '3'),
                    listOf('4', '5', '6'),
                    listOf('7', '8', '9'),
                    listOf('*', '0', '#')
                )
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    keys.forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                            row.forEach { ch ->
                                Surface(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(CircleShape)
                                        .clickable { viewModel.sendDtmf(ch) },
                                    color = Color.White.copy(alpha = 0.14f),
                                    shape = CircleShape
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = ch.toString(),
                                            style = MaterialTheme.typography.titleLarge,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 2x3 Call Controls + End Call Button
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        CallActionTile(
                            icon = Icons.Default.MicOff,
                            label = "Mute",
                            active = isMuted,
                            onClick = viewModel::toggleMute
                        )
                        CallActionTile(
                            icon = Icons.AutoMirrored.Filled.VolumeUp,
                            label = "Speaker",
                            active = isSpeakerOn,
                            onClick = viewModel::toggleSpeaker
                        )
                        CallActionTile(
                            icon = Icons.Default.Bluetooth,
                            label = "Bluetooth",
                            active = isBluetoothOn,
                            onClick = viewModel::toggleBluetooth
                        )
                        CallActionTile(
                            icon = Icons.Default.Pause,
                            label = "Hold",
                            active = isHeld,
                            onClick = viewModel::toggleHold
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        CallActionTile(
                            icon = Icons.Default.Dialpad,
                            label = "Keypad",
                            active = showDtmfPad,
                            onClick = { showDtmfPad = !showDtmfPad }
                        )
                        CallActionTile(
                            icon = Icons.Default.PersonAdd,
                            label = "Add Call",
                            active = false,
                            onClick = { showAddCallDialog = true }
                        )
                        CallActionTile(
                            icon = if (hasMultipleCalls) Icons.Default.SwapCalls else Icons.AutoMirrored.Filled.PhoneForwarded,
                            label = if (hasMultipleCalls) "Swap" else "Transfer",
                            active = false,
                            onClick = {
                                if (hasMultipleCalls) viewModel.swapCalls()
                                else showTransferDialog = true
                            }
                        )
                        CallActionTile(
                            icon = Icons.Default.FiberManualRecord,
                            label = if (isRecording) "Recording" else "Record",
                            active = isRecording,
                            activeColor = SignalRed,
                            onClick = viewModel::onRecordButtonPressed
                        )
                    }
                }

                FloatingActionButton(
                    onClick = {
                        viewModel.hangup()
                        onCallEnded()
                    },
                    containerColor = SignalRed,
                    contentColor = Color.White,
                    shape = CircleShape,
                    modifier = Modifier
                        .size(72.dp)
                        .testTag("end_call_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.CallEnd,
                        contentDescription = "End Call",
                        modifier = Modifier.size(34.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun CallActionTile(
    icon: ImageVector,
    label: String,
    active: Boolean,
    activeColor: Color = Color(0xFF00E5FF),
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(
                    if (active) activeColor.copy(alpha = 0.25f)
                    else Color.White.copy(alpha = 0.12f)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (active) activeColor else Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.88f),
            maxLines = 1
        )
    }
}
