package com.example.ui.settings

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.model.AccountStatus
import com.example.domain.model.SipAccount
import com.example.domain.model.SipTransport
import com.example.ui.components.StatusDot

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SipAccountsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val accounts by viewModel.sipAccounts.collectAsStateWithLifecycle()
    val statuses by viewModel.sipStatuses.collectAsStateWithLifecycle()
    val latencies by viewModel.latencies.collectAsStateWithLifecycle()
    val enforceTlsSrtp by viewModel.enforceTlsSrtp.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var editingAccount by remember { mutableStateOf<SipAccount?>(null) }
    var showEditor by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.refreshAllLatencies()
        viewModel.events.collect { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        }
    }

    if (showEditor) {
        SipAccountEditorDialog(
            initial = editingAccount,
            onDismiss = { showEditor = false },
            onSave = { account ->
                viewModel.saveSipAccount(account)
                showEditor = false
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SIP Accounts") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::refreshAllLatencies) {
                        Icon(Icons.Default.Refresh, contentDescription = "Ping All SIP Servers")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                editingAccount = null
                showEditor = true
            }) {
                Icon(Icons.Default.Add, contentDescription = "Add SIP Account")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Enforce TLS + SRTP", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Forces encrypted signaling and SRTP media for all SIP accounts",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Switch(checked = enforceTlsSrtp, onCheckedChange = viewModel::setEnforceTlsSrtp)
                }
            }

            if (accounts.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "No SIP accounts yet. Tap + to configure a VoIP line.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(accounts, key = { it.id }) { acc ->
                        val status = statuses[acc.id] ?: AccountStatus.OFFLINE
                        val latencyMs = latencies[acc.id]
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                StatusDot(status = status)
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = acc.displayName.ifBlank { "${acc.username}@${acc.domain}" },
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        text = "${acc.username}@${acc.domain}:${acc.port} · ${acc.transport.name}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "${status.name} · ${latencyMs?.let { "$it ms" } ?: "Tap refresh to ping"}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                IconButton(onClick = {
                                    editingAccount = acc
                                    showEditor = true
                                }) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit SIP account")
                                }
                                IconButton(onClick = { viewModel.deleteSipAccount(acc.id) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete SIP account")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SipAccountEditorDialog(
    initial: SipAccount?,
    onDismiss: () -> Unit,
    onSave: (SipAccount) -> Unit
) {
    var displayName by remember { mutableStateOf(initial?.displayName.orEmpty()) }
    var username by remember { mutableStateOf(initial?.username.orEmpty()) }
    var password by remember { mutableStateOf(initial?.password.orEmpty()) }
    var domain by remember { mutableStateOf(initial?.domain.orEmpty()) }
    var port by remember { mutableStateOf((initial?.port ?: 5061).toString()) }
    var transport by remember { mutableStateOf(initial?.transport ?: SipTransport.TLS) }
    var enabled by remember { mutableStateOf(initial?.enabled ?: true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Add SIP Account" else "Edit SIP Account") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it },
                    label = { Text("Label (e.g. Work VoIP)") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Username / Extension") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = domain,
                    onValueChange = { domain = it },
                    label = { Text("SIP Domain / Host") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = port,
                    onValueChange = { port = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Port") },
                    singleLine = true
                )
                Text("Transport", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    SipTransport.entries.forEach { t ->
                        FilterChip(
                            selected = transport == t,
                            onClick = {
                                transport = t
                                if (initial == null) {
                                    port = when (t) {
                                        SipTransport.TLS -> "5061"
                                        SipTransport.WSS -> "443"
                                        else -> "5060"
                                    }
                                }
                            },
                            label = { Text(t.name) }
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Enabled")
                    Switch(checked = enabled, onCheckedChange = { enabled = it })
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                if (username.isNotBlank() && domain.isNotBlank()) {
                    onSave(
                        SipAccount(
                            id = initial?.id ?: 0L,
                            username = username.trim(),
                            password = password,
                            domain = domain.trim(),
                            port = port.toIntOrNull() ?: 5060,
                            transport = transport,
                            enabled = enabled,
                            displayName = displayName.trim()
                        )
                    )
                }
            }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
