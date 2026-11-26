package com.example.ui.components

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.SimCard
import androidx.compose.material.icons.filled.WifiCalling3
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.model.AccountStatus
import com.example.domain.model.CallRoute
import com.example.ui.theme.SignalAmber
import com.example.ui.theme.SignalGreen
import com.example.ui.theme.SignalRed

@Composable
fun StatusDot(status: AccountStatus, modifier: Modifier = Modifier) {
    val color = when (status) {
        AccountStatus.ONLINE -> SignalGreen
        AccountStatus.REGISTERING -> SignalAmber
        AccountStatus.OFFLINE, AccountStatus.ERROR -> SignalRed
    }
    Box(
        modifier = modifier
            .size(10.dp)
            .clip(CircleShape)
            .background(color)
    )
}

@Composable
fun RouteBadge(label: String, isSip: Boolean, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = if (isSip) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.primaryContainer
        }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = if (isSip) Icons.Default.WifiCalling3 else Icons.Default.SimCard,
                contentDescription = label,
                modifier = Modifier.size(12.dp),
                tint = if (isSip) {
                    MaterialTheme.colorScheme.onSecondaryContainer
                } else {
                    MaterialTheme.colorScheme.onPrimaryContainer
                }
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = if (isSip) {
                    MaterialTheme.colorScheme.onSecondaryContainer
                } else {
                    MaterialTheme.colorScheme.onPrimaryContainer
                }
            )
        }
    }
}

@Composable
fun SignalQualityBars(bars: Int, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        for (i in 1..5) {
            val active = i <= bars
            val barColor = when {
                !active -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                bars >= 4 -> SignalGreen
                bars >= 2 -> SignalAmber
                else -> SignalRed
            }
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height((6 + i * 3).dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(barColor)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CallRoutePicker(
    routes: List<CallRoute>,
    numberToCall: String,
    onDismiss: () -> Unit,
    onSelectRoute: (route: CallRoute, rememberAsDefault: Boolean) -> Unit
) {
    var selectedIndex by remember { mutableStateOf(0) }
    var rememberDefault by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("call_route_picker_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp)
        ) {
            Text(
                text = "Choose Call Route",
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                text = "Calling $numberToCall",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(16.dp))

            if (routes.isEmpty()) {
                Text(
                    text = "No active SIM cards or SIP accounts found. Add a SIP account in Settings or insert a SIM.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            } else {
                routes.forEachIndexed { idx, route ->
                    val label = when (route) {
                        is CallRoute.Sim -> route.label
                        is CallRoute.Sip -> "SIP · ${route.label}"
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { selectedIndex = idx }
                            .padding(vertical = 8.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedIndex == idx,
                            onClick = { selectedIndex = idx }
                        )
                        Spacer(Modifier.width(8.dp))
                        Icon(
                            imageVector = if (route is CallRoute.Sip) Icons.Default.WifiCalling3 else Icons.Default.SimCard,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(text = label, style = MaterialTheme.typography.bodyLarge)
                    }
                }

                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { rememberDefault = !rememberDefault }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = rememberDefault,
                        onCheckedChange = { rememberDefault = it }
                    )
                    Text(
                        text = "Remember as default route",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = {
                        routes.getOrNull(selectedIndex)?.let { route ->
                            onSelectRoute(route, rememberDefault)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("confirm_call_route_button")
                ) {
                    Icon(Icons.Default.Phone, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Place Call")
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
fun RecordingConsentDialog(
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDecline,
        title = { Text(stringResource(R.string.recording_consent_title)) },
        text = { Text(stringResource(R.string.recording_consent_message)) },
        confirmButton = {
            Button(
                onClick = onAccept,
                modifier = Modifier.testTag("accept_recording_consent_button")
            ) {
                Text("I Understand & Consent")
            }
        },
        dismissButton = {
            TextButton(onClick = onDecline) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun PermissionRationaleDialog(
    missingPermissions: List<String>,
    permissionLabelProvider: (String) -> String,
    onGrantClick: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Permissions Required") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    "CallerX needs these permissions for dual-SIM calling, VoIP audio, T9 contact search, and SMS:",
                    style = MaterialTheme.typography.bodyMedium
                )
                missingPermissions.take(6).forEach { perm ->
                    Text(
                        text = "• ${permissionLabelProvider(perm)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onGrantClick,
                modifier = Modifier.testTag("grant_permissions_button")
            ) {
                Text("Grant Permissions")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Later")
            }
        }
    )
}

@Composable
fun AppLockGateScreen(
    biometricAvailable: Boolean,
    onPinVerified: (String) -> Boolean,
    onRequestBiometric: () -> Unit
) {
    var enteredPin by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf<String?>(null) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "App Locked",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp)
                )
            }
            Spacer(Modifier.height(16.dp))
            Text("CallerX Locked", style = MaterialTheme.typography.headlineMedium)
            Text(
                text = "Enter your 6-digit privacy PIN",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(24.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                repeat(6) { idx ->
                    val filled = idx < enteredPin.length
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(
                                if (filled) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                    )
                }
            }

            if (errorText != null) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = errorText!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(Modifier.height(28.dp))
            val rows = listOf(
                listOf("1", "2", "3"),
                listOf("4", "5", "6"),
                listOf("7", "8", "9"),
                listOf("BIO", "0", "DEL")
            )
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                rows.forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                        row.forEach { key ->
                            when (key) {
                                "BIO" -> {
                                    if (biometricAvailable) {
                                        IconButton(
                                            onClick = onRequestBiometric,
                                            modifier = Modifier.size(68.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Fingerprint,
                                                contentDescription = "Unlock with Biometrics",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(32.dp)
                                            )
                                        }
                                    } else {
                                        Spacer(Modifier.size(68.dp))
                                    }
                                }

                                "DEL" -> {
                                    IconButton(
                                        onClick = {
                                            if (enteredPin.isNotEmpty()) {
                                                enteredPin = enteredPin.dropLast(1)
                                                errorText = null
                                            }
                                        },
                                        modifier = Modifier.size(68.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.Backspace,
                                            contentDescription = "Backspace"
                                        )
                                    }
                                }

                                else -> {
                                    Surface(
                                        modifier = Modifier
                                            .size(68.dp)
                                            .clip(CircleShape)
                                            .clickable {
                                                if (enteredPin.length < 6) {
                                                    val next = enteredPin + key
                                                    enteredPin = next
                                                    errorText = null
                                                    if (next.length == 6) {
                                                        val ok = onPinVerified(next)
                                                        if (!ok) {
                                                            errorText = "Incorrect PIN. Try again."
                                                            enteredPin = ""
                                                        }
                                                    }
                                                }
                                            },
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.surfaceVariant
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = key,
                                                style = MaterialTheme.typography.titleLarge,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            if (biometricAvailable) {
                Spacer(Modifier.height(16.dp))
                OutlinedButton(onClick = onRequestBiometric) {
                    Icon(Icons.Default.Fingerprint, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Use Biometrics")
                }
            }
        }
    }
}
