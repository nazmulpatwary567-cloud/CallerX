package com.example.ui.history

import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallMissed
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.model.CallLog
import com.example.domain.model.CallRouteType
import com.example.domain.model.CallType
import com.example.domain.model.HistoryFilter
import com.example.ui.components.RouteBadge
import com.example.ui.theme.SignalCyan
import com.example.ui.theme.SignalGreen
import com.example.ui.theme.SignalRed
import com.example.util.DateUtils
import com.example.util.DurationUtils

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HistoryScreen(
    onNavigateToSms: (String) -> Unit,
    onNavigateToRecordings: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HistoryViewModel = hiltViewModel()
) {
    val logs by viewModel.logs.collectAsStateWithLifecycle()
    val filter by viewModel.filter.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var pendingDeleteLog by remember { mutableStateOf<CallLog?>(null) }
    var tagNoteTarget by remember { mutableStateOf<CallLog?>(null) }
    var tagInput by remember { mutableStateOf("") }
    var noteInput by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        viewModel.events.collect { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        }
    }

    if (pendingDeleteLog != null) {
        AlertDialog(
            onDismissRequest = { pendingDeleteLog = null },
            title = { Text("Delete Call Log?") },
            text = { Text("Remove ${pendingDeleteLog?.contactName ?: pendingDeleteLog?.number} from call history?") },
            confirmButton = {
                Button(onClick = {
                    pendingDeleteLog?.let { viewModel.deleteLog(it.id) }
                    pendingDeleteLog = null
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteLog = null }) { Text("Cancel") }
            }
        )
    }

    if (tagNoteTarget != null) {
        AlertDialog(
            onDismissRequest = { tagNoteTarget = null },
            title = { Text("Add Tag & Note") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = tagInput,
                        onValueChange = { tagInput = it },
                        label = { Text("Tag (e.g. Client, VIP)") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = noteInput,
                        onValueChange = { noteInput = it },
                        label = { Text("Note") },
                        minLines = 2
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val t = tagNoteTarget!!
                    viewModel.saveTagAndNote(t.number, t.contactName, tagInput, noteInput)
                    tagInput = ""
                    noteInput = ""
                    tagNoteTarget = null
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { tagNoteTarget = null }) { Text("Cancel") }
            }
        )
    }

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 640.dp)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Call History", style = MaterialTheme.typography.headlineMedium)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    androidx.compose.material3.IconButton(onClick = viewModel::refreshSystemLogs) {
                        Icon(Icons.Default.Sync, contentDescription = "Sync device call logs")
                    }
                    TextButton(onClick = onNavigateToRecordings) {
                        Text("Recordings")
                    }
                }
            }

            OutlinedTextField(
                value = query,
                onValueChange = viewModel::setQuery,
                placeholder = { Text("Search name or number") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .testTag("history_search_input")
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                HistoryFilter.entries.forEach { item ->
                    FilterChip(
                        selected = filter == item,
                        onClick = { viewModel.setFilter(item) },
                        label = {
                            Text(
                                when (item) {
                                    HistoryFilter.ALL -> "All"
                                    HistoryFilter.MISSED -> "Missed"
                                    HistoryFilter.SIP -> "SIP"
                                    HistoryFilter.SIM -> "SIM"
                                }
                            )
                        },
                        modifier = Modifier.testTag("history_filter_${item.name.lowercase()}")
                    )
                }
            }

            if (logs.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            modifier = Modifier.size(56.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = "No call history yet",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(logs, key = { it.id }) { log ->
                        var showMenu by remember { mutableStateOf(false) }
                        val dismissState = rememberSwipeToDismissBoxState(
                            confirmValueChange = { value ->
                                if (value == SwipeToDismissBoxValue.EndToStart) {
                                    pendingDeleteLog = log
                                }
                                false
                            }
                        )

                        SwipeToDismissBox(
                            state = dismissState,
                            enableDismissFromStartToEnd = false,
                            backgroundContent = {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(SignalRed.copy(alpha = 0.18f))
                                        .padding(end = 20.dp),
                                    contentAlignment = Alignment.CenterEnd
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Delete",
                                        tint = SignalRed
                                    )
                                }
                            }
                        ) {
                            Box {
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(16.dp))
                                        .combinedClickable(
                                            onClick = { viewModel.callBack(log) },
                                            onLongClick = { showMenu = true }
                                        )
                                        .testTag("call_log_item_${log.id}"),
                                    shape = RoundedCornerShape(16.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        val (icon, tint) = when {
                                            log.isBlocked -> Icons.Default.Block to SignalRed
                                            log.type == CallType.MISSED -> Icons.AutoMirrored.Filled.CallMissed to SignalRed
                                            log.type == CallType.INCOMING -> Icons.AutoMirrored.Filled.CallReceived to SignalGreen
                                            else -> Icons.AutoMirrored.Filled.CallMade to SignalCyan
                                        }
                                        Box(
                                            modifier = Modifier
                                                .size(42.dp)
                                                .clip(CircleShape)
                                                .background(tint.copy(alpha = 0.15f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(icon, contentDescription = log.type.name, tint = tint)
                                        }

                                        Spacer(Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = log.contactName?.takeIf { it.isNotBlank() } ?: log.number,
                                                style = MaterialTheme.typography.titleMedium,
                                                color = if (log.type == CallType.MISSED) SignalRed else MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Spacer(Modifier.height(4.dp))
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                RouteBadge(
                                                    label = log.routeLabel.ifBlank { log.route.name },
                                                    isSip = log.route == CallRouteType.SIP
                                                )
                                                if (log.duration > 0L) {
                                                    Text(
                                                        text = DurationUtils.formatHumanReadable(log.duration),
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                                if (log.recordingPath != null) {
                                                    Icon(
                                                        imageVector = Icons.Default.FiberManualRecord,
                                                        contentDescription = "Recorded",
                                                        tint = SignalRed,
                                                        modifier = Modifier.size(12.dp)
                                                    )
                                                }
                                            }
                                        }
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                text = DateUtils.formatRelativeTime(log.startTime),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Spacer(Modifier.height(4.dp))
                                            Icon(
                                                imageVector = Icons.Default.Call,
                                                contentDescription = "Call back",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }

                                DropdownMenu(
                                    expanded = showMenu,
                                    onDismissRequest = { showMenu = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Call") },
                                        onClick = {
                                            showMenu = false
                                            viewModel.callBack(log)
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Message") },
                                        onClick = {
                                            showMenu = false
                                            onNavigateToSms(log.number)
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Add tag / note") },
                                        onClick = {
                                            showMenu = false
                                            tagNoteTarget = log
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Block number") },
                                        onClick = {
                                            showMenu = false
                                            viewModel.blockNumber(log.number)
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Delete") },
                                        onClick = {
                                            showMenu = false
                                            pendingDeleteLog = log
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
