package com.example.ui.contacts

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.ui.components.CallRoutePicker
import com.example.ui.theme.SignalRed
import com.example.util.DurationUtils
import com.example.util.PhoneUtils

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ContactDetailScreen(
    contactId: Long,
    onBack: () -> Unit,
    onMessage: (String) -> Unit,
    viewModel: ContactsViewModel = hiltViewModel()
) {
    val contact by viewModel.selectedContact.collectAsStateWithLifecycle()
    val routes by viewModel.availableRoutes.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var tags by remember(contact?.id) { mutableStateOf(contact?.tags.orEmpty()) }
    var newTag by remember { mutableStateOf("") }
    var note by remember(contact?.id) { mutableStateOf(contact?.note.orEmpty()) }
    var showRoutePicker by remember { mutableStateOf(false) }

    LaunchedEffect(contactId) {
        viewModel.loadContact(contactId)
        viewModel.refreshRoutes()
    }

    LaunchedEffect(contact) {
        contact?.let {
            tags = it.tags
            note = it.note
        }
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        }
    }

    if (showRoutePicker && contact != null) {
        CallRoutePicker(
            routes = routes,
            numberToCall = contact!!.number,
            onDismiss = { showRoutePicker = false },
            onSelectRoute = { route, _ ->
                viewModel.placeCall(contact!!.number, route)
                showRoutePicker = false
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(contact?.name ?: "Contact Detail") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        val c = contact
        if (c == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("Loading contact...")
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (!c.photoUri.isNullOrBlank()) {
                    AsyncImage(
                        model = c.photoUri,
                        contentDescription = c.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(92.dp)
                            .clip(CircleShape)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(92.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = c.name,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(44.dp)
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))
                Text(c.name, style = MaterialTheme.typography.headlineMedium)
                Text(c.number, style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "${DurationUtils.formatHumanReadable(c.totalTalkTimeSeconds)} total talk time",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { showRoutePicker = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Call, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Call")
                    }
                    OutlinedButton(
                        onClick = { onMessage(c.number) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Sms, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Message")
                    }
                }

                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val vcf = PhoneUtils.toVCard(c.copy(tags = tags, note = note))
                            val share = Intent(Intent.ACTION_SEND).apply {
                                type = "text/x-vcard"
                                putExtra(Intent.EXTRA_TEXT, vcf)
                                putExtra(Intent.EXTRA_SUBJECT, "${c.name}.vcf")
                            }
                            context.startActivity(Intent.createChooser(share, "Share Contact vCard"))
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Share vCard")
                    }
                    OutlinedButton(
                        onClick = { viewModel.blockNumber(c.number) },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = SignalRed),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Block, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Block")
                    }
                }

                Spacer(Modifier.height(24.dp))
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("Tags", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        tags.forEach { tag ->
                            InputChip(
                                selected = true,
                                onClick = { tags = tags - tag },
                                label = { Text(tag) },
                                trailingIcon = {
                                    Icon(Icons.Default.Close, contentDescription = "Remove $tag", modifier = Modifier.size(16.dp))
                                }
                            )
                        }
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = newTag,
                            onValueChange = { newTag = it },
                            label = { Text("Add tag") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = {
                            if (newTag.isNotBlank()) {
                                tags = (tags + newTag.trim()).distinct()
                                newTag = ""
                            }
                        }) {
                            Icon(Icons.Default.Add, contentDescription = "Add Tag")
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                    Text("Notes", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        label = { Text("Private contact note") },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(14.dp))
                    Button(
                        onClick = {
                            viewModel.updateContactDetails(c.copy(tags = tags, note = note))
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Save Tags & Note")
                    }
                }
            }
        }
    }
}
