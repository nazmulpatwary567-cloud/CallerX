package com.example.ui.dialer

import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Voicemail
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.domain.model.AccountStatus
import com.example.ui.components.CallRoutePicker
import com.example.ui.components.StatusDot
import com.example.ui.theme.SignalGreen

private data class DialKey(val digit: String, val letters: String)

private val dialpadRows = listOf(
    listOf(DialKey("1", "VM"), DialKey("2", "ABC"), DialKey("3", "DEF")),
    listOf(DialKey("4", "GHI"), DialKey("5", "JKL"), DialKey("6", "MNO")),
    listOf(DialKey("7", "PQRS"), DialKey("8", "TUV"), DialKey("9", "WXYZ")),
    listOf(DialKey("*", ""), DialKey("0", "+"), DialKey("#", ""))
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DialerScreen(
    modifier: Modifier = Modifier,
    viewModel: DialerViewModel = hiltViewModel()
) {
    val number by viewModel.number.collectAsStateWithLifecycle()
    val exactName by viewModel.exactMatchedName.collectAsStateWithLifecycle()
    val matchedContacts by viewModel.matchedContacts.collectAsStateWithLifecycle()
    val showRoutePicker by viewModel.showRoutePicker.collectAsStateWithLifecycle()
    val availableRoutes by viewModel.availableRoutes.collectAsStateWithLifecycle()
    val sipStatuses by viewModel.sipStatuses.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val haptics = LocalHapticFeedback.current
    val voicemailNumber = stringResource(R.string.voicemail_number)

    LaunchedEffect(Unit) {
        viewModel.events.collect { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        }
    }

    if (showRoutePicker) {
        CallRoutePicker(
            routes = availableRoutes,
            numberToCall = number,
            onDismiss = viewModel::dismissRoutePicker,
            onSelectRoute = viewModel::selectRouteAndCall
        )
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 560.dp)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // SIP live status pill
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "CallerX Dialer",
                    style = MaterialTheme.typography.titleLarge
                )
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    val anyOnline = sipStatuses.values.any { it == AccountStatus.ONLINE }
                    val anyReg = sipStatuses.values.any { it == AccountStatus.REGISTERING }
                    val status = when {
                        anyOnline -> AccountStatus.ONLINE
                        anyReg -> AccountStatus.REGISTERING
                        else -> AccountStatus.OFFLINE
                    }
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        StatusDot(status)
                        Text(
                            text = when (status) {
                                AccountStatus.ONLINE -> "SIP Online"
                                AccountStatus.REGISTERING -> "SIP Registering"
                                else -> "SIM Ready"
                            },
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }

            // T9 live matched contacts preview
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val results = if (number.isBlank()) emptyList() else matchedContacts.take(6)
                if (results.isEmpty() && number.isNotBlank()) {
                    item {
                        Text(
                            text = "No matching contacts — tap Call to dial directly",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }
                items(results, key = { it.id }) { contact ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { viewModel.setNumber(contact.number) },
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Person,
                                    contentDescription = contact.name,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = contact.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = contact.number,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(
                                onClick = {
                                    viewModel.setNumber(contact.number)
                                    viewModel.onCallPressed()
                                }
                            ) {
                                Icon(
                                    Icons.Default.Call,
                                    contentDescription = "Call ${contact.name}",
                                    tint = SignalGreen
                                )
                            }
                        }
                    }
                }
            }

            // Number display & matched name
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (exactName != null) {
                    Text(
                        text = exactName!!,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = number.ifEmpty { "Enter number" },
                        style = MaterialTheme.typography.displayMedium.copy(
                            fontSize = if (number.length > 13) 24.sp else 32.sp
                        ),
                        color = if (number.isEmpty()) {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("dialer_number_display"),
                        textAlign = TextAlign.Center
                    )
                    if (number.isNotEmpty()) {
                        IconButton(
                            onClick = viewModel::backspace,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("dialer_backspace_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Backspace,
                                contentDescription = "Backspace"
                            )
                        }
                    }
                }
            }

            // Copy / Paste / Clear row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                TextButton(
                    onClick = {
                        if (number.isNotEmpty()) {
                            clipboard.setText(AnnotatedString(number))
                            Toast.makeText(context, "Copied number", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.testTag("dialer_copy_button")
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Copy")
                }
                TextButton(
                    onClick = {
                        val pasted = clipboard.getText()?.text.orEmpty()
                        if (pasted.isNotBlank()) viewModel.paste(pasted)
                    },
                    modifier = Modifier.testTag("dialer_paste_button")
                ) {
                    Icon(Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Paste")
                }
                TextButton(
                    onClick = viewModel::clear,
                    modifier = Modifier.testTag("dialer_clear_button")
                ) {
                    Icon(Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Clear")
                }
            }

            // 3x4 Dialpad Grid
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                dialpadRows.forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        row.forEach { key ->
                            Surface(
                                modifier = Modifier
                                    .size(width = 96.dp, height = 62.dp)
                                    .clip(RoundedCornerShape(20.dp))
                                    .combinedClickable(
                                        onClick = {
                                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            viewModel.appendDigit(key.digit)
                                        },
                                        onLongClick = {
                                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                            when (key.digit) {
                                                "0" -> viewModel.appendDigit("+")
                                                "1" -> viewModel.onVoicemailLongPress(voicemailNumber)
                                                in "2".."9" -> viewModel.onSpeedDialLongPress(key.digit.toInt())
                                            }
                                        }
                                    )
                                    .testTag("dialpad_key_${key.digit}"),
                                shape = RoundedCornerShape(20.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f)
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = key.digit,
                                        style = MaterialTheme.typography.headlineMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    if (key.digit == "1") {
                                        Icon(
                                            imageVector = Icons.Default.Voicemail,
                                            contentDescription = "Voicemail",
                                            modifier = Modifier.size(13.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    } else if (key.letters.isNotEmpty()) {
                                        Text(
                                            text = key.letters,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Green Call FAB
            FloatingActionButton(
                onClick = viewModel::onCallPressed,
                containerColor = SignalGreen,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier
                    .size(64.dp)
                    .testTag("dialer_call_fab")
            ) {
                Icon(
                    imageVector = Icons.Default.Call,
                    contentDescription = "Place Call",
                    modifier = Modifier.size(30.dp)
                )
            }
            Spacer(Modifier.height(6.dp))
        }
    }
}
