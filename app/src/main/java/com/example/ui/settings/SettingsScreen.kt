package com.example.ui.settings

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.PhoneForwarded
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.WifiCalling3
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.BuildConfig
import com.example.R
import com.example.domain.model.AccountStatus
import com.example.domain.model.AppThemeColor
import com.example.domain.model.BackupProvider
import com.example.domain.model.DarkModeOption
import com.example.ui.components.RecordingConsentDialog
import com.example.ui.components.StatusDot
import com.example.util.DateUtils

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    onOpenSipAccounts: () -> Unit,
    onOpenBlockList: () -> Unit,
    onOpenPinSetup: () -> Unit,
    onOpenRecordings: () -> Unit,
    onOpenSmsInbox: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val sipAccounts by viewModel.sipAccounts.collectAsStateWithLifecycle()
    val sipStatuses by viewModel.sipStatuses.collectAsStateWithLifecycle()
    val defaultRoute by viewModel.defaultRoute.collectAsStateWithLifecycle()
    val autoRecord by viewModel.autoRecord.collectAsStateWithLifecycle()
    val callWaiting by viewModel.callWaiting.collectAsStateWithLifecycle()
    val callForwarding by viewModel.callForwarding.collectAsStateWithLifecycle()
    val lockscreenCallUi by viewModel.lockscreenCallUi.collectAsStateWithLifecycle()
    val backgroundSip by viewModel.backgroundSip.collectAsStateWithLifecycle()
    val appLockEnabled by viewModel.appLockEnabled.collectAsStateWithLifecycle()
    val blockUnknown by viewModel.blockUnknown.collectAsStateWithLifecycle()
    val themeColor by viewModel.themeColor.collectAsStateWithLifecycle()
    val darkMode by viewModel.darkMode.collectAsStateWithLifecycle()
    val amoledBlack by viewModel.amoledBlack.collectAsStateWithLifecycle()
    val autoBackupEnabled by viewModel.autoBackupEnabled.collectAsStateWithLifecycle()
    val backupProvider by viewModel.backupProvider.collectAsStateWithLifecycle()
    val backupFreqHours by viewModel.backupFreqHours.collectAsStateWithLifecycle()
    val lastBackupTimestamp by viewModel.lastBackupTimestamp.collectAsStateWithLifecycle()
    val activeSims by viewModel.activeSims.collectAsStateWithLifecycle()
    val showConsent by viewModel.showRecordingConsent.collectAsStateWithLifecycle()
    val speedDials by viewModel.speedDials.collectAsStateWithLifecycle()

    val context = LocalContext.current

    var showForwardDialog by remember { mutableStateOf(false) }
    var forwardDraft by remember(callForwarding) { mutableStateOf(callForwarding) }
    var showBackupPasswordDialog by remember { mutableStateOf(false) }
    var backupPassword by remember { mutableStateOf("") }
    var pendingExportUri by remember { mutableStateOf<Uri?>(null) }
    var showAutoBackupPasswordDialog by remember { mutableStateOf(false) }
    var autoBackupPasswordDraft by remember { mutableStateOf("") }
    var pendingRestoreUri by remember { mutableStateOf<Uri?>(null) }
    var restorePassword by remember { mutableStateOf("") }
    var showTokenDialog by remember { mutableStateOf(false) }
    var tokenDraft by remember { mutableStateOf("") }
    var showSpeedDialDialog by remember { mutableStateOf(false) }
    var selectedSpeedKey by remember { mutableStateOf(2) }
    var speedNumberDraft by remember { mutableStateOf("") }

    val restoreLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            pendingRestoreUri = uri
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        if (uri != null) {
            pendingExportUri = uri
            showBackupPasswordDialog = true
        }
    }

    LaunchedEffect(Unit) {
        viewModel.loadSettings()
        viewModel.events.collect { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        }
    }

    if (showConsent) {
        RecordingConsentDialog(
            onAccept = viewModel::acceptRecordingConsent,
            onDecline = viewModel::dismissRecordingConsent
        )
    }

    if (showForwardDialog) {
        AlertDialog(
            onDismissRequest = { showForwardDialog = false },
            title = { Text("Call Forwarding Number") },
            text = {
                OutlinedTextField(
                    value = forwardDraft,
                    onValueChange = { forwardDraft = it },
                    label = { Text("Forward to number (blank to disable)") },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.setCallForwarding(forwardDraft)
                    showForwardDialog = false
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showForwardDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showBackupPasswordDialog) {
        AlertDialog(
            onDismissRequest = {
                pendingExportUri = null
                showBackupPasswordDialog = false
            },
            title = { Text(if (pendingExportUri != null) "Export Backup (.callerx)" else "Encrypt Backup (.callerx)") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Protected with AES-256-GCM + PBKDF2 (100,000 iterations).",
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = backupPassword,
                        onValueChange = { backupPassword = it },
                        label = { Text("Backup Password (min 6 chars)") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (backupPassword.length >= 6) {
                        val exportTarget = pendingExportUri
                        if (exportTarget != null) {
                            viewModel.exportBackupToUri(exportTarget, backupPassword)
                        } else {
                            viewModel.backupNow(backupPassword)
                        }
                        backupPassword = ""
                        pendingExportUri = null
                        showBackupPasswordDialog = false
                    }
                }) { Text(if (pendingExportUri != null) "Export" else "Create Backup") }
            },
            dismissButton = {
                TextButton(onClick = {
                    pendingExportUri = null
                    showBackupPasswordDialog = false
                }) { Text("Cancel") }
            }
        )
    }

    if (showAutoBackupPasswordDialog) {
        AlertDialog(
            onDismissRequest = { showAutoBackupPasswordDialog = false },
            title = { Text("Enable Scheduled Auto-Backup") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Set an encryption passphrase stored inside Android Keystore for scheduled backups.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = autoBackupPasswordDraft,
                        onValueChange = { autoBackupPasswordDraft = it },
                        label = { Text("Encryption Passphrase (min 6 chars)") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (autoBackupPasswordDraft.length >= 6) {
                        viewModel.enableAutoBackupWithPassword(autoBackupPasswordDraft)
                        autoBackupPasswordDraft = ""
                        showAutoBackupPasswordDialog = false
                    }
                }) { Text("Enable") }
            },
            dismissButton = {
                TextButton(onClick = { showAutoBackupPasswordDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (pendingRestoreUri != null) {
        AlertDialog(
            onDismissRequest = { pendingRestoreUri = null },
            title = { Text("Decrypt & Restore Backup") },
            text = {
                OutlinedTextField(
                    value = restorePassword,
                    onValueChange = { restorePassword = it },
                    label = { Text("Backup Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true
                )
            },
            confirmButton = {
                Button(onClick = {
                    pendingRestoreUri?.let { viewModel.restoreFromUri(it, restorePassword) }
                    restorePassword = ""
                    pendingRestoreUri = null
                }) { Text("Restore") }
            },
            dismissButton = {
                TextButton(onClick = { pendingRestoreUri = null }) { Text("Cancel") }
            }
        )
    }

    if (showTokenDialog) {
        AlertDialog(
            onDismissRequest = { showTokenDialog = false },
            title = { Text("${backupProvider.label} OAuth Token") },
            text = {
                OutlinedTextField(
                    value = tokenDraft,
                    onValueChange = { tokenDraft = it },
                    label = { Text("Bearer Access Token") },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.setCloudAccessToken(backupProvider, tokenDraft)
                    tokenDraft = ""
                    showTokenDialog = false
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showTokenDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showSpeedDialDialog) {
        AlertDialog(
            onDismissRequest = { showSpeedDialDialog = false },
            title = { Text("Speed Dial Keys (2-9)") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        (2..9).forEach { k ->
                            FilterChip(
                                selected = selectedSpeedKey == k,
                                onClick = {
                                    selectedSpeedKey = k
                                    speedNumberDraft = speedDials.firstOrNull { it.key == k }?.number.orEmpty()
                                },
                                label = { Text("#$k") }
                            )
                        }
                    }
                    OutlinedTextField(
                        value = speedNumberDraft,
                        onValueChange = { speedNumberDraft = it },
                        label = { Text("Phone number for key #$selectedSpeedKey") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.setSpeedDial(selectedSpeedKey, speedNumberDraft)
                    showSpeedDialDialog = false
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showSpeedDialDialog = false }) { Text("Close") }
            }
        )
    }

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 680.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Settings", style = MaterialTheme.typography.headlineMedium)

            // Privacy Hero Card with generated illustration
            Card(
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(modifier = Modifier.height(132.dp)) {
                    Image(
                        painter = painterResource(id = R.drawable.img_privacy_shield_banner),
                        contentDescription = "CallerX Security & Privacy Shield",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.horizontalGradient(
                                    listOf(Color(0xD907101C), Color(0x5507101C))
                                )
                            )
                            .padding(18.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Column {
                            Text(
                                text = "Zero-Telemetry Privacy",
                                style = MaterialTheme.typography.titleLarge,
                                color = Color.White
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "Encrypted local storage, SRTP/TLS VoIP, and AES-256 backups.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                        }
                    }
                }
            }

            // Quick Tools Row (Recordings + SMS)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onOpenRecordings,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.GraphicEq, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Recordings")
                }
                OutlinedButton(
                    onClick = onOpenSmsInbox,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Sms, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("SMS Inbox")
                }
            }

            // Section: Calling
            SettingsSectionCard(title = "Calling") {
                Text("Default Call Route", style = MaterialTheme.typography.labelLarge)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(vertical = 6.dp)
                ) {
                    FilterChip(
                        selected = defaultRoute.isBlank(),
                        onClick = { viewModel.setDefaultRoute("") },
                        label = { Text("Ask Every Time") }
                    )
                    activeSims.forEach { sim ->
                        val token = "SIM:${sim.subscriptionId}"
                        FilterChip(
                            selected = defaultRoute == token,
                            onClick = { viewModel.setDefaultRoute(token) },
                            label = { Text("SIM ${sim.slotIndex + 1} · ${sim.carrierName}") }
                        )
                    }
                    sipAccounts.filter { it.enabled }.forEach { acc ->
                        val token = "SIP:${acc.id}"
                        FilterChip(
                            selected = defaultRoute == token,
                            onClick = { viewModel.setDefaultRoute(token) },
                            label = { Text("SIP · ${acc.displayName.ifBlank { acc.username }}") }
                        )
                    }
                }

                SettingsToggleRow("Auto Call Recording", autoRecord, viewModel::onAutoRecordChanged)
                SettingsToggleRow("Call Waiting", callWaiting, viewModel::setCallWaiting)
                SettingsToggleRow("Lock Screen Call UI", lockscreenCallUi, viewModel::setLockscreenCallUi)
                SettingsToggleRow("Background SIP Service", backgroundSip, viewModel::setBackgroundSip)

                SettingsActionRow(
                    icon = Icons.AutoMirrored.Filled.PhoneForwarded,
                    title = "Call Forwarding",
                    subtitle = callForwarding.ifBlank { "Disabled" },
                    onClick = { showForwardDialog = true }
                )
                SettingsActionRow(
                    icon = Icons.Default.Speed,
                    title = "Speed Dials (Keys 2–9)",
                    subtitle = "${speedDials.size} configured",
                    onClick = {
                        speedNumberDraft = speedDials.firstOrNull { it.key == selectedSpeedKey }?.number.orEmpty()
                        showSpeedDialDialog = true
                    }
                )
            }

            // Section: SIP Accounts
            SettingsSectionCard(title = "SIP Accounts") {
                val onlineCount = sipStatuses.values.count { it == AccountStatus.ONLINE }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(onClick = onOpenSipAccounts)
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.WifiCalling3, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Manage SIP Accounts", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "${sipAccounts.size} configured · $onlineCount online",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    StatusDot(if (onlineCount > 0) AccountStatus.ONLINE else AccountStatus.OFFLINE)
                    Spacer(Modifier.width(8.dp))
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
                }
            }

            // Section: Privacy
            SettingsSectionCard(title = "Privacy & Security") {
                SettingsToggleRow(
                    title = "App Lock",
                    checked = appLockEnabled,
                    onCheckedChange = { wantEnable ->
                        if (!viewModel.setAppLockEnabled(wantEnable)) {
                            onOpenPinSetup()
                        }
                    }
                )
                SettingsActionRow(
                    icon = Icons.Default.Lock,
                    title = "Configure PIN & Biometrics",
                    subtitle = if (appLockEnabled) "6-digit SHA-256 PIN active" else "Set up 6-digit PIN",
                    onClick = onOpenPinSetup
                )
                SettingsActionRow(
                    icon = Icons.Default.Block,
                    title = "Block List",
                    subtitle = "Manage blocked callers",
                    onClick = onOpenBlockList
                )
                SettingsToggleRow("Block Unknown Numbers", blockUnknown, viewModel::setBlockUnknown)
            }

            // Section: Appearance
            SettingsSectionCard(title = "Appearance") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Palette, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Text("Accent Color (9 Palettes)", style = MaterialTheme.typography.titleMedium)
                }
                Spacer(Modifier.height(10.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    AppThemeColor.entries.forEach { option ->
                        val selected = themeColor == option
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Color(option.previewHex))
                                .border(
                                    width = if (selected) 3.dp else 1.dp,
                                    color = if (selected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { viewModel.setThemeColor(option) }
                                .testTag("theme_color_${option.name.lowercase()}"),
                            contentAlignment = Alignment.Center
                        ) {
                            if (selected) {
                                Icon(Icons.Default.Check, contentDescription = option.label, tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))
                Text("Dark Mode", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DarkModeOption.entries.forEach { mode ->
                        FilterChip(
                            selected = darkMode == mode,
                            onClick = { viewModel.setDarkMode(mode) },
                            label = { Text(mode.label) }
                        )
                    }
                }

                SettingsToggleRow("AMOLED Pure Black", amoledBlack, viewModel::setAmoledBlack)
            }

            // Section: Backup
            SettingsSectionCard(title = "Encrypted Backup") {
                SettingsToggleRow(
                    title = "Auto-Backup",
                    checked = autoBackupEnabled,
                    onCheckedChange = { wantEnable ->
                        if (!viewModel.setAutoBackupEnabled(wantEnable)) {
                            showAutoBackupPasswordDialog = true
                        }
                    }
                )
                Text(
                    text = if (lastBackupTimestamp > 0L) {
                        "Last backup: ${DateUtils.formatFullDateTime(lastBackupTimestamp)}"
                    } else {
                        "No backups created yet"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(6.dp))
                Text("Backup Provider", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    BackupProvider.entries.forEach { prov ->
                        FilterChip(
                            selected = backupProvider == prov,
                            onClick = { viewModel.setBackupProvider(prov) },
                            label = { Text(prov.label) }
                        )
                    }
                }
                if (backupProvider != BackupProvider.LOCAL) {
                    TextButton(onClick = { showTokenDialog = true }) {
                        Text("Configure ${backupProvider.label} OAuth Token")
                    }
                }
                Text("Auto-Backup Frequency", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(12 to "12h", 24 to "Daily", 168 to "Weekly").forEach { (hrs, lbl) ->
                        FilterChip(
                            selected = backupFreqHours == hrs,
                            onClick = { viewModel.setBackupFreqHours(hrs) },
                            label = { Text(lbl) }
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            pendingExportUri = null
                            showBackupPasswordDialog = true
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Backup, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Backup Now")
                    }
                    OutlinedButton(
                        onClick = {
                            exportLauncher.launch("callerx_backup_${System.currentTimeMillis()}.callerx")
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.SaveAlt, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Export .callerx")
                    }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { restoreLauncher.launch(arrayOf("*/*")) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Restore, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Restore from .callerx File")
                }
            }

            // Section: About
            val githubUrl = stringResource(R.string.github_url)
            SettingsSectionCard(title = "About CallerX") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text("CallerX v${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.titleMedium)
                        Text(
                            stringResource(R.string.license_notice),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(githubUrl)))
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Open Source Repository on GitHub")
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SettingsSectionCard(
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(10.dp))
            content()
        }
    }
}

@Composable
private fun SettingsToggleRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun SettingsActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
    }
}
