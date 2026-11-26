package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.privacy.AppLockManager
import com.example.service.SipService
import com.example.ui.components.AppLockGateScreen
import com.example.ui.components.PermissionRationaleDialog
import com.example.ui.contacts.ContactDetailScreen
import com.example.ui.contacts.ContactsScreen
import com.example.ui.contacts.ContactsViewModel
import com.example.ui.dialer.DialerScreen
import com.example.ui.dialer.DialerViewModel
import com.example.ui.history.HistoryScreen
import com.example.ui.history.HistoryViewModel
import com.example.ui.navigation.Screen
import com.example.ui.recording.PlayerScreen
import com.example.ui.recording.RecordingsScreen
import com.example.ui.settings.BlockListScreen
import com.example.ui.settings.PinSetupScreen
import com.example.ui.settings.SettingsScreen
import com.example.ui.settings.SipAccountsScreen
import com.example.ui.sms.ConversationScreen
import com.example.ui.sms.SmsInboxScreen
import com.example.ui.theme.CallerXTheme
import com.example.ui.theme.ThemeViewModel
import com.example.util.PermissionUtils
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

private data class BottomTab(
    val route: Screen,
    val label: String,
    val icon: ImageVector,
    val testTag: String
)

private val bottomTabs = listOf(
    BottomTab(Screen.Dialer, "Dialer", Icons.Default.Dialpad, "tab_dialer"),
    BottomTab(Screen.History, "History", Icons.Default.History, "tab_history"),
    BottomTab(Screen.Contacts, "Contacts", Icons.Default.Contacts, "tab_contacts"),
    BottomTab(Screen.Settings, "Settings", Icons.Default.Settings, "tab_settings")
)

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    @Inject lateinit var appLockManager: AppLockManager

    private val themeViewModel: ThemeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        SipService.start(this)

        val initialDialNumber = extractTelNumber(intent)

        setContent {
            val themeColor by themeViewModel.themeColor.collectAsStateWithLifecycle()
            val darkMode by themeViewModel.darkMode.collectAsStateWithLifecycle()
            val amoledBlack by themeViewModel.amoledBlack.collectAsStateWithLifecycle()
            val isLocked by appLockManager.isLocked.collectAsStateWithLifecycle()

            var showRationale by remember { mutableStateOf(false) }
            var missingPerms by remember { mutableStateOf(PermissionUtils.missingPermissions(this)) }

            val permissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestMultiplePermissions()
            ) {
                val stillMissing = PermissionUtils.missingPermissions(this)
                missingPerms = stillMissing
                showRationale = stillMissing.isNotEmpty()
            }

            LaunchedEffect(Unit) {
                if (missingPerms.isNotEmpty()) {
                    permissionLauncher.launch(missingPerms.toTypedArray())
                }
            }

            CallerXTheme(
                themeColor = themeColor,
                darkMode = darkMode,
                amoledBlack = amoledBlack
            ) {
                if (isLocked) {
                    val canBio = appLockManager.isBiometricEnabled() &&
                        appLockManager.canUseBiometrics(this)

                    LaunchedEffect(canBio) {
                        if (canBio) {
                            appLockManager.showBiometricPrompt(
                                activity = this@MainActivity,
                                onSuccess = {},
                                onError = {}
                            )
                        }
                    }

                    AppLockGateScreen(
                        biometricAvailable = canBio,
                        onPinVerified = { pin -> appLockManager.verifyPin(pin) },
                        onRequestBiometric = {
                            appLockManager.showBiometricPrompt(
                                activity = this@MainActivity,
                                onSuccess = {},
                                onError = {}
                            )
                        }
                    )
                } else {
                    if (showRationale && missingPerms.isNotEmpty()) {
                        PermissionRationaleDialog(
                            missingPermissions = missingPerms,
                            permissionLabelProvider = PermissionUtils::permissionLabel,
                            onGrantClick = {
                                showRationale = false
                                permissionLauncher.launch(missingPerms.toTypedArray())
                            },
                            onDismiss = { showRationale = false }
                        )
                    }

                    CallerXMainScaffold(initialDialNumber = initialDialNumber)
                }
            }
        }
    }

    private fun extractTelNumber(intent: Intent?): String? {
        val data = intent?.data ?: return null
        return if (data.scheme == "tel") data.schemeSpecificPart else null
    }
}

@Composable
private fun CallerXMainScaffold(initialDialNumber: String?) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDest = navBackStackEntry?.destination

    val historyViewModel: HistoryViewModel = hiltViewModel()
    val missedCount by historyViewModel.missedCount.collectAsStateWithLifecycle()

    val contactsViewModel: ContactsViewModel = hiltViewModel()
    val dialerViewModel: DialerViewModel = hiltViewModel()

    LaunchedEffect(initialDialNumber) {
        if (!initialDialNumber.isNullOrBlank()) {
            dialerViewModel.setNumber(initialDialNumber)
        }
    }

    var showAddContactDialog by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }
    var newNumber by remember { mutableStateOf("") }
    var newTags by remember { mutableStateOf("") }
    var newNote by remember { mutableStateOf("") }

    if (showAddContactDialog) {
        AlertDialog(
            onDismissRequest = { showAddContactDialog = false },
            title = { Text("New Contact") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = newName, onValueChange = { newName = it }, label = { Text("Name") }, singleLine = true)
                    OutlinedTextField(value = newNumber, onValueChange = { newNumber = it }, label = { Text("Phone / SIP") }, singleLine = true)
                    OutlinedTextField(value = newTags, onValueChange = { newTags = it }, label = { Text("Tags (comma separated)") }, singleLine = true)
                    OutlinedTextField(value = newNote, onValueChange = { newNote = it }, label = { Text("Note") }, minLines = 2)
                }
            },
            confirmButton = {
                Button(onClick = {
                    contactsViewModel.saveNewContact(newName, newNumber, newTags, newNote)
                    newName = ""; newNumber = ""; newTags = ""; newNote = ""
                    showAddContactDialog = false
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showAddContactDialog = false }) { Text("Cancel") }
            }
        )
    }

    val isTopLevel = currentDest?.hierarchy?.any { dest ->
        bottomTabs.any { dest.hasRoute(it.route::class) }
    } == true

    val isContactsRoute = currentDest?.hasRoute(Screen.Contacts::class) == true

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavRail = maxWidth >= 600.dp

        Scaffold(
            bottomBar = {
                if (isTopLevel && !useNavRail) {
                    NavigationBar {
                        bottomTabs.forEach { tab ->
                            val selected = currentDest?.hierarchy?.any { it.hasRoute(tab.route::class) } == true
                            NavigationBarItem(
                                selected = selected,
                                onClick = {
                                    navController.navigate(tab.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = {
                                    if (tab.route == Screen.History && missedCount > 0) {
                                        BadgedBox(badge = { Badge { Text(missedCount.toString()) } }) {
                                            Icon(tab.icon, contentDescription = tab.label)
                                        }
                                    } else {
                                        Icon(tab.icon, contentDescription = tab.label)
                                    }
                                },
                                label = { Text(tab.label) },
                                modifier = Modifier.testTag(tab.testTag)
                            )
                        }
                    }
                }
            },
            floatingActionButton = {
                if (isContactsRoute) {
                    FloatingActionButton(
                        onClick = { showAddContactDialog = true },
                        modifier = Modifier.testTag("add_contact_fab")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add Contact")
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (isTopLevel && useNavRail) {
                    NavigationRail {
                        bottomTabs.forEach { tab ->
                            val selected = currentDest?.hierarchy?.any { it.hasRoute(tab.route::class) } == true
                            NavigationRailItem(
                                selected = selected,
                                onClick = {
                                    navController.navigate(tab.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = {
                                    if (tab.route == Screen.History && missedCount > 0) {
                                        BadgedBox(badge = { Badge { Text(missedCount.toString()) } }) {
                                            Icon(tab.icon, contentDescription = tab.label)
                                        }
                                    } else {
                                        Icon(tab.icon, contentDescription = tab.label)
                                    }
                                },
                                label = { Text(tab.label) }
                            )
                        }
                    }
                }

                NavHost(
                    navController = navController,
                    startDestination = Screen.Dialer,
                    modifier = Modifier.weight(1f)
                ) {
                    composable<Screen.Dialer> {
                        DialerScreen(viewModel = dialerViewModel)
                    }
                    composable<Screen.History> {
                        HistoryScreen(
                            onNavigateToSms = { address ->
                                navController.navigate(Screen.Conversation(0L, address))
                            },
                            onNavigateToRecordings = {
                                navController.navigate(Screen.Recordings)
                            },
                            viewModel = historyViewModel
                        )
                    }
                    composable<Screen.Contacts> {
                        ContactsScreen(
                            onOpenContact = { id ->
                                navController.navigate(Screen.ContactDetail(id))
                            },
                            onOpenSmsInbox = {
                                navController.navigate(Screen.SmsInbox)
                            },
                            onMessageNumber = { number ->
                                navController.navigate(Screen.Conversation(0L, number))
                            },
                            viewModel = contactsViewModel
                        )
                    }
                    composable<Screen.Settings> {
                        SettingsScreen(
                            onOpenSipAccounts = { navController.navigate(Screen.SipAccounts) },
                            onOpenBlockList = { navController.navigate(Screen.BlockList) },
                            onOpenPinSetup = { navController.navigate(Screen.PinSetup) },
                            onOpenRecordings = { navController.navigate(Screen.Recordings) },
                            onOpenSmsInbox = { navController.navigate(Screen.SmsInbox) }
                        )
                    }
                    composable<Screen.ContactDetail> { backStackEntry ->
                        val route = backStackEntry.toRoute<Screen.ContactDetail>()
                        ContactDetailScreen(
                            contactId = route.contactId,
                            onBack = { navController.popBackStack() },
                            onMessage = { number ->
                                navController.navigate(Screen.Conversation(0L, number))
                            }
                        )
                    }
                    composable<Screen.SmsInbox> {
                        SmsInboxScreen(
                            onBack = { navController.popBackStack() },
                            onOpenConversation = { threadId, address ->
                                navController.navigate(Screen.Conversation(threadId, address))
                            }
                        )
                    }
                    composable<Screen.Conversation> { backStackEntry ->
                        val route = backStackEntry.toRoute<Screen.Conversation>()
                        ConversationScreen(
                            threadId = route.threadId,
                            address = route.address,
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable<Screen.Recordings> {
                        RecordingsScreen(
                            onBack = { navController.popBackStack() },
                            onOpenPlayer = { recId -> navController.navigate(Screen.Player(recId)) }
                        )
                    }
                    composable<Screen.Player> { backStackEntry ->
                        val route = backStackEntry.toRoute<Screen.Player>()
                        PlayerScreen(
                            recordingId = route.recordingId,
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable<Screen.SipAccounts> {
                        SipAccountsScreen(onBack = { navController.popBackStack() })
                    }
                    composable<Screen.BlockList> {
                        BlockListScreen(onBack = { navController.popBackStack() })
                    }
                    composable<Screen.PinSetup> {
                        PinSetupScreen(onBack = { navController.popBackStack() })
                    }
                }
            }
        }
    }
}
